package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.executor.MapExecutor;
import com.unihogsoft.scalag.vulkan.compute.LayoutInfo;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3i;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
class SortByKeyExecutorTest {
    private static VulkanContext context;

    @BeforeAll
    static void setUp() {
        context = new VulkanContext(true);
    }

    @AfterAll
    static void tearDown() {
        context.destroy();
    }

    @Test
    @Ignore
    void sortShaderTest() {
//        int n = 2*64;
//
//        Shader shader = new Shader(
//                loadShader("sort.spv", VulkanContext.class.getClassLoader()),
//                new Vector3i(64, 1, 1),
//                Arrays.asList(
//                        new LayoutInfo(0, 0, 4),
//                        new LayoutInfo(0, 1, 4)
//                ),
//                "main",
//                context.getDevice()
//        );
//
//        ComputePipeline pipeline = new ComputePipeline(shader, context);
//
//        MapExecutor executor = new MapExecutor(n, pipeline, context);
//
//        Random rand = new Random(System.currentTimeMillis());
//        int[] values = IntStream.generate(() -> rand.nextInt(1000)).limit(n).toArray();
//
//        ByteBuffer inputBuffer = BufferUtils.createByteBuffer(n * 4);
//        inputBuffer.asIntBuffer().put(values);
//        ByteBuffer[] input = {inputBuffer};
//
//        List<ByteBuffer> output = executor.execute(input);
//
//        executor.destroy();
//        pipeline.destroy();
//        shader.destroy();
//
//        int[] result = new int[values.length];
//        output.get(0).asIntBuffer().get(result);
//
//
//        result = Arrays.stream(result).map(x -> values[x]).toArray();
//
//        Arrays.sort(values);
//
//        assertArrayEquals(values, result);
    }

    private ByteBuffer loadShader(String path) {
        return loadShader(path, getClass().getClassLoader());
    }

    private ByteBuffer loadShader(String path, ClassLoader classLoader) {
        try {
            File file = new File(Objects.requireNonNull(classLoader.getResource(path)).getFile());
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            return fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}