package com.scalag.vulkan.executor

import com.scalag.vulkan.VulkanContext
import com.scalag.vulkan.compute.*
import com.scalag.vulkan.memory.{Buffer, DescriptorSet}
import com.scalag.vulkan.util.Util.{check, pushStack}
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

import scala.collection.mutable
import scala.util.Using

/** @author
  *   MarconZet Created 15.04.2020
  */
class MapExecutor(dataLength: Int, bufferActions: Seq[BufferAction], computePipeline: ComputePipeline, context: VulkanContext)
    extends AbstractExecutor(dataLength, bufferActions, context) {
  private lazy val shader: Shader = computePipeline.computeShader

  protected def getBiggestTransportData: Int = shader.layoutInfos.collect {
    case LayoutInfo(_, _, InputBufferSize(n)) => n
  }.max

  protected def setupBuffers(): (Seq[DescriptorSet], Seq[Buffer]) = pushStack { stack =>
    val layoutInfos = shader.layoutInfos
    val buffers = layoutInfos.zipWithIndex.map { case (layoutInfo, i) =>
      val bufferSize = layoutInfo.size match {
        case InputBufferSize(n) => n * dataLength
        case UniformSize(n) => n
      }
      new Buffer(
        bufferSize,
        VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | bufferActions(i).action,
        0,
        VMA_MEMORY_USAGE_GPU_ONLY,
        allocator
      )
    }

    val bufferDeque = mutable.ArrayDeque.from(buffers)
    val descriptorSetLayouts = computePipeline.descriptorSetLayouts
    val descriptorSets = for (i <- descriptorSetLayouts.indices) yield {
      val descriptorSet = new DescriptorSet(device, descriptorSetLayouts(i), descriptorPool)
      val layouts = shader.getLayoutsBySets(i)

      val writeDescriptorSet = VkWriteDescriptorSet.calloc(layouts.size, stack)

      for (j <- layouts.indices) do {
        val descriptorBufferInfo = VkDescriptorBufferInfo
          .calloc(1, stack)
          .buffer(bufferDeque.removeHead().get)
          .offset(0)
          .range(VK_WHOLE_SIZE)

        writeDescriptorSet
          .get(j)
          .sType$Default()
          .dstSet(descriptorSet.get)
          .dstBinding(layouts(j).binding)
          .descriptorCount(1)
          .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
          .pBufferInfo(descriptorBufferInfo)
      }

      vkUpdateDescriptorSets(device.get, writeDescriptorSet, null)
      descriptorSet
    }
    (descriptorSets, buffers)
  }

  protected def recordCommandBuffer(commandBuffer: VkCommandBuffer): Unit =
    pushStack { stack =>
      vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.get)

      val pDescriptorSets = stack.longs(descriptorSets.map(_.get): _*)
      vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.pipelineLayout, 0, pDescriptorSets, null)

      val workgroup = shader.workgroupDimensions
      vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z())
    }

}
