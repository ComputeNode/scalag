package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.executor.BufferAction;
import com.unihogsoft.scalag.vulkan.executor.MapExecutor;
import com.unihogsoft.scalag.vulkan.compute.LayoutInfo;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * @author MarconZet
 * Created 17.04.2020
 */
class MapExecutorTest {

    private static VulkanContext context;

    private final static int bufferLength = 1024;
    private final static int bufferSize = 4 * bufferLength;

    @BeforeAll
    static void setUp() {
        context = new VulkanContext(true);
    }

    @AfterAll
    static void tearDown() {
        context.destroy();
    }

    @Test
    void copyRandomDataFromOneBufferToAnother() {
        ByteBuffer shaderCode;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(Objects.requireNonNull(classLoader.getResource("copy.spv")).getFile());
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            shaderCode = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Shader shader = new Shader(
                shaderCode,
                new Vector3i(128, 1, 1),
                Arrays.asList(
                        new LayoutInfo(0, 0, 4),
                        new LayoutInfo(0, 1, 4)
                ),
                "main",
                context.getDevice()
        );


        ComputePipeline pipeline = new ComputePipeline(shader, context);

        MapExecutor executor = new MapExecutor(
                bufferLength,
                Arrays.asList(
                        new BufferAction(BufferAction.LOAD_INTO),
                        new BufferAction(BufferAction.LOAD_FROM)
                ),
                pipeline,
                context
        );

        Random rand = new Random(System.currentTimeMillis());
        byte[] randData = new byte[bufferSize];
        rand.nextBytes(randData);
        ByteBuffer inputBuffer = BufferUtils.createByteBuffer(bufferSize);
        inputBuffer.put(randData).flip();
        var input = Collections.singletonList(inputBuffer);

        List<ByteBuffer> output = executor.execute(input);

        executor.destroy();
        pipeline.destroy();
        shader.destroy();

        byte[] in = new byte[input.get(0).remaining()];
        byte[] out = new byte[output.get(0).remaining()];
        input.get(0).get(in);
        output.get(0).get(out);

        assertArrayEquals(in, out);
    }

    private static final int n = 2;

    @Test
    void copyRandomDataBetweenFourBuffers() {
        ByteBuffer shaderCode;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(Objects.requireNonNull(classLoader.getResource("two_copy.spv")).getFile());
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            shaderCode = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Shader shader = new Shader(
                shaderCode,
                new Vector3i(128, 1, 1),
                Arrays.asList(
                        new LayoutInfo(0, 0, 4),
                        new LayoutInfo(0, 1, 4),
                        new LayoutInfo(0, 2, 4),
                        new LayoutInfo(0, 3, 4)
                ),
                "main",
                context.getDevice()
        );

        ComputePipeline pipeline = new ComputePipeline(shader, context);

        MapExecutor executor = new MapExecutor(
                bufferLength,
                Arrays.asList(
                        new BufferAction(BufferAction.LOAD_INTO),
                        new BufferAction(BufferAction.LOAD_INTO),
                        new BufferAction(BufferAction.LOAD_FROM),
                        new BufferAction(BufferAction.LOAD_FROM)
                ),
                pipeline,
                context
        );

        Random rand = new Random(System.currentTimeMillis());
        byte[][] randData = new byte[n][bufferSize];
        ByteBuffer[] inputArray = new ByteBuffer[n];
        for (int i = 0; i < n; i++) {
            rand.nextBytes(randData[i]);
            inputArray[i] = MemoryUtil.memAlloc(bufferSize);
            inputArray[i].put(randData[i]).flip();
        }

        List<ByteBuffer> input = Arrays.asList(inputArray.clone());

        List<ByteBuffer> output = executor.execute(input);

        executor.destroy();
        pipeline.destroy();
        shader.destroy();

        for (int i = 0; i < 2; i++) {
            byte[] in = new byte[input.get(i).remaining()];
            byte[] out = new byte[output.get(i).remaining()];
            input.get(i).get(in);
            output.get(i).get(out);
            assertArrayEquals(in, out);
        }
    }
}