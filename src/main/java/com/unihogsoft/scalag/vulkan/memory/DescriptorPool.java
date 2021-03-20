package com.unihogsoft.scalag.vulkan.memory;

import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObjectHandle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 14.04.2019
 */

public class DescriptorPool extends VulkanObjectHandle {
    private static final int MAX_SETS = 100;

    private Device device;

    public DescriptorPool(Device device) {
        this.device = device;
        create();
    }

    @Override
    protected void init() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer descriptorPoolSize = VkDescriptorPoolSize.callocStack(1);
            descriptorPoolSize.get(0)
                    .type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(2*MAX_SETS);

            VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .maxSets(MAX_SETS)
                    .flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                    .pPoolSizes(descriptorPoolSize);

            LongBuffer pDescriptorPool = stack.callocLong(1);
            int err = vkCreateDescriptorPool(device.get(), descriptorPoolCreateInfo, null, pDescriptorPool);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to create descriptor pool", err);
            }
            handle = pDescriptorPool.get();
        }
    }

    @Override
    protected void close() {
        vkDestroyDescriptorPool(device.get(), handle, null);
    }
}
