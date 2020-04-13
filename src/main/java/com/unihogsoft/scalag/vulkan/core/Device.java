package com.unihogsoft.scalag.vulkan.core;

import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObject;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.unihogsoft.scalag.vulkan.Context.VALIDATION_LAYERS;
import static com.unihogsoft.scalag.vulkan.Context.enableValidationLayers;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 13.04.2020
 */
public class Device extends VulkanObject {
    private static final String[] DEVICE_EXTENSIONS = {VK_KHR_SWAPCHAIN_EXTENSION_NAME, VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME};

    private VkDevice device;
    private VkPhysicalDevice physicalDevice;

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

            physicalDevice = new VkPhysicalDevice(pPhysicalDevices.get(), instance.get());
            int computeFamily = getBestCompute(physicalDevice);

            FloatBuffer pQueuePriorities = stack.callocFloat(1).put(1.0f);
            pQueuePriorities.flip();

            VkDeviceQueueCreateInfo.Buffer pQueueCreateInfo = VkDeviceQueueCreateInfo.callocStack(1).put(
                    VkDeviceQueueCreateInfo.callocStack()
                            .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                            .pNext(0)
                            .flags(0)
                            .queueFamilyIndex(computeFamily)
                            .pQueuePriorities(pQueuePriorities)
            );

            PointerBuffer ppExtensionNames = stack.callocPointer(DEVICE_EXTENSIONS.length);
            for (String extension : DEVICE_EXTENSIONS) {
                ppExtensionNames.put(stack.ASCII(extension));
            }
            ppExtensionNames.flip();

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.callocStack();
            VkDeviceCreateInfo pCreateInfo = VkDeviceCreateInfo.create()
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pNext(NULL)
                    .pQueueCreateInfos(pQueueCreateInfo)
                    .pEnabledFeatures(deviceFeatures)
                    .ppEnabledExtensionNames(ppExtensionNames);

            if (enableValidationLayers) {
                PointerBuffer ppValidationLayers = stack.callocPointer(VALIDATION_LAYERS.length);
                for (String layer : VALIDATION_LAYERS) {
                    ppValidationLayers.put(stack.ASCII(layer));
                }
                pCreateInfo.ppEnabledLayerNames(ppValidationLayers.flip());
            }

            PointerBuffer pDevice = stack.callocPointer(1);
            err = vkCreateDevice(physicalDevice, pCreateInfo, null, pDevice);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to create device", err);
            }
            device = new VkDevice(pDevice.get(0), physicalDevice, pCreateInfo);
        }
    }

    @Override
    protected void close() {
        vkDestroyDevice(device, null);
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

    public VkDevice get(){
        return device;
    }
}
