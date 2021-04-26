package com.unihogsoft.scalag.vulkan.executor;

import com.unihogsoft.scalag.dsl.Array;
import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.LayoutInfo;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorSet;
import com.unihogsoft.scalag.vulkan.utility.VulkanObjectHandle;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.LongBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_GPU_ONLY;
import static org.lwjgl.vulkan.VK10.*;

public class SortByKeyExecutor extends AbstractExecutor {
    private final int sortPasses;

    private final Shader keyShader;

    private final ComputePipeline keyPipeline;
    private final ComputePipeline preparePipeline;
    private final ComputePipeline sortPipeline;
    private final ComputePipeline copyPipeline;

    public SortByKeyExecutor(int dataLength, ComputePipeline keyPipeline, VulkanContext context) {
        super(dataLength, createBufferActions(), context);
        this.keyPipeline = keyPipeline;
        this.keyShader = keyPipeline.getComputeShader();
        this.preparePipeline = createPreparePipeline(context);
        this.copyPipeline = createCopyPipeline(context);
        this.sortPipeline = createSortPipeline(context);
        this.sortPasses = (int) Math.pow(Math.log(dataLength), 2);
        setup();
    }

    @Override
    protected void setupBuffers() {
        List<LayoutInfo> layoutInfos = keyShader.getLayoutInfos();

        int[] sizes = {layoutInfos.get(0).getSize(), layoutInfos.get(0).getSize(), 4, 4, 4};
        for (int i = 0; i < sizes.length; i++) {
            buffers.add(
                    new Buffer(
                            sizes[i] * dataLength,
                            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | bufferActions.get(i).getAction(),
                            0,
                            VMA_MEMORY_USAGE_GPU_ONLY,
                            allocator
                    )
            );
        }

        for (int i = 0; i < 3; i++) {
            buffers.add(
                    new Buffer(
                            8,
                            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                            0,
                            VMA_MEMORY_USAGE_GPU_ONLY,
                            allocator
                    )
            );
        }

        Buffer in = buffers.get(0);
        Buffer out = buffers.get(1);
        Buffer keys = buffers.get(2);
        Buffer order1 = buffers.get(3);
        Buffer order2 = buffers.get(4);
        Buffer data1 = buffers.get(5);
        Buffer data2 = buffers.get(6);
        Buffer size = buffers.get(7);

        Buffer outOrder = (sortPasses % 2 == 0) ? order1 : order2;

        List<DescriptorSet> descriptorSets = Arrays.asList(
                createUpdatedDescriptorSet(keyPipeline.getDescriptorSetLayouts()[0], Arrays.asList(in, keys)),
                createUpdatedDescriptorSet(preparePipeline.getDescriptorSetLayouts()[0], Arrays.asList(order1, data1)),
                createUpdatedDescriptorSet(sortPipeline.getDescriptorSetLayouts()[0], Arrays.asList(keys, order1, order2, data1, data2)),
                createUpdatedDescriptorSet(sortPipeline.getDescriptorSetLayouts()[0], Arrays.asList(keys, order2, order1, data2, data1)),
                createUpdatedDescriptorSet(copyPipeline.getDescriptorSetLayouts()[0], Arrays.asList(in, out, outOrder, size))
        );
        this.descriptorSets.addAll(descriptorSets);
    }

    @Override
    protected void recordCommandBuffer(VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, keyPipeline.get());

            LongBuffer pDescriptorSets = stack.longs(descriptorSets.get(0).get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, keyPipeline.getPipelineLayout(), 0, pDescriptorSets, null);

            Vector3ic workgroup = keyShader.getWorkgroupDimensions();
            vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());


            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, preparePipeline.get());

            pDescriptorSets = stack.longs(descriptorSets.get(5).get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, preparePipeline.getPipelineLayout(), 0, pDescriptorSets, null);

            workgroup = preparePipeline.getComputeShader().getWorkgroupDimensions();
            vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());


            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, sortPipeline.get());

            int oddIteration = 1;
            for (int k = 2; k <= dataLength; k = 2 * k) {
                oddIteration = 1 - oddIteration;
                pDescriptorSets = stack.longs(descriptorSets.get(1).get(), descriptorSets.get(3 + oddIteration).get());
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, sortPipeline.getPipelineLayout(), 0, pDescriptorSets, null);

                workgroup = sortPipeline.getComputeShader().getWorkgroupDimensions();
                vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());
            }


            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, copyPipeline.get());

            pDescriptorSets = stack.longs(descriptorSets.get(2).get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, copyPipeline.getPipelineLayout(), 0, pDescriptorSets, null);

            workgroup = copyPipeline.getComputeShader().getWorkgroupDimensions();
            vkCmdDispatch(commandBuffer, (dataLength * getBiggestTransportData()) / (4 * workgroup.x()), 1 / workgroup.y(), 1 / workgroup.z());
        }
    }

    @Override
    protected int getBiggestTransportData() {
        return keyShader.getLayoutInfos().get(0).getSize();
    }

    private static ComputePipeline createCopyPipeline(VulkanContext context) {
        Shader shader = new Shader(
                Shader.loadShader("copy.spv"),
                new Vector3i(1024, 1, 1),
                Arrays.asList(
                        new LayoutInfo(0, 0, 4),
                        new LayoutInfo(0, 1, 4)
                ),
                "main",
                context.getDevice()
        );

        return new ComputePipeline(shader, context);
    }

    private static ComputePipeline createSortPipeline(VulkanContext context) {
        Shader shader = new Shader(
                Shader.loadShader("sort.spv"),
                new Vector3i(1024, 1, 1),
                Arrays.asList(
                        new LayoutInfo(0, 0, 4),
                        new LayoutInfo(0, 1, 4),
                        new LayoutInfo(1, 0, 4),
                        new LayoutInfo(1, 1, 4)
                ),
                "main",
                context.getDevice()
        );

        return new ComputePipeline(shader, context);
    }

    private static ComputePipeline createPreparePipeline(VulkanContext context) {
        Shader shader = new Shader(
                Shader.loadShader("prepare.spv"),
                new Vector3i(1024, 1, 1),
                Arrays.asList(
                        new LayoutInfo(0, 0, 4),
                        new LayoutInfo(0, 1, 4)
                ),
                "main",
                context.getDevice()
        );

        return new ComputePipeline(shader, context);
    }

    private static List<BufferAction> createBufferActions() {
        int[] actions = {VK_BUFFER_USAGE_TRANSFER_DST_BIT, 0, 0, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, 0, 0};
        return IntStream.of(actions).mapToObj(BufferAction::new).collect(Collectors.toList());
    }

    @Override
    public void destroy() {
        Stream.of(preparePipeline, copyPipeline, sortPipeline).forEach(pipeline -> {
            pipeline.getComputeShader().destroy();
            pipeline.destroy();
        });

        super.destroy();
    }
}
