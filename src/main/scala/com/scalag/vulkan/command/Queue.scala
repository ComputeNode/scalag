package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.utility.VulkanObject
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkQueue
import org.lwjgl.vulkan.VkSubmitInfo
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VK10.vkQueueSubmit

import scala.util.Using;

/** @author
  *   MarconZet Created 13.04.2020
  */
class Queue(val familyIndex: Int, queueIndex: Int, device: Device) extends VulkanObject {
  private val queue: VkQueue = Using(stackPush()) { stack =>
    val pQueue = stack.callocPointer(1);
    vkGetDeviceQueue(device.get, familyIndex, queueIndex, pQueue);
    new VkQueue(pQueue.get(0), device.get);
  }.get

  def submit(submitInfo: VkSubmitInfo, fence: Fence): Int = this.synchronized {
    vkQueueSubmit(queue, submitInfo, fence.get);
  }

  def get: VkQueue = queue

  protected def close(): Unit = ()
}
