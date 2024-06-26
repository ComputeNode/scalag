package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.utility.VulkanAssertionError;
import com.scalag.vulkan.utility.VulkanObjectHandle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 30.10.2019
 */
class Semaphore extends VulkanObjectHandle {
    private Device device;

    Semaphore(Device device) {
        this.device = device;
        create();
    }

    override     void close() {
        vkDestroySemaphore(device.get(), handle, null);
    }

    override     protected void init() {
        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            LongBuffer pointer = stack.callocLong(1);
            int err = vkCreateSemaphore(device.get(), semaphoreCreateInfo, null, pointer);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to create semaphore", err);
            }
            this.handle = pointer.get();
        }
    }
}
