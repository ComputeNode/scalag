package com.unihogsoft.scalag.vulkan.executor;

import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.LayoutInfo;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.*;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObject;
import com.unihogsoft.scalag.vulkan.utility.VulkanObjectHandle;
import org.joml.Vector3ic;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;

/**
 * @author MarconZet
 * Created 15.04.2020
 */
public class MapExecutor implements Executor {
    private List<Buffer> buffers;
    private VkCommandBuffer commandBuffer;
    private List<DescriptorSet> descriptorSets;
    private Fence fence;

    private final int groupCount;
    private final List<BufferAction> bufferActions;
    private final Shader shader;
    private final ComputePipeline computePipeline;

    private final Device device;
    private final Queue queue;
    private final Allocator allocator;
    private final DescriptorPool descriptorPool;
    private final CommandPool commandPool;

    public MapExecutor(int groupCount, List<BufferAction> bufferActions, ComputePipeline computePipeline, VulkanContext context) {
        this.computePipeline = computePipeline;
        this.groupCount = groupCount;
        this.bufferActions = bufferActions;
        this.shader = computePipeline.getComputeShader();
        this.device = context.getDevice();
        this.allocator = context.getAllocator();
        this.descriptorPool = context.getDescriptorPool();
        this.commandPool = context.getCommandPool();
        this.queue = context.getComputeQueue();
        this.buffers = new ArrayList<>();
        this.descriptorSets = new ArrayList<>();
        setup();
    }

    private void setup() {
        try (MemoryStack stack = stackPush()) {
            List<LayoutInfo> layoutInfos = shader.getLayoutInfos();

            for (int i = 0; i < layoutInfos.size(); i++) {
                buffers.add(
                        new Buffer(
                                layoutInfos.get(i).getSize() * groupCount,
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

            VkCommandBuffer commandBuffer = commandPool.createCommandBuffer();

            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to begin recording command buffer", err);
            }

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.get());

            LongBuffer pDescriptorSets = stack.longs(descriptorSets.stream().mapToLong(VulkanObjectHandle::get).toArray());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.getPipelineLayout(), 0, pDescriptorSets, null);

            Vector3ic workgroup = shader.getWorkgroupDimensions();
            vkCmdDispatch(commandBuffer, groupCount / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());

            err = vkEndCommandBuffer(commandBuffer);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to finish recording command buffer", err);
            }
            this.commandBuffer = commandBuffer;

            this.fence = new Fence(device);
        }
    }

    @Override
    public List<ByteBuffer> execute(List<ByteBuffer> input) {
        Buffer stagingBuffer = new Buffer(
                shader.getLayoutInfos().stream().mapToInt(LayoutInfo::getSize).max().orElse(0) * groupCount,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                VMA_MEMORY_USAGE_UNKNOWN,
                allocator
        );

        for (int i = 0; i < bufferActions.size(); i++) {
            if (bufferActions.get(i).getAction() == BufferAction.LOAD_INTO) {
                ByteBuffer buffer = input.get(i);
                Buffer.copyBuffer(buffer, stagingBuffer, buffer.remaining());
                Buffer.copyBuffer(stagingBuffer, buffers.get(i), buffer.remaining(), commandPool).block().destroy();
            }
        }

        try (MemoryStack stack = stackPush()) {
            PointerBuffer pCommandBuffer = stack.callocPointer(1).put(0, commandBuffer);
            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(pCommandBuffer);

            int err = vkQueueSubmit(queue.get(), submitInfo, fence.get());
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to submit command buffer to queue", err);
            }
            fence.block().reset();
        }

        List<ByteBuffer> output = new ArrayList<>();
        for (int i = 0; i < bufferActions.size(); i++) {
            if (bufferActions.get(i).getAction() == BufferAction.LOAD_FROM) {
                Fence fence = Buffer.copyBuffer(buffers.get(i), stagingBuffer, buffers.get(i).getSize(), commandPool);
                var outBuffer = BufferUtils.createByteBuffer(buffers.get(i).getSize());
                fence.block().destroy();
                Buffer.copyBuffer(stagingBuffer, outBuffer, outBuffer.remaining());
                output.add(outBuffer);
            }
        }
        stagingBuffer.destroy();
        return output;
    }

    @Override
    public void destroy() {
        fence.destroy();
        commandPool.freeCommandBuffer(commandBuffer);
        descriptorSets.forEach(DescriptorSet::destroy);
        buffers.forEach(VulkanObject::destroy);
    }
}
