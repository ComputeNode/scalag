package com.scalag.vulkan.executor

import com.scalag.vulkan.VulkanContext
import com.scalag.vulkan.command.*
import com.scalag.vulkan.compute.*
import com.scalag.vulkan.core.*
import com.scalag.vulkan.executor.BufferAction
import com.scalag.vulkan.executor.SequenceExecutor.*
import com.scalag.vulkan.memory.*
import com.scalag.vulkan.util.Util.*
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSynchronization2.{VK_ACCESS_2_SHADER_READ_BIT_KHR, VK_ACCESS_2_SHADER_WRITE_BIT_KHR, VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT_KHR, vkCmdPipelineBarrier2KHR}
import org.lwjgl.vulkan.VK10.*

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.util.Using

/** @author
  *   MarconZet Created 15.04.2020
  */
class SequenceExecutor(dataLength: Int, computeSequence: ComputationSequence, context: VulkanContext) {
  private val device: Device = context.device
  private val queue: Queue = context.computeQueue
  private val allocator: Allocator = context.allocator
  private val descriptorPool: DescriptorPool = context.descriptorPool
  private val commandPool: CommandPool = context.commandPool

  private val descriptorSets: Map[ComputePipeline, Seq[DescriptorSet]] = pushStack { stack =>
    val pipelines = computeSequence.sequence.collect { case Compute(pipeline, _) => pipeline }

    val rawSets = pipelines.map(_.computeShader.layoutInfo.sets)
    val numbered = rawSets.flatten.zipWithIndex
    val numberedSets = rawSets
      .foldLeft((numbered, Seq.empty[Seq[(LayoutSet, Int)]])) { case ((remaining, acc), sequence) =>
        val (current, rest) = remaining.splitAt(sequence.length)
        (rest, acc :+ current)
      }
      ._2

    val pipelineToIndex = pipelines.zipWithIndex.toMap
    val dependencies = computeSequence.dependencies.map { case Dependency(from, fromSet, to, toSet) =>
      (pipelineToIndex(from), fromSet, pipelineToIndex(to), toSet)
    }
    val resolvedSets = dependencies
      .foldLeft(numberedSets.map(_.toArray)) { case (sets, (from, fromSet, to, toSet)) =>
        val a = sets(from)(fromSet)
        val b = sets(to)(toSet)
        assert(a._1 == b._1)
        val nextIndex = a._2 min b._2
        sets(from).update(fromSet, (a._1, nextIndex))
        sets(to).update(toSet, (b._1, nextIndex))
        sets
      }
      .map(_.toSeq.map(_._2))

    val descriptorSetMap = resolvedSets
      .zip(pipelines.map(_.descriptorSetLayouts))
      .flatMap { case (sets, layouts) =>
        sets.zip(layouts)
      }
      .distinctBy(_._1)
      .map { case (set, layout) =>
        (set, new DescriptorSet(device, layout, descriptorPool))
      }
      .toMap

    pipelines.zip(resolvedSets.map(_.map(descriptorSetMap(_)))).toMap
  }

  private val commandBuffer: VkCommandBuffer = pushStack { stack =>
    val commandBuffer = commandPool.createCommandBuffer()

    val commandBufferBeginInfo = VkCommandBufferBeginInfo
      .calloc(stack)
      .sType$Default()
      .flags(0)

    check(vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo), "Failed to begin recording command buffer")

    computeSequence.sequence.foreach {
      case Compute(pipeline, _) =>
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.get)

        val pDescriptorSets = stack.longs(descriptorSets(pipeline).map(_.get): _*)
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.pipelineLayout, 0, pDescriptorSets, null)

        val workgroup = pipeline.computeShader.workgroupDimensions
        vkCmdDispatch(commandBuffer, dataLength / workgroup.x, 1 / workgroup.y, 1 / workgroup.z) // TODO this can be changed to indirect dispatch
      case MemoryBarrier =>
        val memoryBarrier = VkMemoryBarrier2KHR
          .calloc(1, stack)
          .sType$Default()
          .srcStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT_KHR)
          .srcAccessMask(VK_ACCESS_2_SHADER_WRITE_BIT_KHR)
          .dstStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT_KHR)
          .dstStageMask(VK_ACCESS_2_SHADER_READ_BIT_KHR)

        val dependencyInfo = VkDependencyInfoKHR
          .calloc(stack)
          .sType$Default()
          .pMemoryBarriers(memoryBarrier)

        vkCmdPipelineBarrier2KHR(commandBuffer, dependencyInfo)
    }

    check(vkEndCommandBuffer(commandBuffer), "Failed to finish recording command buffer")
    commandBuffer
  }

  def execute(inputs: Seq[ByteBuffer], inputSize: Int): Seq[ByteBuffer] = pushStack { stack =>
    assume(inputSize == dataLength, "Input size does not match the data length")

    val stagingBuffer = new Buffer(
      inputs.map(_.remaining()).max,
      VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
      VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
      VMA_MEMORY_USAGE_UNKNOWN,
      allocator
    )
    
    val bufferActions = computeSequence.sequence.collect { case Compute(_, actions) => actions }
    for (i <- bufferActions.indices if bufferActions(i) == BufferAction.LOAD_INTO) do {
      val buffer = input(i)
      Buffer.copyBuffer(buffer, stagingBuffer, buffer.remaining())
      Buffer.copyBuffer(stagingBuffer, buffers(i), buffer.remaining(), commandPool).block().destroy()
    }

    val fence = new Fence(device)
    val pCommandBuffer = stack.callocPointer(1).put(0, commandBuffer)
    val submitInfo = VkSubmitInfo
      .calloc(stack)
      .sType$Default()
      .pCommandBuffers(pCommandBuffer)

    check(VK10.vkQueueSubmit(queue.get, submitInfo, fence.get), "Failed to submit command buffer to queue")
    fence.block().destroy()

    val output = for (i <- bufferActions.indices if bufferActions(i) == BufferAction.LOAD_FROM) yield {
      val fence = Buffer.copyBuffer(buffers(i), stagingBuffer, buffers(i).size, commandPool)
      val outBuffer = BufferUtils.createByteBuffer(buffers(i).size)
      fence.block().destroy()
      Buffer.copyBuffer(stagingBuffer, outBuffer, outBuffer.remaining())
      outBuffer

    }
    stagingBuffer.destroy()
    output
  }

  def destroy(): Unit = {
    commandPool.freeCommandBuffer(commandBuffer)
    descriptorSets.foreach(_.destroy())
  }

}

object SequenceExecutor {
  case class ComputationSequence(sequence: Seq[ComputationStep], dependencies: Seq[Dependency])

  sealed trait ComputationStep
  case class Compute(computePipeline: ComputePipeline, bufferActions: Map[LayoutInfo, BufferAction]) extends ComputationStep
  case object MemoryBarrier extends ComputationStep

  case class Dependency(from: ComputePipeline, fromSet: Int, to: ComputePipeline, toSet: Int)
}
