package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkFenceCreateInfo

import java.nio.LongBuffer
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*

import scala.util.Using;

/** @author
  *   MarconZet Created 13.04.2020 Copied from Wrap
  */
class Fence(device: Device, flags: Int = 0, onDestroy: () => Unit = () => ()) extends VulkanObjectHandle {
  protected val handle: Long = Using(stackPush()) { stack =>
    val fenceInfo = VkFenceCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
      .pNext(VK_NULL_HANDLE)
      .flags(flags);

    val pFence = stack.callocLong(1);
    val err = vkCreateFence(device.get, fenceInfo, null, pFence);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create fence", err);
    pFence.get();
  }.get

  override def close(): Unit = {
    onDestroy.apply()
    vkDestroyFence(device.get, handle, null);
  }

  def isSignaled: Boolean = {
    val result = vkGetFenceStatus(device.get, handle);
    if (!(result == VK_SUCCESS || result == VK_NOT_READY))
      throw new VulkanAssertionError("Failed to get fence status", result);
    result == VK_SUCCESS;
  }

  def reset(): Fence = {
    vkResetFences(device.get, handle);
    this;
  }

  def block(): Fence = {
    block(Long.MaxValue);
    this;
  }

  def block(timeout: Long): Boolean = {
    val err = vkWaitForFences(device.get, handle, true, timeout);
    if (err != VK_SUCCESS && err != VK_TIMEOUT)
      throw new VulkanAssertionError("Failed to wait for fences", err);
    err == VK_SUCCESS;
  }
}
