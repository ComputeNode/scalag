package com.unihogsoft.scalag.vulkan.compute;

import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObjectHandle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 14.04.2020
 */
public class MapPipeline extends VulkanObjectHandle {
    private int[] computeShader;

    private long shaderModule;
    private long descriptorSetLayout;
    private long pipelineLayout;

    private Device device;

    public MapPipeline(int[] computeShader, Device device) {
        this.computeShader = computeShader;
        this.device = device;
        create();
    }

    @Override
    protected void init() {
        try(MemoryStack stack = stackPush()){
            ByteBuffer shader = stack.calloc(4*computeShader.length);
            shader.asIntBuffer().put(computeShader).flip();
            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .pCode(shader);

            LongBuffer pShaderModule = stack.callocLong(1);
            int err = vkCreateShaderModule(device.get(), shaderModuleCreateInfo, null, pShaderModule);
            if(err != VK_SUCCESS){
                throw new VulkanAssertionError("Failed to create shader module",  err);
            }
            shaderModule = pShaderModule.get();

            VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.callocStack(2);
            descriptorSetLayoutBindings.get(0)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
                    .pImmutableSamplers(null);
            descriptorSetLayoutBindings.get(1)
                    .binding(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
                    .pImmutableSamplers(null);

            VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .pBindings(descriptorSetLayoutBindings);

            LongBuffer pDescriptorSetLayout = stack.callocLong(1);
            err = vkCreateDescriptorSetLayout(device.get(), descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout);
            if(err != VK_SUCCESS){
                throw new VulkanAssertionError("Failed to create descriptor set layout", err);
            }
            descriptorSetLayout = pDescriptorSetLayout.get(0);

            VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .pSetLayouts(pDescriptorSetLayout)
                    .pPushConstantRanges(null);

            LongBuffer pPipelineLayout  = stack.callocLong(1);
            err = vkCreatePipelineLayout(device.get(), pipelineLayoutCreateInfo, null, pPipelineLayout);
            if(err != VK_SUCCESS){
                throw new VulkanAssertionError("Failed to create pipeline layout", err);
            }
            pipelineLayout = pPipelineLayout.get(0);

            VkPipelineShaderStageCreateInfo pipelineShaderStageCreateInfo = VkPipelineShaderStageCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .stage(VK_SHADER_STAGE_COMPUTE_BIT)
                    .module(shaderModule)
                    .pName(stack.ASCII("map"));

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
            if(err != VK_SUCCESS){
                throw new VulkanAssertionError("Failed to create compute pipeline", err);
            }
            handle = pPipeline.get(0);
        }
    }

    @Override
    protected void close() {
        vkDestroyPipeline(device.get(), handle, null);
        vkDestroyPipelineLayout(device.get(), pipelineLayout, null);
        vkDestroyShaderModule(device.get(), shaderModule, null);
        vkDestroyDescriptorSetLayout(device.get(), descriptorSetLayout, null);
    }

    public long getDescriptorSetLayout() {
        return descriptorSetLayout;
    }
}
