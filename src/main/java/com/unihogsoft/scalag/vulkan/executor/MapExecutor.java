package com.unihogsoft.scalag.vulkan.executor;

import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.*;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObject;
import org.joml.Vector3ic;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.stream.Collectors;

import static com.unihogsoft.scalag.vulkan.memory.BindingInfo.BINDING_TYPE_INPUT;
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
    private DescriptorSet descriptorSet;
    private Fence fence;

    private final int groupCount;
    private final Shader shader;
    private final ComputePipeline computePipeline;

    private final Device device;
    private final Queue queue;
    private final Allocator allocator;
    private final DescriptorPool descriptorPool;
    private final CommandPool commandPool;

    public MapExecutor(int groupCount, ComputePipeline computePipeline, VulkanContext context) {
        this.computePipeline = computePipeline;
        this.groupCount = groupCount;
        this.shader = computePipeline.getComputeShader();
        this.device = context.getDevice();
        this.allocator = context.getAllocator();
        this.descriptorPool = context.getDescriptorPool();
        this.commandPool = context.getCommandPool();
        this.queue = context.getComputeQueue();
        setup();
    }

    private void setup() {
        try (MemoryStack stack = stackPush()) {
            List<BindingInfo> bindingInfos = shader.getBindingInfos();
            buffers = bindingInfos.stream().map(bindingInfo ->
                    new Buffer(
                            bindingInfo.getSize() * groupCount,
                            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | (bindingInfo.getType() == BINDING_TYPE_INPUT ? VK_BUFFER_USAGE_TRANSFER_DST_BIT : VK_BUFFER_USAGE_TRANSFER_SRC_BIT),
                            0,
                            VMA_MEMORY_USAGE_GPU_ONLY,
                            allocator
                    )
            ).collect(Collectors.toList());

            descriptorSet = new DescriptorSet(device, computePipeline, descriptorPool);

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

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.get());

            LongBuffer pDescriptorSet = stack.callocLong(1).put(0, descriptorSet.get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.getPipelineLayout(), 0, pDescriptorSet, null);

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
    public ByteBuffer[] execute(ByteBuffer[] input) {
        Buffer stagingBuffer = new Buffer(
                shader.getBindingInfos().stream().mapToInt(BindingInfo::getSize).max().orElse(0) * groupCount,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                VMA_MEMORY_USAGE_UNKNOWN,
                allocator
        );

        for (int i = 0; i < input.length; i++) {
            ByteBuffer buffer = input[i];
            Buffer.copyBuffer(buffer, stagingBuffer, buffer.remaining());
            Buffer.copyBuffer(stagingBuffer, buffers.get(i), buffer.remaining(), commandPool).block().destroy();
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

        ByteBuffer[] output = new ByteBuffer[shader.getOutputNumber()];
        int offset = shader.getInputNumber();
        for (int i = offset; i < shader.getBindingInfos().size(); i++) {
            Fence fence = Buffer.copyBuffer(buffers.get(i), stagingBuffer, buffers.get(i).getSize(), commandPool);
            output[i - offset] = BufferUtils.createByteBuffer(buffers.get(i).getSize());
            fence.block().destroy();
            Buffer.copyBuffer(stagingBuffer, output[i - offset], output[i - offset].remaining());
        }
        stagingBuffer.destroy();
        return output;
    }

    @Override
    public void destroy() {
        fence.destroy();
        commandPool.freeCommandBuffer(commandBuffer);
        descriptorSet.destroy();
        buffers.forEach(VulkanObject::destroy);
    }
}
