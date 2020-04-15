package com.unihogsoft.scalag.vulkan.memory;

import com.unihogsoft.scalag.vulkan.compute.MapPipeline;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObjectHandle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 15.04.2020
 */
public class MapDescriptorSet extends VulkanObjectHandle {
    private Device device;
    private MapPipeline pipeline;
    private DescriptorPool descriptorPool;

    public MapDescriptorSet(Device device, MapPipeline pipeline, DescriptorPool descriptorPool) {
        this.device = device;
        this.pipeline = pipeline;
        this.descriptorPool = descriptorPool;
        create();
    }

    @Override
    protected void init() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSetLayout = stack.callocLong(1).put(0, pipeline.getDescriptorSetLayout());
            VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.callocStack()
                    .descriptorPool(descriptorPool.get())
                    .pSetLayouts(pSetLayout);

            LongBuffer pDescriptorSet = stack.callocLong(1);
            int err = vkAllocateDescriptorSets(device.get(), descriptorSetAllocateInfo, pDescriptorSet);
            if(err != VK_SUCCESS){
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
