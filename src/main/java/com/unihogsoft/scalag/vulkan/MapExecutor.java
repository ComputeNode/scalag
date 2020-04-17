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
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_CPU_TO_GPU;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_GPU_TO_CPU;
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
        Buffer.copyBuffer(input, inputBuffer, inputSize);
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pCommandBuffer = stack.callocPointer(1).put(0, commandBuffer);
            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack()
                    .pCommandBuffers(pCommandBuffer);

            int err = vkQueueSubmit(queue.get(), submitInfo, fence.get());
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to submit command buffer to queue", err);
            }
            fence.block().reset();
        }
        ByteBuffer output = BufferUtils.createByteBuffer(outputSize);
        Buffer.copyBuffer(outputBuffer, output, outputSize);
        return output;
    }

    private void setup() {
        try (MemoryStack stack = stackPush()) {
            //TODO this is not optimal
            inputBuffer = new Buffer(inputSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, VMA_MEMORY_USAGE_CPU_TO_GPU, allocator);
            outputBuffer = new Buffer(outputSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, VMA_MEMORY_USAGE_GPU_TO_CPU, allocator);

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
                    .dstSet(descriptorSet.get())
                    .dstBinding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .pBufferInfo(in_descriptorBufferInfo);

            writeDescriptorSet.get(1)
                    .dstSet(descriptorSet.get())
                    .dstBinding(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .pBufferInfo(out_descriptorBufferInfo);

            vkUpdateDescriptorSets(device.get(), writeDescriptorSet, null);

            VkCommandBuffer commandBuffer = commandPool.createCommandBuffer();

            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.callocStack().flags(0);

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


    public void close() {
        commandPool.freeCommandBuffer(commandBuffer);
        descriptorSet.destroy();
        inputBuffer.destroy();
        outputBuffer.destroy();
    }
}
