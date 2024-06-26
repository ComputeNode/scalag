package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkSemaphoreCreateInfo

import java.nio.LongBuffer
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*

import scala.util.Using;

/** @author
  *   MarconZet Created 30.10.2019
  */
class Semaphore(device: Device) extends VulkanObjectHandle {
  def close(): Unit =
    vkDestroySemaphore(device.get, handle, null);

  protected val handle: Long = Using(stackPush()) { stack =>
    val semaphoreCreateInfo = VkSemaphoreCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
    val pointer = stack.callocLong(1);
    val err = vkCreateSemaphore(device.get, semaphoreCreateInfo, null, pointer);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create semaphore", err);
    pointer.get();
  }.get

}
