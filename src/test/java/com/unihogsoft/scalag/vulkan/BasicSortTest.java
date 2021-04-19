package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.executor.BufferAction;
import com.unihogsoft.scalag.vulkan.executor.MapExecutor;
import com.unihogsoft.scalag.vulkan.compute.LayoutInfo;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3i;
import org.junit.Ignore;
import org.junit.jupiter.api.*;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


@Slf4j
class BasicSortTest {
    private VulkanContext context;

    @BeforeEach
    void setUp() {
        context = new VulkanContext(true);
    }

    @AfterEach
    void tearDown() {
        context.destroy();
    }

    @Test
    void sortOnGpuSmallSample() {
        int n = 64;

        Shader shader = new Shader(
                Shader.loadShader("sort.spv", this.getClass().getClassLoader()),
                new Vector3i(n, 1, 1),
                Arrays.asList(
                        new LayoutInfo(0, 0, 4),
                        new LayoutInfo(0, 1, 4)
                ),
                "main",
                context.getDevice()
        );

        ComputePipeline pipeline = new ComputePipeline(shader, context);

        MapExecutor executor = new MapExecutor(
                n,
                Arrays.asList(
                        new BufferAction(BufferAction.LOAD_INTO),
                        new BufferAction(BufferAction.LOAD_FROM)
                ),
                pipeline,
                context
        );

        Random rand = new Random(System.currentTimeMillis());
        int[] values = IntStream.generate(() -> rand.nextInt(1000)).limit(n).toArray();

        ByteBuffer inputBuffer = BufferUtils.createByteBuffer(n * 4);
        inputBuffer.asIntBuffer().put(values);
        List<ByteBuffer> input = Collections.singletonList(inputBuffer);

        List<ByteBuffer> output = executor.execute(input);

        executor.destroy();
        pipeline.destroy();
        shader.destroy();

        int[] result = new int[values.length];
        output.get(0).asIntBuffer().get(result);


        result = Arrays.stream(result).map(x -> values[x]).toArray();

        Arrays.sort(values);

        log.info(Arrays.toString(result));
        assertArrayEquals(values, result);
    }
}