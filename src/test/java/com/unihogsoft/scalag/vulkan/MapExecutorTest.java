package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.compute.ComputePipeline;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.executor.MapExecutor;
import com.unihogsoft.scalag.vulkan.memory.BindingInfo;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import static com.unihogsoft.scalag.vulkan.memory.BindingInfo.*;
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

        List<BindingInfo> bindingInfos = new ArrayList<>(2);
        bindingInfos.add(new BindingInfo(0, 4, BINDING_TYPE_INPUT));
        bindingInfos.add(new BindingInfo(1, 4, BINDING_TYPE_OUTPUT));

        Vector3ic workgroupDimensions = new Vector3i(128, 1, 1);

        Shader shader = new Shader(shaderCode, workgroupDimensions, bindingInfos, "main", context.getDevice());
        ComputePipeline pipeline = new ComputePipeline(shader, context);

        MapExecutor executor = new MapExecutor(bufferLength, pipeline, context);

        Random rand = new Random(System.currentTimeMillis());
        byte[] randData = new byte[bufferSize];
        rand.nextBytes(randData);
        ByteBuffer inputBuffer = MemoryUtil.memAlloc(bufferSize);
        inputBuffer.put(randData).flip();
        ByteBuffer[] input = new ByteBuffer[1];
        input[0] = inputBuffer;

        ByteBuffer[] output = executor.execute(input);

        executor.destroy();
        pipeline.destroy();
        shader.destroy();

        byte[] in = new byte[input[0].remaining()];
        byte[] out = new byte[output[0].remaining()];
        input[0].get(in);
        output[0].get(out);

        memFree(input[0]);
        memFree(output[0]);

        assertArrayEquals(in, out);
    }

    private static final int n = 2;

    @Test
    void copyRandomDataBetweenFourBuffers(){
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

        List<BindingInfo> bindingInfos = new ArrayList<>(4);
        bindingInfos.add(new BindingInfo(0, 4, BINDING_TYPE_INPUT));
        bindingInfos.add(new BindingInfo(1, 4, BINDING_TYPE_INPUT));
        bindingInfos.add(new BindingInfo(2, 4, BINDING_TYPE_OUTPUT));
        bindingInfos.add(new BindingInfo(3, 4, BINDING_TYPE_OUTPUT));

        Vector3ic workgroupDimensions = new Vector3i(128, 1, 1);

        Shader shader = new Shader(shaderCode, workgroupDimensions, bindingInfos, "main", context.getDevice());
        ComputePipeline pipeline = new ComputePipeline(shader, context);

        MapExecutor executor = new MapExecutor(bufferLength, pipeline, context);

        Random rand = new Random(System.currentTimeMillis());
        byte[][] randData = new byte[n][bufferSize];
        ByteBuffer[] input = new ByteBuffer[n];
        for(int i = 0; i<n; i++){
            rand.nextBytes(randData[i]);
            input[i] = MemoryUtil.memAlloc(bufferSize);
            input[i].put(randData[i]).flip();
        }

        ByteBuffer[] output = executor.execute(input);

        executor.destroy();
        pipeline.destroy();
        shader.destroy();

        byte[][] out = new byte[n][bufferSize];
        for(int i = 0; i<n; i++){
            output[i].get(out[i]);
        }

        for(int i = 0; i<n; i++){
            memFree(input[i]);
            memFree(output[i]);
        }

        for(int i = 0; i<n; i++){
            assertArrayEquals(randData[i], out[i]);
        }
    }
}