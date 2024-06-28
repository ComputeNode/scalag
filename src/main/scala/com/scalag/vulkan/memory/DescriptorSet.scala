package com.scalag.vulkan.memory

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.util.Util.{check, pushStack}
import com.scalag.vulkan.util.{VulkanAssertionError, VulkanObjectHandle}
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo

import java.nio.LongBuffer
import scala.util.Using

/** @author
  *   MarconZet Created 15.04.2020
  */
class DescriptorSet(device: Device, descriptorSetLayout: Long, descriptorPool: DescriptorPool) extends VulkanObjectHandle {

  protected val handle: Long = pushStack { stack =>
    val pSetLayout = stack.callocLong(1).put(0, descriptorSetLayout)
    val descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo
      .calloc(stack)
      .sType$Default()
      .descriptorPool(descriptorPool.get)
      .pSetLayouts(pSetLayout)

    val pDescriptorSet = stack.callocLong(1)
    check(vkAllocateDescriptorSets(device.get, descriptorSetAllocateInfo, pDescriptorSet), "Failed to allocate descriptor set")
    pDescriptorSet.get()
  }

  override protected def close(): Unit =
    vkFreeDescriptorSets(device.get, descriptorPool.get, handle)
}
