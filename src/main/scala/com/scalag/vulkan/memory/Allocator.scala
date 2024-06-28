package com.scalag.vulkan.memory

import com.scalag.vulkan.core.{Device, Instance}
import com.scalag.vulkan.util.Util.{check, pushStack}
import com.scalag.vulkan.util.VulkanObjectHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.vma.Vma.{vmaCreateAllocator, vmaDestroyAllocator}
import org.lwjgl.util.vma.{VmaAllocatorCreateInfo, VmaVulkanFunctions}

/** @author
  *   MarconZet Created 13.04.2020
  */
class Allocator(instance: Instance, device: Device) extends VulkanObjectHandle {

  protected val handle: Long = pushStack { stack =>
    val functions = VmaVulkanFunctions.calloc(stack)
    functions.set(instance.get, device.get)
    val allocatorInfo = VmaAllocatorCreateInfo
      .calloc(stack)
      .device(device.get)
      .physicalDevice(device.physicalDevice)
      .instance(instance.get)
      .pVulkanFunctions(functions)

    val pAllocator = stack.callocPointer(1)
    check(vmaCreateAllocator(allocatorInfo, pAllocator), "Failed to create allocator")
    pAllocator.get(0)
  }

  def close(): Unit =
    vmaDestroyAllocator(handle)
}
