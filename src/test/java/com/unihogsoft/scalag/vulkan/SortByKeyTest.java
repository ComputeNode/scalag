package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.LayoutInfo;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.executor.BufferAction;
import com.unihogsoft.scalag.vulkan.executor.MapExecutor;
import com.unihogsoft.scalag.vulkan.executor.SortByKeyExecutor;
import org.joml.Vector3i;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class SortByKeyTest {
    private VulkanContext context;

    private final static int bufferLength = 1024;
    private final static int bufferSize = 4 * bufferLength;

    @BeforeEach
    void setUp() {
        context = new VulkanContext(true);
    }

    @AfterEach
    void tearDown() {
        context.destroy();
    }

    @Test
    void sortBySimpleKey() {
        Shader shader = new Shader(
                Shader.loadShader("simple_key.spv", this.getClass().getClassLoader()),
                new Vector3i(128, 1, 1),
                Arrays.asList(
                        new LayoutInfo(0, 0, 4),
                        new LayoutInfo(0, 1, 4)
                ),
                "main",
                context.getDevice()
        );


        ComputePipeline pipeline = new ComputePipeline(shader, context);

        SortByKeyExecutor executor = new SortByKeyExecutor(
                bufferLength,
                pipeline,
                context
        );
        float[] testData = new float[bufferLength];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (float)testData.length - i;
        }

        ByteBuffer inputBuffer = BufferUtils.createByteBuffer(bufferSize);
        inputBuffer.asFloatBuffer().put(testData).flip();
        var input = Collections.singletonList(inputBuffer);

        ByteBuffer output = executor.execute(input).get(0);

        executor.destroy();
        pipeline.destroy();
        shader.destroy();

        float[] outData = new float[bufferLength];
        output.asFloatBuffer().get(outData);

        Arrays.sort(testData);

        assertArrayEquals(testData, outData);
    }

    static class FloatDummy implements Comparable<FloatDummy> {
        float value;

        private float key() {
            return value;
        }

        @Override
        public int compareTo(FloatDummy floatDummy) {
            return (int) (key() - floatDummy.key());
        }
    }
}
