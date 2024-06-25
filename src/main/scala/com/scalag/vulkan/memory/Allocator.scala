package com.scalag.vulkan.memory;

import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.core.Instance;
import com.scalag.vulkan.utility.VulkanAssertionError;
import com.scalag.vulkan.utility.VulkanObjectHandle;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.vmaCreateAllocator;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * @author MarconZet
 * Created 13.04.2020
 * Copied from Wrap
 */
public class Allocator extends VulkanObjectHandle {

    private Instance instance;
    private Device device;

    public Allocator(Instance instance, Device device) {
        this.instance = instance;
        this.device = device;
        create();
    }

    @Override
    protected void init() {
        try (MemoryStack stack = stackPush()) {
            VmaVulkanFunctions functions = VmaVulkanFunctions.callocStack();
            functions.set(instance.get(), device.get());
            VmaAllocatorCreateInfo allocatorInfo = VmaAllocatorCreateInfo.create()
                    .device(device.get())
                    .physicalDevice(device.getPhysicalDevice())
                    .pVulkanFunctions(functions);

            PointerBuffer pAllocator = stack.callocPointer(1);
            int err = vmaCreateAllocator(allocatorInfo, pAllocator);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to create allocator", err);
            }
            handle = pAllocator.get(0);
        }
    }

    @Override
    protected void close() {
        vmaDestroyAllocator(handle);
    }
}
