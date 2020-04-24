package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.compute.MapPipeline;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.Allocator;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;
import com.unihogsoft.scalag.vulkan.memory.DescriptorSet;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;

/**
 * @author MarconZet
 * Created 15.04.2020
 */
public class MapExecutor {
    private Buffer inputBuffer;
    private Buffer outputBuffer;
    private VkCommandBuffer commandBuffer;
    private DescriptorSet descriptorSet;
    private Fence fence;

    private MapPipeline mapPipeline;
    private int inputSize;
    private int outputSize;
    private int groupCount;

    private Device device;
    private Queue queue;
    private Allocator allocator;
    private DescriptorPool descriptorPool;
    private CommandPool commandPool;

    public MapExecutor(int inputSize, int outputSize, int groupCount, MapPipeline mapPipeline, VulkanContext context) {
        this.mapPipeline = mapPipeline;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.groupCount = groupCount;
        this.device = context.getDevice();
        this.allocator = context.getAllocator();
        this.descriptorPool = context.getDescriptorPool();
        this.commandPool = context.getCommandPool();
        this.queue = context.getComputeQueue();
        setup();
    }

    public MapExecutor(int inputSize, int outputSize, int groupCount, MapPipeline mapPipeline, Device device, Queue queue ,Allocator allocator, DescriptorPool descriptorPool, CommandPool commandPool) {
        this.mapPipeline = mapPipeline;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.groupCount = groupCount;
        this.device = device;
        this.allocator = allocator;
        this.descriptorPool = descriptorPool;
        this.commandPool = commandPool;
        this.queue = queue;
        setup();
    }

    public ByteBuffer execute(ByteBuffer input) {
        Buffer stagingBuffer = new Buffer(
                Math.max(inputSize, outputSize),
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                VMA_MEMORY_USAGE_UNKNOWN,
                allocator
        );

        Buffer.copyBuffer(input, stagingBuffer, inputSize);
        Buffer.copyBuffer(stagingBuffer, inputBuffer, inputSize, commandPool).block().destroy();

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

        ByteBuffer output = MemoryUtil.memAlloc(outputSize);
        Buffer.copyBuffer(outputBuffer, stagingBuffer, outputSize, commandPool).block().destroy();
        Buffer.copyBuffer(stagingBuffer, output, outputSize);
        stagingBuffer.destroy();
        return output;
    }

    private void setup() {
        try (MemoryStack stack = stackPush()) {
            inputBuffer = new Buffer(
                    inputSize,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    0,
                    VMA_MEMORY_USAGE_GPU_ONLY,
                    allocator
            );
            outputBuffer = new Buffer(
                    outputSize,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    0,
                    VMA_MEMORY_USAGE_GPU_ONLY,
                    allocator
            );

            VkDescriptorBufferInfo.Buffer in_descriptorBufferInfo = VkDescriptorBufferInfo.callocStack(1)
                    .buffer(inputBuffer.get())
                    .offset(0)
                    .range(VK_WHOLE_SIZE);

            VkDescriptorBufferInfo.Buffer out_descriptorBufferInfo = VkDescriptorBufferInfo.callocStack(1)
                    .buffer(outputBuffer.get())
                    .offset(0)
                    .range(VK_WHOLE_SIZE);

            descriptorSet = new DescriptorSet(device, mapPipeline, descriptorPool);

            VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.callocStack(2);
            writeDescriptorSet.get(0)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSet.get())
                    .dstBinding(0)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .pBufferInfo(in_descriptorBufferInfo);

            writeDescriptorSet.get(1)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSet.get())
                    .dstBinding(1)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .pBufferInfo(out_descriptorBufferInfo);

            vkUpdateDescriptorSets(device.get(), writeDescriptorSet, null);

            VkCommandBuffer commandBuffer = commandPool.createCommandBuffer();

            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to begin recording command buffer", err);
            }

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, mapPipeline.get());

            LongBuffer pDescriptorSet = stack.callocLong(1).put(0, descriptorSet.get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, mapPipeline.getPipelineLayout(), 0, pDescriptorSet, null);

            vkCmdDispatch(commandBuffer, groupCount, 1, 1);

            err = vkEndCommandBuffer(commandBuffer);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to finish recording command buffer", err);
            }
            this.commandBuffer = commandBuffer;

            this.fence = new Fence(device);
        }
    }

    public void destroy() {
        fence.destroy();
        commandPool.freeCommandBuffer(commandBuffer);
        descriptorSet.destroy();
        inputBuffer.destroy();
        outputBuffer.destroy();
    }
}
