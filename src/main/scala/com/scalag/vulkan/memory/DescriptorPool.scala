package com.scalag.vulkan.memory;

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.memory.DescriptorPool.MAX_SETS
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo
import org.lwjgl.vulkan.VkDescriptorPoolSize

import java.nio.LongBuffer
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*

import scala.util.Using;

/** @author
  *   MarconZet Created 14.04.2019
  */
object DescriptorPool {
  val MAX_SETS = 100;
}
class DescriptorPool(device: Device) extends VulkanObjectHandle {
  protected val handle: Long = Using(stackPush()) { stack =>
    val descriptorPoolSize = VkDescriptorPoolSize.callocStack(1);
    descriptorPoolSize
      .get(0)
      .`type`(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
      .descriptorCount(2 * MAX_SETS);

    val descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
      .maxSets(MAX_SETS)
      .flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
      .pPoolSizes(descriptorPoolSize);

    val pDescriptorPool = stack.callocLong(1);
    val err = vkCreateDescriptorPool(device.get, descriptorPoolCreateInfo, null, pDescriptorPool);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create descriptor pool", err);
    pDescriptorPool.get();
  }.get

  override protected def close(): Unit =
    vkDestroyDescriptorPool(device.get, handle, null);
}
