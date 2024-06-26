package com.scalag.vulkan.executor;

import com.scalag.vulkan.VulkanContext
import com.scalag.vulkan.compute.ComputePipeline
import com.scalag.vulkan.compute.LayoutInfo
import com.scalag.vulkan.compute.Shader
import com.scalag.vulkan.utility.VulkanObjectHandle
import com.scalag.vulkan.memory.Buffer
import com.scalag.vulkan.memory.DescriptorSet
import org.joml.Vector3ic
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*

import java.nio.LongBuffer
import java.util.*
import scala.collection.mutable
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.VK10.*

import scala.util.Using;

/** @author
  *   MarconZet Created 15.04.2020
  */
class MapExecutor(dataLength: Int, bufferActions: Seq[BufferAction], computePipeline: ComputePipeline, context: VulkanContext)
    extends AbstractExecutor(dataLength, bufferActions, context) {
  private val shader: Shader = computePipeline.computeShader;

  protected def getBiggestTransportData: Int = shader.layoutInfos.maxBy(_.size).size

  protected def setupBuffers(): (Seq[DescriptorSet], Seq[Buffer]) = {
    val layoutInfos = shader.layoutInfos
    val buffers = layoutInfos.indices.map { i =>
      new Buffer(
        layoutInfos(i).size * dataLength,
        VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | bufferActions(i).action,
        0,
        VMA_MEMORY_USAGE_GPU_ONLY,
        allocator
      )
    };

    val bufferDeque = mutable.ArrayDeque.from(buffers)
    val descriptorSetLayouts = computePipeline.descriptorSetLayouts
    val descriptorSets = for (i <- descriptorSetLayouts.indices) yield {
      val descriptorSet = new DescriptorSet(device, descriptorSetLayouts(i), descriptorPool);
      val layouts = shader.getLayoutsBySets(i);

      val writeDescriptorSet = VkWriteDescriptorSet.callocStack(layouts.size);

      for (j <- layouts.indices) do {
        val descriptorBufferInfo = VkDescriptorBufferInfo
          .callocStack(1)
          .buffer(bufferDeque.removeHead().get)
          .offset(0)
          .range(VK_WHOLE_SIZE);

        writeDescriptorSet
          .get(j)
          .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
          .dstSet(descriptorSet.get)
          .dstBinding(layouts(j).binding)
          .descriptorCount(1)
          .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
          .pBufferInfo(descriptorBufferInfo);
      }

      vkUpdateDescriptorSets(device.get, writeDescriptorSet, null);
      descriptorSet
    }
    (descriptorSets, buffers)
  }

  protected def recordCommandBuffer(commandBuffer: VkCommandBuffer): Unit =
    Using(stackPush()) { stack =>
      vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.get);

      val pDescriptorSets = stack.longs(descriptorSets.map(_.get): _*);
      vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.pipelineLayout, 0, pDescriptorSets, null);

      val workgroup = shader.workgroupDimensions
      vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());
    }

}
