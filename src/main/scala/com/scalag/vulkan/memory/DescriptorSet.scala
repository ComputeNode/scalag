package com.scalag.vulkan.memory;

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo

import java.nio.LongBuffer
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*

import scala.util.Using;

/** @author
  *   MarconZet Created 15.04.2020
  */
class DescriptorSet(device: Device, descriptorSetLayout: Long, descriptorPool: DescriptorPool) extends VulkanObjectHandle {

  protected val handle: Long = Using(stackPush()) { stack =>
    val pSetLayout = stack.callocLong(1).put(0, descriptorSetLayout);
    val descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
      .descriptorPool(descriptorPool.get)
      .pSetLayouts(pSetLayout);

    val pDescriptorSet = stack.callocLong(1);
    val err = vkAllocateDescriptorSets(device.get, descriptorSetAllocateInfo, pDescriptorSet);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to allocate descriptor set", err);
    pDescriptorSet.get();
  }.get

  override protected def close(): Unit =
    vkFreeDescriptorSets(device.get, descriptorPool.get, handle);
}
