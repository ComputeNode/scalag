package com.unihogsoft.scalag.vulkan.compute;

import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.BindingInfo;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;
import com.unihogsoft.scalag.vulkan.memory.DescriptorSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;

public class SortPipelines {
    private final ComputePipeline keyPipeline;
    private final ComputePipeline sortPipeline;
    private final ComputePipeline copyPipeline;

    public SortPipelines(ComputePipeline keyPipeline, ComputePipeline sortPipeline, ComputePipeline copyPipeline) {
        this.keyPipeline = keyPipeline;
        this.sortPipeline = sortPipeline;
        this.copyPipeline = copyPipeline;
        verify();
    }

    public List<DescriptorSet> createDescriptorSets(List<Buffer> buffer, DescriptorPool descriptorPool, Device device) {
        return Arrays.asList(
                bindTo(keyPipeline, buffer.subList(0, 2), descriptorPool, device),
                bindTo(sortPipeline, buffer.subList(1, 2), descriptorPool, device),
                bindTo(copyPipeline, Arrays.asList(buffer.get(0), buffer.get(2), buffer.get(3)), descriptorPool, device)
        );
    }

    private DescriptorSet bindTo(ComputePipeline pipeline, List<Buffer> buffers, DescriptorPool descriptorPool, Device device) {
        DescriptorSet descriptorSet = new DescriptorSet(device, pipeline, descriptorPool);

        List<BindingInfo> bindingInfos = pipeline.getComputeShader().getBindingInfos();
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
                        .dstBinding(bindingInfos.get(i).getBinding())
                        .descriptorCount(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                        .pBufferInfo(descriptorBufferInfo);
            }

            vkUpdateDescriptorSets(device.get(), writeDescriptorSet, null);
        }
        return descriptorSet;
    }


    private void verify() {
        verifyShader(keyPipeline.getComputeShader(), 1, 2);
        verifyShader(copyPipeline.getComputeShader(), 2, 1);
        verifyShader(sortPipeline.getComputeShader(), 1, 1);
    }

    private void verifyShader(Shader shader, int in, int out) {
        if (shader.getInputNumber() != in || shader.getOutputNumber() != out)
            throw new IllegalArgumentException();
    }

    public int getDataSize() {
        return keyPipeline.getComputeShader().getBindingInfos().get(0).getSize();
    }

    public ComputePipeline getKeyPipeline() {
        return keyPipeline;
    }

    public ComputePipeline getSortPipeline() {
        return sortPipeline;
    }

    public ComputePipeline getCopyPipeline() {
        return copyPipeline;
    }
}
