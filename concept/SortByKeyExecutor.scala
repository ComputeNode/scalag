package com.scalag.vulkan.executor

import com.scalag.vulkan.VulkanContext
import com.scalag.vulkan.compute.{ComputePipeline, LayoutInfo, Shader}
import com.scalag.vulkan.executor.BufferAction.*
import com.scalag.vulkan.executor.SortByKeyExecutor.*
import com.scalag.vulkan.memory.{Buffer, DescriptorSet}
import com.scalag.vulkan.util.Util.{check, pushStack}
import org.joml.Vector3i
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.{VMA_MEMORY_USAGE_CPU_TO_GPU, VMA_MEMORY_USAGE_GPU_ONLY}
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.{VkBufferMemoryBarrier, VkCommandBuffer}

import scala.collection.mutable
import scala.util.Using

class SortByKeyExecutor(dataLength: Int, keyPipeline: ComputePipeline, context: VulkanContext)
    extends AbstractExecutor(dataLength, createBufferActions, context) {
  private val sortPasses: Int = getNumberOfPasses(dataLength)

  private val keyShader: Shader = keyPipeline.computeShader

  private val preparePipeline: ComputePipeline = createPreparePipeline(context)
  private val sortPipeline: ComputePipeline = createSortPipeline(context)
  private val copyPipeline: ComputePipeline = createCopyPipeline(context)

  def getBiggestTransportData: Int = keyShader.layoutInfos.head.size

  override def destroy(): Unit = {
    Seq(preparePipeline, copyPipeline, sortPipeline).foreach { pipeline =>
      pipeline.computeShader.destroy()
      pipeline.destroy()
    }
    super.destroy()
  }

  protected def setupBuffers(): (Seq[DescriptorSet], Seq[Buffer]) = pushStack { stack =>
    val layoutInfos = keyShader.layoutInfos

    val sizes = Seq(layoutInfos.head.size, layoutInfos.head.size, 4, 4, 4)

    val buffers = mutable.Buffer[Buffer]()
    for (i <- sizes.indices) do
      buffers.addOne(
        new Buffer(sizes(i) * dataLength, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | bufferActions(i).action, 0, VMA_MEMORY_USAGE_GPU_ONLY, allocator)
      )

    for (i <- 0 until 2) do buffers.addOne(new Buffer(8, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, 0, VMA_MEMORY_USAGE_GPU_ONLY, allocator))

    buffers.addOne(new Buffer(4, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, 0, VMA_MEMORY_USAGE_CPU_TO_GPU, allocator))
    val Seq(in, out, keys, order1, order2, data1, data2, size) = buffers.toSeq

    val sizeData = stack.calloc(4 * 2)
    sizeData.asIntBuffer().put(this.dataLength).put(this.dataLength)
    Buffer.copyBuffer(sizeData, size, sizeData.remaining())

    val outOrder = if (sortPasses % 2 == 0) order1 else order2

    val descriptorSets = Seq(
      createUpdatedDescriptorSet(keyPipeline.descriptorSetLayouts.head, Seq(in, keys)),
      createUpdatedDescriptorSet(preparePipeline.descriptorSetLayouts.head, Seq(order1, data1)),
      createUpdatedDescriptorSet(sortPipeline.descriptorSetLayouts.head, Seq(keys, order1, order2, data1, data2)),
      createUpdatedDescriptorSet(sortPipeline.descriptorSetLayouts.head, Seq(keys, order2, order1, data2, data1)),
      createUpdatedDescriptorSet(copyPipeline.descriptorSetLayouts.head, Seq(in, out, outOrder, size))
    )
    (descriptorSets, buffers.toSeq)
  }

  protected def recordCommandBuffer(commandBuffer: VkCommandBuffer): Unit = pushStack { stack =>
    val Seq(keySet, prepSet, sort1Set, sort2Set, copySet) = descriptorSets

    val Seq(inBuffer, outBuffer, keysBuffer, order1Buffer, order2Buffer, data1Buffer, data2Buffer, sizeBuffer) = buffers.toSeq

    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, keyPipeline.get)

    var pDescriptorSets = stack.longs(keySet.get)
    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, keyPipeline.pipelineLayout, 0, pDescriptorSets, null)

    var workgroup = keyShader.workgroupDimensions
    vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z())

    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, preparePipeline.get)

    pDescriptorSets = stack.longs(prepSet.get)
    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, preparePipeline.pipelineLayout, 0, pDescriptorSets, null)

    workgroup = preparePipeline.computeShader.workgroupDimensions
    vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z())

    var bufferMemoryBarriers = getMemoryBarriers(Seq(keysBuffer, order1Buffer, data1Buffer))
    vkCmdPipelineBarrier(
      commandBuffer,
      VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
      VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
      0,
      null,
      bufferMemoryBarriers,
      null
    )

    val buffersSet1 = Seq(order1Buffer, data1Buffer)
    val buffersSet2 = Seq(order2Buffer, data2Buffer)

    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, sortPipeline.get)

    for (i <- 0 until sortPasses) do {
      pDescriptorSets = stack.longs(if (i % 2 == 0) sort1Set.get else sort2Set.get)
      vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, sortPipeline.pipelineLayout, 0, pDescriptorSets, null)

      workgroup = sortPipeline.computeShader.workgroupDimensions
      vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z())

      bufferMemoryBarriers = getMemoryBarriers(if (i % 2 != 0) buffersSet1 else buffersSet2)
      vkCmdPipelineBarrier(
        commandBuffer,
        VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
        VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
        0,
        null,
        bufferMemoryBarriers,
        null
      )

      vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, copyPipeline.get)

      pDescriptorSets = stack.longs(copySet.get)
      vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, copyPipeline.pipelineLayout, 0, pDescriptorSets, null)

      workgroup = copyPipeline.computeShader.workgroupDimensions
      vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z())
    }
  }

  private def getMemoryBarriers(buffers: Seq[Buffer]): VkBufferMemoryBarrier.Buffer = pushStack { stack =>
    val bufferMemoryBarriers = VkBufferMemoryBarrier.calloc(buffers.size, stack)

    for (i <- buffers.indices) do
      bufferMemoryBarriers
        .get(i)
        .pNext(0)
        .sType$Default()
        .srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
        .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        .buffer(buffers(i).get)
        .offset(0)
        .size(VK_WHOLE_SIZE)
    bufferMemoryBarriers
  }
}

object SortByKeyExecutor {

  private def createBufferActions: Seq[BufferAction] =
    Seq(LoadTo, LoadFrom, DoNothing, DoNothing, DoNothing, DoNothing, DoNothing, DoNothing)

  private def getNumberOfPasses(dataLength: Int): Int = {
    var remaining = dataLength
    var d = 0
    while (remaining > 1) {
      if (remaining % 2 != 0)
        throw new IllegalArgumentException("Number of data must be power of 2")
      remaining = remaining / 2
      d = d + 1
    }
    (d * d + d) / 2
  }

  private def createCopyPipeline(context: VulkanContext): ComputePipeline = {
    val shader = new Shader(
      Shader.loadShader("copy.spv"),
      new Vector3i(1024, 1, 1),
      Seq(LayoutInfo(0, 0, 4), LayoutInfo(0, 1, 4), LayoutInfo(0, 2, 4), LayoutInfo(0, 3, 4)),
      "main",
      context.device
    )
    new ComputePipeline(shader, context)
  }

  private def createSortPipeline(context: VulkanContext): ComputePipeline = {
    val shader = new Shader(
      Shader.loadShader("sort.spv"),
      new Vector3i(1024, 1, 1),
      Seq(LayoutInfo(0, 0, 4), LayoutInfo(0, 1, 4), LayoutInfo(0, 2, 4), LayoutInfo(0, 3, 4), LayoutInfo(0, 4, 4)),
      "main",
      context.device
    )
    new ComputePipeline(shader, context)
  }

  private def createPreparePipeline(context: VulkanContext): ComputePipeline = {
    val shader =
      new Shader(Shader.loadShader("prepare.spv"), new Vector3i(1024, 1, 1), Seq(LayoutInfo(0, 0, 4), LayoutInfo(0, 1, 4)), "main", context.device)
    new ComputePipeline(shader, context)
  }
}
