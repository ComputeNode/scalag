package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.utility.VulkanAssertionError;
import com.scalag.vulkan.utility.VulkanObjectHandle;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 13.04.2020
 * Copied from Wrap
 */
abstract class CommandPool extends VulkanObjectHandle {
    private long commandPool;

    private Device device;
    private Queue queue;

    CommandPool(Device device, Queue queue) {
        this.device = device;
        this.queue = queue;
        create();
    }

    override     protected void init() {
        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo createInfo = VkCommandPoolCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .pNext(VK_NULL_HANDLE)
                    .queueFamilyIndex(queue.getFamilyIndex())
                    .flags(getFlags());

            LongBuffer pCommandPoll = stack.callocLong(1);
            int err = vkCreateCommandPool(device.get(), createInfo, null, pCommandPoll);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to create command pool", err);
            }
            commandPool = pCommandPoll.get();
        }
    }

    VkCommandBuffer[] createCommandBuffer(int n) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer[] commandBuffers = new VkCommandBuffer[n];
            VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(commandBuffers.length);

            PointerBuffer pointerBuffer = stack.callocPointer(commandBuffers.length);
            int err = vkAllocateCommandBuffers(device.get(), allocateInfo, pointerBuffer);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to allocate command buffers", err);
            }

            for (int i = 0; i < commandBuffers.length; i++) {
                long l = pointerBuffer.get();
                commandBuffers[i] = new VkCommandBuffer(l, device.get());
            }
            return commandBuffers;
        }
    }

    VkCommandBuffer createCommandBuffer() {
        return createCommandBuffer(1)[0];
    }

    VkCommandBuffer beginSingleTimeCommands() {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = this.createCommandBuffer();

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to begin single time command buffer", err);
            }
            return commandBuffer;
        }
    }

    Fence endSingleTimeCommands(VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            vkEndCommandBuffer(commandBuffer);

            PointerBuffer pointerBuffer = stack.callocPointer(1).put(0, commandBuffer);
            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(pointerBuffer);

            Fence fence = new Fence(device);
            queue.submit(submitInfo, fence);
            return fence.onDestroy(() -> freeCommandBuffer(commandBuffer));
        }
    }

    void freeCommandBuffer(VkCommandBuffer... commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pointerBuffer = stack.callocPointer(commandBuffer.length);
            for (VkCommandBuffer buffer : commandBuffer) {
                pointerBuffer.put(buffer);
            }
            pointerBuffer.flip();
            vkFreeCommandBuffers(device.get(), commandPool, pointerBuffer);
        }
    }

    override     protected void close() {
        vkDestroyCommandPool(device.get(), commandPool, null);
    }

    protected abstract int getFlags();

    long get() {
        return commandPool;
    }
}
