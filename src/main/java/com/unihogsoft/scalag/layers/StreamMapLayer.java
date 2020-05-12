package com.unihogsoft.scalag.layers;

import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.compute.BindingInfo;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;
import com.unihogsoft.scalag.vulkan.memory.DescriptorSet;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.LongBuffer;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 11.05.2020
 */
public class StreamMapLayer implements Layer {
    private ComputePipeline computePipeline;
    private DescriptorSet descriptorSet;


    private Shader shader;
    private Device device;

    public StreamMapLayer(Shader shader) {
        this.shader = shader;
    }

    @Override
    public void create(Device device, DescriptorPool descriptorPool) {
        this.device = device;
        computePipeline = new ComputePipeline(shader, device);
        descriptorSet = new DescriptorSet(device, computePipeline, descriptorPool);
    }

    @Override
    public void record(VkCommandBuffer commandBuffer, Buffer workgroup) {
        try (MemoryStack stack = stackPush()) {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.get());

            LongBuffer pDescriptorSet = stack.callocLong(1).put(0, descriptorSet.get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.getPipelineLayout(), 0, pDescriptorSet, null);

            vkCmdDispatchIndirect(commandBuffer, workgroup.get(), 0);
        }
    }

    @Override
    public List<BindingInfo> getBufferInfos() {
        return shader.getBindingInfos();
    }

    @Override
    public void bindBuffers(List<Buffer> inputData, List<Buffer> outputData, List<Buffer> additionalData) {
        List<Buffer> buffers = new LinkedList<>();
        buffers.addAll(additionalData);
        buffers.addAll(inputData);
        buffers.addAll(outputData);

        try (MemoryStack stack = stackPush()) {
            VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.callocStack(buffers.size());

            for (int i = 0; i < writeDescriptorSet.capacity(); i++) {
                VkDescriptorBufferInfo.Buffer descriptorBufferInfo = VkDescriptorBufferInfo.callocStack(1)
                        .buffer(buffers.get(i).get())
                        .offset(0)
                        .range(VK_WHOLE_SIZE);

                writeDescriptorSet.get(i)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSet.get())
                        .dstBinding(shader.getBindingInfos().get(i).getBinding())
                        .descriptorCount(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                        .pBufferInfo(descriptorBufferInfo);
            }

            vkUpdateDescriptorSets(device.get(), writeDescriptorSet, null);
        }
    }

    @Override
    public void destroy() {
        descriptorSet.destroy();
        computePipeline.destroy();
    }
}
