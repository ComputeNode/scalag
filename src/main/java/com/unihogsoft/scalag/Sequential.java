package com.unihogsoft.scalag;

import com.unihogsoft.scalag.layers.Layer;
import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 09.05.2020
 */
public class Sequential {
    private List<Layer> layers;
    private VkCommandBuffer commandBuffer;
    private Fence fence;


    private CommandPool commandPool;
    private Queue queue;
    private Device device;

    Sequential(VulkanContext context){
        layers = new LinkedList<>();
        commandPool = context.getCommandPool();
        device = context.getDevice();
        queue = context.getComputeQueue();
    }


    public void addLayer(Layer layer){
        layer.create();
        layers.add(layer);
    }

    public void compile(){
        VkCommandBuffer commandBuffer = commandPool.createCommandBuffer();

        VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.callocStack()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(0);

        int err = vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);
        if (err != VK_SUCCESS) {
            throw new VulkanAssertionError("Failed to begin recording command buffer", err);
        }

        layers.forEach(layer -> layer.record(commandBuffer));

        err = vkEndCommandBuffer(commandBuffer);
        if (err != VK_SUCCESS) {
            throw new VulkanAssertionError("Failed to finish recording command buffer", err);
        }
        this.commandBuffer = commandBuffer;

        this.fence = new Fence(device);
    }

    public void execute(){
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
    }

    public void allocateMemory(){

    }

    public void freeMemory(){

    }

    public void destroy(){
        layers.forEach(Layer::destroy);
    }

}
