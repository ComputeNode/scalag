package com.scalag.vulkan.command

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.util.Util.{check, pushStack}
import com.scalag.vulkan.util.{VulkanAssertionError, VulkanObjectHandle}
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkSemaphoreCreateInfo

import scala.util.Using

/** @author
  *   MarconZet Created 30.10.2019
  */
class Semaphore(device: Device) extends VulkanObjectHandle {
  protected val handle: Long = pushStack { stack =>
    val semaphoreCreateInfo = VkSemaphoreCreateInfo
      .calloc(stack)
      .sType$Default()
    val pointer = stack.callocLong(1)
    check(vkCreateSemaphore(device.get, semaphoreCreateInfo, null, pointer), "Failed to create semaphore")
    pointer.get()
  }

  def close(): Unit =
    vkDestroySemaphore(device.get, handle, null)

}
