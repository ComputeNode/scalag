package com.unihogsoft.scalag.vulkan.executor;

import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.SortPipelines;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.Allocator;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;
import com.unihogsoft.scalag.vulkan.memory.DescriptorSet;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import org.joml.Vector3ic;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_GPU_ONLY;
import static org.lwjgl.vulkan.VK10.*;

public class SortByKeyExecutor {
    private List<Buffer> buffers;
    private VkCommandBuffer commandBuffer;
    private Fence fence;
    private List<DescriptorSet> descriptorSets;

    private final int groupCount;
    private final SortPipelines sortPipelines;

    private final Device device;
    private final Queue queue;
    private final Allocator allocator;
    private final DescriptorPool descriptorPool;
    private final CommandPool commandPool;

    public SortByKeyExecutor(int groupCount, SortPipelines sortPipelines, VulkanContext context) {
        this.groupCount = groupCount;
        this.sortPipelines = sortPipelines;
        this.device = context.getDevice();
        this.allocator = context.getAllocator();
        this.descriptorPool = context.getDescriptorPool();
        this.commandPool = context.getCommandPool();
        this.queue = context.getComputeQueue();
        setup();
    }

    private void setup() {
        try (MemoryStack stack = stackPush()) {
            buffers = new ArrayList<>(4);
            buffers.add(
                    new Buffer(
                            sortPipelines.getDataSize() * groupCount,
                            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                            0,
                            VMA_MEMORY_USAGE_GPU_ONLY,
                            allocator
                    )
            );
            for (int i = 0; i < 2; i++) {
                buffers.add(
                        new Buffer(
                                sortPipelines.getDataSize() * 4,
                                VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                                0,
                                VMA_MEMORY_USAGE_GPU_ONLY,
                                allocator
                        )
                );
            }
            buffers.add(
                    new Buffer(
                            sortPipelines.getDataSize() * groupCount,
                            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            0,
                            VMA_MEMORY_USAGE_GPU_ONLY,
                            allocator
                    )
            );


            descriptorSets = sortPipelines.createDescriptorSets(buffers, descriptorPool, device);

            VkCommandBuffer commandBuffer = commandPool.createCommandBuffer();

            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to begin recording command buffer", err);
            }
            List<ComputePipeline> pipelines = sortPipelines.getPipelines();
            for (int i = 0; i < 3; i++) {
                dispatchPipeline(commandBuffer, pipelines.get(i), descriptorSets.get(i));
            }

            err = vkEndCommandBuffer(commandBuffer);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to finish recording command buffer", err);
            }
            this.commandBuffer = commandBuffer;

            this.fence = new Fence(device);
        }
    }

    private void dispatchPipeline(VkCommandBuffer commandBuffer, ComputePipeline pipeline, DescriptorSet set) {
        try (MemoryStack stack = stackPush()) {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.get());

            LongBuffer pDescriptorSet = stack.callocLong(1).put(0, set.get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.getPipelineLayout(), 0, pDescriptorSet, null);

            Vector3ic workgroup = pipeline.getComputeShader().getWorkgroupDimensions();
            vkCmdDispatch(commandBuffer, groupCount / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());
        }
    }


    public ByteBuffer execute(ByteBuffer input) {
        return BufferUtils.createByteBuffer(0);
    }

    public void destroy() {

    }
}
