package com.scalag.vulkan.compute;

import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.utility.VulkanAssertionError;
import com.scalag.vulkan.utility.VulkanObjectHandle;
import com.scalag.vulkan.VulkanContext;
import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.utility.VulkanAssertionError;
import com.scalag.vulkan.utility.VulkanObjectHandle;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;

/**
 * @author MarconZet
 * Created 14.04.2020
 */
public class ComputePipeline extends VulkanObjectHandle {
    @Getter
    private long[] descriptorSetLayouts;
    @Getter
    private long pipelineLayout;
    @Getter
    private final Shader computeShader;

    private final Device device;

    public ComputePipeline(Shader computeShader, VulkanContext context) {
        this.computeShader = computeShader;
        this.device = context.getDevice();
        create();
    }

    @Override
    protected void init() {
        try (MemoryStack stack = stackPush()) {
            List<List<LayoutInfo>> layoutInfos = computeShader.getLayoutsBySets();
            descriptorSetLayouts = layoutInfos.stream().mapToLong(this::createDescriptorSetLayout).toArray();

            VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .pSetLayouts(stack.longs(descriptorSetLayouts))
                    .pPushConstantRanges(null);

            LongBuffer pPipelineLayout = stack.callocLong(1);
            int err = vkCreatePipelineLayout(device.get(), pipelineLayoutCreateInfo, null, pPipelineLayout);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to create pipeline layout", err);
            }
            pipelineLayout = pPipelineLayout.get(0);

            VkPipelineShaderStageCreateInfo pipelineShaderStageCreateInfo = VkPipelineShaderStageCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .stage(VK_SHADER_STAGE_COMPUTE_BIT)
                    .module(computeShader.get())
                    .pName(stack.ASCII(computeShader.getFunctionName()));

            VkComputePipelineCreateInfo.Buffer computePipelineCreateInfo = VkComputePipelineCreateInfo.callocStack(1);
            computePipelineCreateInfo.get(0)
                    .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .stage(pipelineShaderStageCreateInfo)
                    .layout(pipelineLayout)
                    .basePipelineHandle(0)
                    .basePipelineIndex(0);

            LongBuffer pPipeline = stack.callocLong(1);
            err = vkCreateComputePipelines(device.get(), 0, computePipelineCreateInfo, null, pPipeline);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to create compute pipeline", err);
            }
            handle = pPipeline.get(0);
        }
    }

    private long createDescriptorSetLayout(List<LayoutInfo> layoutInfos) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.callocStack(layoutInfos.size());
            for (LayoutInfo layoutInfo : layoutInfos) {
                descriptorSetLayoutBindings.get()
                        .binding(layoutInfo.getBinding())
                        .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                        .descriptorCount(1)
                        .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
                        .pImmutableSamplers(null);
            }
            descriptorSetLayoutBindings.flip();

            VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .pBindings(descriptorSetLayoutBindings);

            LongBuffer pDescriptorSetLayout = stack.callocLong(1);
            int err = vkCreateDescriptorSetLayout(device.get(), descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to create descriptor set layout", err);
            }
            return pDescriptorSetLayout.get(0);
        }
    }

    @Override
    protected void close() {
        vkDestroyPipeline(device.get(), handle, null);
        vkDestroyPipelineLayout(device.get(), pipelineLayout, null);
        for (long l : descriptorSetLayouts) {
            vkDestroyDescriptorSetLayout(device.get(), l, null);
        }
    }
}
