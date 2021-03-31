package com.unihogsoft.scalag.vulkan.executor;

import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.LayoutInfo;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.memory.*;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObject;
import com.unihogsoft.scalag.vulkan.utility.VulkanObjectHandle;
import org.joml.Vector3ic;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 15.04.2020
 */
public class MapExecutor extends AbstractExecutor {
    private final Shader shader;
    private final ComputePipeline computePipeline;

    public MapExecutor(int dataLength, List<BufferAction> bufferActions, ComputePipeline computePipeline, VulkanContext context) {
        super(dataLength, bufferActions, context);
        this.computePipeline = computePipeline;
        this.shader = computePipeline.getComputeShader();
        setup();
    }

    @Override
    protected int getBiggestTransportData() {
        return shader.getLayoutInfos().stream().mapToInt(LayoutInfo::getSize).max().orElse(0);
    }

    @Override
    protected void setupBuffers() {
        List<LayoutInfo> layoutInfos = shader.getLayoutInfos();

        for (int i = 0; i < layoutInfos.size(); i++) {
            buffers.add(
                    new Buffer(
                            layoutInfos.get(i).getSize() * dataLength,
                            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | bufferActions.get(i).getAction(),
                            0,
                            VMA_MEMORY_USAGE_GPU_ONLY,
                            allocator
                    )
            );
        }

        java.util.Queue<Buffer> bufferStack = new ArrayDeque<>(buffers);
        long[] descriptorSetLayouts = computePipeline.getDescriptorSetLayouts();
        for (int i = 0; i < descriptorSetLayouts.length; i++) {
            var descriptorSet = new DescriptorSet(device, descriptorSetLayouts[i], descriptorPool);
            var layouts = shader.getLayoutsBySets(i);

            VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.callocStack(layouts.size());

            for (int j = 0; j < writeDescriptorSet.capacity(); j++) {
                VkDescriptorBufferInfo.Buffer descriptorBufferInfo = VkDescriptorBufferInfo.callocStack(1)
                        .buffer(bufferStack.remove().get())
                        .offset(0)
                        .range(VK_WHOLE_SIZE);

                writeDescriptorSet.get(j)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSet.get())
                        .dstBinding(layouts.get(j).getBinding())
                        .descriptorCount(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                        .pBufferInfo(descriptorBufferInfo);
            }

            vkUpdateDescriptorSets(device.get(), writeDescriptorSet, null);
            descriptorSets.add(descriptorSet);
        }
    }

    @Override
    protected void recordCommandBuffer(VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.get());

            LongBuffer pDescriptorSets = stack.longs(descriptorSets.stream().mapToLong(VulkanObjectHandle::get).toArray());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.getPipelineLayout(), 0, pDescriptorSets, null);

            Vector3ic workgroup = shader.getWorkgroupDimensions();
            vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());
        }
    }
}
