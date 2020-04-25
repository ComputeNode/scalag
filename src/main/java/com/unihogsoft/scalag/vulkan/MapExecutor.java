package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.compute.MapPipeline;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.*;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.unihogsoft.scalag.vulkan.memory.BindingInfo.OP_DO_NOTHING;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;

/**
 * @author MarconZet
 * Created 15.04.2020
 */
public class MapExecutor {
    private List<Buffer> buffers;
    private VkCommandBuffer commandBuffer;
    private DescriptorSet descriptorSet;
    private Fence fence;

    private final int groupCount;
    private final Map<Integer, Integer> operations;
    private final Shader shader;
    private final MapPipeline mapPipeline;

    private final Device device;
    private final Queue queue;
    private final Allocator allocator;
    private final DescriptorPool descriptorPool;
    private final CommandPool commandPool;

    public MapExecutor(int groupCount, Map<Integer, Integer> operations, MapPipeline mapPipeline, VulkanContext context) {
        this.mapPipeline = mapPipeline;
        this.groupCount = groupCount;
        this.operations = operations;
        this.shader = mapPipeline.getComputeShader();
        this.device = context.getDevice();
        this.allocator = context.getAllocator();
        this.descriptorPool = context.getDescriptorPool();
        this.commandPool = context.getCommandPool();
        this.queue = context.getComputeQueue();
        setup();
    }

    private void setup() {
        try (MemoryStack stack = stackPush()) {
            List<BindingInfo> bindingInfos = shader.getInputSizes();
            buffers = bindingInfos.stream().map(bindingInfo ->
                    new Buffer(
                            bindingInfo.getSize() * groupCount,
                            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | operations.getOrDefault(bindingInfo.getBinding(), OP_DO_NOTHING),
                            0,
                            VMA_MEMORY_USAGE_GPU_ONLY,
                            allocator
                    )
            ).collect(Collectors.toList());

            descriptorSet = new DescriptorSet(device, mapPipeline, descriptorPool);

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

    public void destroy() {
        fence.destroy();
        commandPool.freeCommandBuffer(commandBuffer);
        descriptorSet.destroy();
        inputBuffer.destroy();
        outputBuffer.destroy();
    }
}
