package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.utility.VulkanObject;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;

/**
 * @author MarconZet
 * Created 13.04.2020
 * Copied from Wrap
 */
class Queue extends VulkanObject {
    private VkQueue queue;

    private int familyIndex;
    private int queueIndex;

    private Device device;

    Queue(int familyIndex, int queueIndex, Device device) {
        this.familyIndex = familyIndex;
        this.queueIndex = queueIndex;
        this.device = device;
        create();
    }

    synchronized int submit(VkSubmitInfo submitInfo, Fence fence) {
        return vkQueueSubmit(queue, submitInfo, fence.get());
    }

    override     protected void init() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(device.get(), familyIndex, queueIndex, pQueue);
            queue = new VkQueue(pQueue.get(0), device.get());
        }
    }

    override     protected void close() {

    }

    int getFamilyIndex() {
        return familyIndex;
    }

    VkQueue get() {
        return queue;
    }

}
