package com.scalag.vulkan.memory;

import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.utility.VulkanAssertionError;
import com.scalag.vulkan.utility.VulkanObjectHandle;
import com.scalag.vulkan.compute.ComputePipeline;
import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.utility.VulkanAssertionError;
import com.scalag.vulkan.utility.VulkanObjectHandle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 15.04.2020
 */
public class DescriptorSet extends VulkanObjectHandle {
    private final Device device;
    private final long descriptorSetLayout;
    private final DescriptorPool descriptorPool;

    public DescriptorSet(Device device, long descriptorSetLayout, DescriptorPool descriptorPool) {
        this.device = device;
        this.descriptorSetLayout = descriptorSetLayout;
        this.descriptorPool = descriptorPool;
        create();
    }

    @Override
    protected void init() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSetLayout = stack.callocLong(1).put(0, descriptorSetLayout);
            VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool.get())
                    .pSetLayouts(pSetLayout);

            LongBuffer pDescriptorSet = stack.callocLong(1);
            int err = vkAllocateDescriptorSets(device.get(), descriptorSetAllocateInfo, pDescriptorSet);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to allocate descriptor set", err);
            }
            handle = pDescriptorSet.get();
        }
    }

    @Override
    protected void close() {
        vkFreeDescriptorSets(device.get(), descriptorPool.get(), handle);
    }
}
