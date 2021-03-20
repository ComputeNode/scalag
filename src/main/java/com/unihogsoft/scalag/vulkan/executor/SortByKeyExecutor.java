package com.unihogsoft.scalag.vulkan.executor;

import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.command.Fence;
import com.unihogsoft.scalag.vulkan.command.Queue;
import com.unihogsoft.scalag.vulkan.compute.SortPipelines;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.Allocator;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;
import com.unihogsoft.scalag.vulkan.memory.DescriptorSet;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_GPU_ONLY;
import static org.lwjgl.vulkan.VK10.*;

public class SortByKeyExecutor implements Executor {
    private List<Buffer> buffers;
    private VkCommandBuffer commandBuffer;
    private DescriptorSet descriptorSet;
    private Fence fence;

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
        }

        List<DescriptorSet> descriptorSets = sortPipelines.createDescriptorSets(buffers, descriptorPool, device);
    }


    @Override
    public ByteBuffer[] execute(ByteBuffer[] input) {
        return new ByteBuffer[0];
    }

    @Override
    public void destroy() {

    }
}
