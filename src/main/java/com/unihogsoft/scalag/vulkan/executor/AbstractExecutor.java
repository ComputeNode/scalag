package com.unihogsoft.scalag.vulkan.executor;

import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.compute.LayoutInfo;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.Allocator;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;
import com.unihogsoft.scalag.vulkan.memory.DescriptorSet;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObject;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_UNKNOWN;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public abstract class AbstractExecutor {
    private VkCommandBuffer commandBuffer;

    protected final List<DescriptorSet> descriptorSets;
    protected List<Buffer> buffers;
    protected final int dataLength;
    protected final List<BufferAction> bufferActions;


    protected final Device device;
    protected final Queue queue;
    protected final Allocator allocator;
    protected final DescriptorPool descriptorPool;
    protected final CommandPool commandPool;

    public AbstractExecutor(int dataLength, List<BufferAction> bufferActions, VulkanContext context) {
        this.device = context.getDevice();
        this.allocator = context.getAllocator();
        this.descriptorPool = context.getDescriptorPool();
        this.commandPool = context.getCommandPool();
        this.queue = context.getComputeQueue();

        this.dataLength = dataLength;
        this.buffers = new ArrayList<>();
        this.descriptorSets = new ArrayList<>();
        this.bufferActions = bufferActions;
    }

    protected abstract void setupBuffers();

    protected abstract void recordCommandBuffer(VkCommandBuffer commandBuffer);

    protected abstract int getBiggestTransportData();

    protected void setup() {
        try (MemoryStack stack = stackPush()) {
            setupBuffers();

            VkCommandBuffer commandBuffer = commandPool.createCommandBuffer();

            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to begin recording command buffer", err);
            }

            recordCommandBuffer(commandBuffer);

            err = vkEndCommandBuffer(commandBuffer);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to finish recording command buffer", err);
            }
            this.commandBuffer = commandBuffer;
        }
    }

    public List<ByteBuffer> execute(List<ByteBuffer> input) {
        Buffer stagingBuffer = new Buffer(
                getBiggestTransportData() * dataLength,
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
            Fence fence = new Fence(device);
            PointerBuffer pCommandBuffer = stack.callocPointer(1).put(0, commandBuffer);
            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(pCommandBuffer);

            int err = vkQueueSubmit(queue.get(), submitInfo, fence.get());
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to submit command buffer to queue", err);
            }
            fence.block().destroy();
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

    public void destroy() {
        commandPool.freeCommandBuffer(commandBuffer);
        descriptorSets.forEach(DescriptorSet::destroy);
        buffers.forEach(VulkanObject::destroy);
    }
}
