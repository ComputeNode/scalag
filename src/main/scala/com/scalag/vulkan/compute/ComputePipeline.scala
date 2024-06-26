package com.scalag.vulkan.compute;

import com.scalag.vulkan.VulkanContext
import com.scalag.vulkan.core.Device
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import lombok.Getter
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*

import java.nio.LongBuffer
import java.util.List
import java.util.stream.Collectors.groupingBy
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout

import scala.util.Using;

/** @author
  *   MarconZet Created 14.04.2020
  */
class ComputePipeline(val computeShader: Shader, context: VulkanContext) extends VulkanObjectHandle {
  private val device: Device = context.device;

  val descriptorSetLayouts: Seq[Long] =
    computeShader.getLayoutsBySets.map(createDescriptorSetLayout)

  val pipelineLayout: Long = Using(stackPush()) { stack =>
    val pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
      .pNext(0)
      .flags(0)
      .pSetLayouts(stack.longs(descriptorSetLayouts: _*))
      .pPushConstantRanges(null)

    val pPipelineLayout = stack.callocLong(1);
    val err = vkCreatePipelineLayout(device.get, pipelineLayoutCreateInfo, null, pPipelineLayout);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create pipeline layout", err);
    pPipelineLayout.get(0);
  }.get

  protected val handle: Long = Using(stackPush()) { stack =>
    val pipelineShaderStageCreateInfo = VkPipelineShaderStageCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
      .pNext(0)
      .flags(0)
      .stage(VK_SHADER_STAGE_COMPUTE_BIT)
      .module(computeShader.get)
      .pName(stack.ASCII(computeShader.functionName));

    val computePipelineCreateInfo = VkComputePipelineCreateInfo.callocStack(1);
    computePipelineCreateInfo
      .get(0)
      .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
      .pNext(0)
      .flags(0)
      .stage(pipelineShaderStageCreateInfo)
      .layout(pipelineLayout)
      .basePipelineHandle(0)
      .basePipelineIndex(0);

    val pPipeline = stack.callocLong(1);
    val err = vkCreateComputePipelines(device.get, 0, computePipelineCreateInfo, null, pPipeline);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create compute pipeline", err);
    pPipeline.get(0);
  }.get

  private def createDescriptorSetLayout(layoutInfos: Seq[LayoutInfo]): Long =
    Using(stackPush()) { stack =>
      val descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.callocStack(layoutInfos.size)
      layoutInfos.foreach { layoutInfo =>
        descriptorSetLayoutBindings
          .get()
          .binding(layoutInfo.binding)
          .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
          .descriptorCount(1)
          .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
          .pImmutableSamplers(null)
      }
      descriptorSetLayoutBindings.flip();

      val descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo
        .callocStack()
        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
        .pNext(0)
        .flags(0)
        .pBindings(descriptorSetLayoutBindings);

      val pDescriptorSetLayout = stack.callocLong(1);
      val err = vkCreateDescriptorSetLayout(device.get, descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout);
      if (err != VK_SUCCESS)
        throw new VulkanAssertionError("Failed to create descriptor set layout", err);
      pDescriptorSetLayout.get(0);
    }.get

  protected def close(): Unit = {
    vkDestroyPipeline(device.get, handle, null);
    vkDestroyPipelineLayout(device.get, pipelineLayout, null);
    descriptorSetLayouts.foreach(vkDestroyDescriptorSetLayout(device.get, _, null))
  }
}
