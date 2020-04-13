package com.unihogsoft.scalag.vulkan.core;

import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObject;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 13.04.2020
 */
public class Device extends VulkanObject {


    private Instance instance;

    public Device(Instance instance) {
        this.instance = instance;
        create();
    }

    @Override
    protected void init() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pPhysicalDeviceCount = stack.callocInt(1);
            int err = vkEnumeratePhysicalDevices(instance.get(), pPhysicalDeviceCount, null);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to get number of physical devices", err);
            }
            int deviceCount = pPhysicalDeviceCount.get(0);
            if (deviceCount == 0) {
                throw new AssertionError("Failed to find GPUs with Vulkan support");
            }

            PointerBuffer pPhysicalDevices = stack.callocPointer(deviceCount);
            err = vkEnumeratePhysicalDevices(instance.get(), pPhysicalDeviceCount, pPhysicalDevices);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to get physical devices", err);
            }

            int queue = getBestCompute(new VkPhysicalDevice(pPhysicalDevices.get(), instance.get()));
        }
    }

    @Override
    protected void close() {

    }

    private int getBestCompute(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pQueueFamilyCount = stack.callocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, null);
            int queueFamilyCount = pQueueFamilyCount.get(0);

            VkQueueFamilyProperties.Buffer pQueueFamilies = VkQueueFamilyProperties.callocStack(queueFamilyCount);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, pQueueFamilies);

            for (int i = 0; i < queueFamilyCount; i++) {
                VkQueueFamilyProperties queueFamily = pQueueFamilies.get(i);
                int maskedFlags = (~(VK_QUEUE_TRANSFER_BIT | VK_QUEUE_SPARSE_BINDING_BIT) & queueFamily.queueFlags());
                if (~(VK_QUEUE_GRAPHICS_BIT & maskedFlags) > 0 && (VK_QUEUE_COMPUTE_BIT & maskedFlags) > 0) {
                    return i;
                }
            }

            for (int i = 0; i < queueFamilyCount; i++) {
                VkQueueFamilyProperties queueFamily = pQueueFamilies.get(i);

                int maskedFlags = (~(VK_QUEUE_TRANSFER_BIT | VK_QUEUE_SPARSE_BINDING_BIT) & queueFamily.queueFlags());

                if ((VK_QUEUE_COMPUTE_BIT & maskedFlags) > 0) {
                    return i;
                }
            }
        }
        throw new AssertionError("No suitable queue family found for computing");
    }
}
