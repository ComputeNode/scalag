package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.OneTimeCommandPool;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.command.StandardCommandPool;
import com.unihogsoft.scalag.vulkan.core.DebugCallback;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.core.Instance;
import com.unihogsoft.scalag.vulkan.memory.Allocator;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;

/**
 * @author MarconZet
 * Created 13.04.2020
 */
public class VulkanContext {
    public static final String[] VALIDATION_LAYERS = {"VK_LAYER_KHRONOS_validation"};
    public static final boolean enableValidationLayers = true;

    private Instance instance;
    private DebugCallback debugCallback;
    private Device device;
    private Allocator allocator;
    private Queue computeQueue;
    private DescriptorPool descriptorPool;
    private CommandPool commandPool;

    public VulkanContext() {
        instance = new Instance();
        debugCallback = new DebugCallback(instance);
        device = new Device(instance);
        computeQueue = new Queue(device.getComputeQueueFamily(), 0, device);
        allocator = new Allocator(instance, device);
        descriptorPool = new DescriptorPool(device);
        commandPool = new StandardCommandPool(device, computeQueue);
    }
}
