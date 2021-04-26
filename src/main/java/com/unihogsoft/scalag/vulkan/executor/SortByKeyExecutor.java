package com.unihogsoft.scalag.vulkan.executor;

import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.LayoutInfo;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorSet;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_CPU_TO_GPU;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_GPU_ONLY;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_DEPENDENCY_DEVICE_GROUP_BIT;

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
        this.sortPasses = getNumberOfPasses(dataLength);
        setup();
    }

    private static int getNumberOfPasses(int dataLength) {
        int remaining = dataLength;
        int d = 0;
        while (remaining > 1) {
            if (remaining % 2 != 0) {
                throw new IllegalArgumentException("Number of data must be power of 2");
            }
            remaining /= 2;
            d++;
        }
        return (d * d + d) / 2;
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

        for (int i = 0; i < 2; i++) {
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

        buffers.add(
                new Buffer(
                        4,
                        VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                        0,
                        VMA_MEMORY_USAGE_CPU_TO_GPU,
                        allocator
                )
        );

        Buffer in = buffers.get(0),
                out = buffers.get(1),
                keys = buffers.get(2),
                order1 = buffers.get(3),
                order2 = buffers.get(4),
                data1 = buffers.get(5),
                data2 = buffers.get(6),
                size = buffers.get(7);


        try (MemoryStack stack = stackPush()) {
            var sizeData = stack.calloc(4 * 2);
            sizeData.asIntBuffer().put(this.dataLength).put(this.dataLength);
            Buffer.copyBuffer(sizeData, size, sizeData.remaining());
        }

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

    private static List<BufferAction> createBufferActions() {
        int[] actions = {VK_BUFFER_USAGE_TRANSFER_DST_BIT, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, 0, 0, 0, 0, 0, 0};
        return IntStream.of(actions).mapToObj(BufferAction::new).collect(Collectors.toList());
    }

    @Override
    protected void recordCommandBuffer(VkCommandBuffer commandBuffer) {
        DescriptorSet keySet = descriptorSets.get(0),
                prepSet = descriptorSets.get(1),
                sort1Set = descriptorSets.get(2),
                sort2Set = descriptorSets.get(3),
                copySet = descriptorSets.get(4);

        Buffer inBuffer = buffers.get(0),
                outBuffer = buffers.get(1),
                keysBuffer = buffers.get(2),
                order1Buffer = buffers.get(3),
                order2Buffer = buffers.get(4),
                data1Buffer = buffers.get(5),
                data2Buffer = buffers.get(6),
                sizeBuffer = buffers.get(7);

        try (MemoryStack stack = stackPush()) {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, keyPipeline.get());

            LongBuffer pDescriptorSets = stack.longs(keySet.get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, keyPipeline.getPipelineLayout(), 0, pDescriptorSets, null);

            Vector3ic workgroup = keyShader.getWorkgroupDimensions();
            vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());


            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, preparePipeline.get());

            pDescriptorSets = stack.longs(prepSet.get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, preparePipeline.getPipelineLayout(), 0, pDescriptorSets, null);

            workgroup = preparePipeline.getComputeShader().getWorkgroupDimensions();
            vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());

            var bufferMemoryBarriers = getMemoryBarriers(Arrays.asList(keysBuffer, order1Buffer, data1Buffer));
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, 0, null, bufferMemoryBarriers, null);


            var buffersSet1 = Arrays.asList(order1Buffer, data1Buffer);
            var buffersSet2 = Arrays.asList(order2Buffer, data2Buffer);

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, sortPipeline.get());

            for (int i = 0; i < sortPasses; i++) {
                pDescriptorSets = stack.longs((i % 2 == 0) ? sort1Set.get() : sort2Set.get());
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, sortPipeline.getPipelineLayout(), 0, pDescriptorSets, null);

                workgroup = sortPipeline.getComputeShader().getWorkgroupDimensions();
                vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());

                bufferMemoryBarriers = getMemoryBarriers((i % 2 != 0) ? buffersSet1 : buffersSet2);
                vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, 0, null, bufferMemoryBarriers, null);
            }


            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, copyPipeline.get());

            pDescriptorSets = stack.longs(copySet.get());
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, copyPipeline.getPipelineLayout(), 0, pDescriptorSets, null);

            workgroup = copyPipeline.getComputeShader().getWorkgroupDimensions();
            vkCmdDispatch(commandBuffer, dataLength / workgroup.x(), 1 / workgroup.y(), 1 / workgroup.z());
        }
    }


    private VkBufferMemoryBarrier.Buffer getMemoryBarriers(List<Buffer> buffers) {
        var bufferMemoryBarriers = VkBufferMemoryBarrier.callocStack(buffers.size());

        for (int i = 0; i < buffers.size(); i++) {
            bufferMemoryBarriers.get(i)
                    .pNext(0)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                    .srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .buffer(buffers.get(i).get())
                    .offset(0)
                    .size(VK_WHOLE_SIZE);
        }
        return bufferMemoryBarriers;
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
                        new LayoutInfo(0, 1, 4),
                        new LayoutInfo(0, 2, 4),
                        new LayoutInfo(0, 3, 4)
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
                        new LayoutInfo(0, 2, 4),
                        new LayoutInfo(0, 3, 4),
                        new LayoutInfo(0, 4, 4)
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

    @Override
    public void destroy() {
        Stream.of(preparePipeline, copyPipeline, sortPipeline).forEach(pipeline -> {
            pipeline.getComputeShader().destroy();
            pipeline.destroy();
        });

        super.destroy();
    }
}
