package com.unihogsoft.scalag.vulkan.command;

import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObject;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 13.04.2020
 * Copied from Wrap
 */
public class Queue extends VulkanObject {
    private VkQueue queue;

    private int familyIndex;
    private int queueIndex;

    private Device device;

    public Queue(int familyIndex, int queueIndex, Device device) {
        this.familyIndex = familyIndex;
        this.queueIndex = queueIndex;
        this.device = device;
        create();
    }

    public synchronized void submit(VkSubmitInfo submitInfo, Fence fence) {
        int err = vkQueueSubmit(queue, submitInfo, fence.get());
        if (err != VK_SUCCESS) {
            throw new VulkanAssertionError("Failed to submit command buffer to queue", err);
        }
    }

    @Override
    protected void init() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(device.get(), familyIndex, queueIndex, pQueue);
            queue = new VkQueue(pQueue.get(0), device.get());
        }
    }

    @Override
    protected void close() {

    }

    public int getFamilyIndex() {
        return familyIndex;
    }

    public VkQueue get() {
        return queue;
    }

}
