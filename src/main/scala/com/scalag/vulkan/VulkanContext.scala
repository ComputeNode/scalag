package com.scalag.vulkan;

import com.scalag.vulkan.command.CommandPool;
import com.scalag.vulkan.command.Queue;
import com.scalag.vulkan.command.StandardCommandPool;
import com.scalag.vulkan.core.DebugCallback;
import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.core.Instance;
import com.scalag.vulkan.memory.Allocator;
import com.scalag.vulkan.memory.DescriptorPool;
import com.scalag.vulkan.command.CommandPool;
import com.scalag.vulkan.command.Queue;
import com.scalag.vulkan.command.StandardCommandPool;
import com.scalag.vulkan.core.DebugCallback;
import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.core.Instance;
import com.scalag.vulkan.memory.Allocator;
import com.scalag.vulkan.memory.DescriptorPool;

/**
 * @author MarconZet
 * Created 13.04.2020
 */
public class VulkanContext {
  public static final String[] VALIDATION_LAYERS = {"VK_LAYER_KHRONOS_validation"};
  private final boolean enableValidationLayers;

  private final Instance instance;
  private final DebugCallback debugCallback;
  private final Device device;
  private final Allocator allocator;
  private final Queue computeQueue;
  private final DescriptorPool descriptorPool;
  private final CommandPool commandPool;

  public VulkanContext() {
    this(false);
  }

  public VulkanContext(boolean enableValidationLayers) {
    this.enableValidationLayers = enableValidationLayers;
    instance = new Instance(enableValidationLayers);
    debugCallback = (enableValidationLayers) ? new DebugCallback(instance) : null;
    device = new Device(enableValidationLayers, instance);
    computeQueue = new Queue(device.getComputeQueueFamily(), 0, device);
    allocator = new Allocator(instance, device);
    descriptorPool = new DescriptorPool(device);
    commandPool = new StandardCommandPool(device, computeQueue);
  }

  public void destroy() {
    commandPool.destroy();
    descriptorPool.destroy();
    allocator.destroy();
    computeQueue.destroy();
    device.destroy();
    if (enableValidationLayers)
      debugCallback.destroy();
    instance.destroy();
  }


  public Device getDevice() {
    return device;
  }

  public Allocator getAllocator() {
    return allocator;
  }

  public Queue getComputeQueue() {
    return computeQueue;
  }

  public DescriptorPool getDescriptorPool() {
    return descriptorPool;
  }

  public CommandPool getCommandPool() {
    return commandPool;
  }
}
