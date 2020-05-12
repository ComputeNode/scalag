package com.unihogsoft.scalag;

import com.unihogsoft.scalag.layers.Layer;
import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.compute.BindingInfo;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.Allocator;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObject;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDispatchIndirectCommand;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.unihogsoft.scalag.vulkan.compute.BindingInfo.BINDING_TYPE_INPUT;
import static com.unihogsoft.scalag.vulkan.compute.BindingInfo.BINDING_TYPE_OUTPUT;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 09.05.2020
 */
public class Sequential {
    private List<Layer> layers;
    private List<Buffer> buffers;
    private List<Buffer> topOfLineBuffers;
    private VkCommandBuffer commandBuffer;
    private Fence fence;
    private Buffer workgroup;

    private DescriptorPool descriptorPool;
    private CommandPool commandPool;
    private Queue queue;
    private Allocator allocator;
    private Device device;

    Sequential(VulkanContext context) {
        layers = new LinkedList<>();
        buffers = new LinkedList<>();
        commandPool = context.getCommandPool();
        device = context.getDevice();
        queue = context.getComputeQueue();
        descriptorPool = context.getDescriptorPool();
    }


    public void addLayer(Layer layer) {
        layer.create(device, descriptorPool);
        layers.add(layer);
    }

    public void compile() {
        VkCommandBuffer commandBuffer = commandPool.createCommandBuffer();

        VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.callocStack()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(0);

        int err = vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);
        if (err != VK_SUCCESS) {
            throw new VulkanAssertionError("Failed to begin recording command buffer", err);
        }

        workgroup = new Buffer(
                4 * 3,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                VMA_MEMORY_USAGE_UNKNOWN,
                allocator
        );

        layers.forEach(layer -> layer.record(commandBuffer, workgroup));

        err = vkEndCommandBuffer(commandBuffer);
        if (err != VK_SUCCESS) {
            throw new VulkanAssertionError("Failed to finish recording command buffer", err);
        }
        this.commandBuffer = commandBuffer;

        this.fence = new Fence(device);
    }

    public Fence execute() {
        try (MemoryStack stack = stackPush()) {
            VkDispatchIndirectCommand indirectCommand = VkDispatchIndirectCommand.callocStack().set(10, 1, 1);
            int indirectSize = VkDispatchIndirectCommand.SIZEOF;
            ByteBuffer byteBuffer = stack.calloc(indirectSize);
            memCopy(memAddress(byteBuffer), indirectCommand.address(), indirectSize);
            Buffer.copyBuffer(byteBuffer, workgroup, indirectSize);

            PointerBuffer pCommandBuffer = stack.callocPointer(1).put(0, commandBuffer);
            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(pCommandBuffer);

            queue.submit(submitInfo, fence);
            return fence;
        }
    }

    public void allocateMemory(List<ByteBuffer> additionalData, int length) {
        Buffer stagingBuffer = new Buffer(
                additionalData.stream().mapToInt(ByteBuffer::remaining).max().orElse(0),
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                0,
                VMA_MEMORY_USAGE_CPU_TO_GPU,
                allocator
        );
        List<Buffer> additionalBuffers = additionalData.stream().map(byteBuffer -> {
            int size = byteBuffer.remaining();
            Buffer.copyBuffer(byteBuffer, stagingBuffer, size);
            Buffer gpuData = new Buffer(
                    size,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    0,
                    VMA_MEMORY_USAGE_GPU_ONLY,
                    allocator
            );
            buffers.add(gpuData);
            Buffer.copyBuffer(stagingBuffer, gpuData, size, commandPool).block().destroy();
            return gpuData;
        }).collect(Collectors.toList());
        buffers.addAll(additionalBuffers);

        topOfLineBuffers = layers.get(0)
                .getBufferInfos()
                .stream()
                .filter(x -> x.getType() == BINDING_TYPE_INPUT)
                .map(bindingInfo ->
                        new Buffer(
                                length * bindingInfo.getSize(),
                                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                                0,
                                VMA_MEMORY_USAGE_GPU_ONLY,
                                allocator
                        ))
                .collect(Collectors.toList());
        buffers.addAll(topOfLineBuffers);

        List<Buffer> previousBuffers = topOfLineBuffers;

        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);

            int lastFlag = (i == layers.size()-1)?VK_BUFFER_USAGE_TRANSFER_SRC_BIT:0;
            List<Buffer> nextBuffers = layer
                    .getBufferInfos()
                    .stream()
                    .filter(x -> x.getType() == BINDING_TYPE_OUTPUT)
                    .map(bindingInfo ->
                            new Buffer(
                                    length * bindingInfo.getSize(),
                                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | lastFlag,
                                    0,
                                    VMA_MEMORY_USAGE_GPU_ONLY,
                                    allocator
                            ))
                    .collect(Collectors.toList());
            buffers.addAll(nextBuffers);

            layer.bindBuffers(previousBuffers, nextBuffers, additionalBuffers);
        }
    }

    public void sendBatch(List<ByteBuffer> data){
        Buffer stagingBuffer = new Buffer(
                data.stream().mapToInt(ByteBuffer::remaining).max().orElse(0),
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                0,
                VMA_MEMORY_USAGE_CPU_TO_GPU,
                allocator
        );

        for (int i = 0; i < topOfLineBuffers.size(); i++) {
            ByteBuffer byteBuffer = data.get(i);
            int size = byteBuffer.remaining();
            Buffer.copyBuffer(byteBuffer, stagingBuffer, size);
            Buffer gpuData = topOfLineBuffers.get(i);
            Buffer.copyBuffer(stagingBuffer, gpuData, size, commandPool).block().destroy();
        }

        stagingBuffer.destroy();
    }

    public void freeMemory() {
        buffers.forEach(VulkanObject::destroy);
        buffers.clear();
    }

    public void destroy() {
        layers.forEach(Layer::destroy);
        layers.clear();
    }

}
