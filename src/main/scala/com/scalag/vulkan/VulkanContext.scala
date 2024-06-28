package com.scalag.vulkan

import com.scalag.vulkan.command.{CommandPool, Queue, StandardCommandPool}
import com.scalag.vulkan.core.{DebugCallback, Device, Instance}
import com.scalag.vulkan.memory.{Allocator, DescriptorPool}

/** @author
  *   MarconZet Created 13.04.2020
  */
object VulkanContext {
  val VALIDATION_LAYERS: Array[String] = Array("VK_LAYER_KHRONOS_validation")
}

class VulkanContext(val enableValidationLayers: Boolean = false) {
  val instance: Instance = new Instance(enableValidationLayers)
  val debugCallback: DebugCallback = if (enableValidationLayers) new DebugCallback(instance) else null
  val device: Device = new Device(enableValidationLayers, instance)
  val computeQueue: Queue = new Queue(device.computeQueueFamily, 0, device)
  val allocator: Allocator = new Allocator(instance, device)
  val descriptorPool: DescriptorPool = new DescriptorPool(device)
  val commandPool: CommandPool = new StandardCommandPool(device, computeQueue)

  def destroy(): Unit = {
    commandPool.destroy()
    descriptorPool.destroy()
    allocator.destroy()
    computeQueue.destroy()
    device.destroy()
    if (enableValidationLayers)
      debugCallback.destroy()
    instance.destroy()
  }
}
