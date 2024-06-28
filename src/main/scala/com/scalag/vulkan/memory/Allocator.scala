package com.scalag.vulkan.memory;

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.core.Instance
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.vmaCreateAllocator
import org.lwjgl.util.vma.Vma.vmaDestroyAllocator
import org.lwjgl.vulkan.VK10.VK_SUCCESS

import scala.util.Using;

/** @author
  *   MarconZet Created 13.04.2020
  */
class Allocator(instance: Instance, device: Device) extends VulkanObjectHandle {

  protected val handle: Long = Using(stackPush()) { stack =>
    val functions = VmaVulkanFunctions.callocStack();
    functions.set(instance.get, device.get);
    val allocatorInfo = VmaAllocatorCreateInfo
      .calloc(stack)
      .device(device.get)
      .physicalDevice(device.physicalDevice)
      .instance(instance.get)
      .pVulkanFunctions(functions);

    val pAllocator = stack.callocPointer(1);
    val err = vmaCreateAllocator(allocatorInfo, pAllocator);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create allocator", err);
    pAllocator.get(0);
  }.get

  def close(): Unit =
    vmaDestroyAllocator(handle);
}
