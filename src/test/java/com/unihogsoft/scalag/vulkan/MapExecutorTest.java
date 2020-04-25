package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.compute.MapPipeline;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.memory.BindingInfo;
import org.joml.Vector3f;
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

import static com.unihogsoft.scalag.vulkan.memory.BindingInfo.OP_READ_AFTER_OPERATION;
import static com.unihogsoft.scalag.vulkan.memory.BindingInfo.OP_WRITE_BEFORE_EXECUTION;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * @author MarconZet
 * Created 17.04.2020
 */
class MapExecutorTest {

    private static VulkanContext context;

    private final static int bufferLength = 16384;
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
    void execute() {
        ByteBuffer shaderCode;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(Objects.requireNonNull(classLoader.getResource("comp.spv")).getFile());
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            shaderCode = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<BindingInfo> bindingInfos = new ArrayList<>(2);
        bindingInfos.add(new BindingInfo(0, 4));
        bindingInfos.add(new BindingInfo(1, 4));

        Vector3f workgroupDimensions = new Vector3f(128, 1, 1);

        Shader shader = new Shader(shaderCode, workgroupDimensions, bindingInfos, "main", context.getDevice());
        MapPipeline pipeline = new MapPipeline(shader, context);

        Map<Integer, Integer> operations = new HashMap<>();
        operations.put(0, OP_WRITE_BEFORE_EXECUTION);
        operations.put(1, OP_READ_AFTER_OPERATION);

        MapExecutor executor = new MapExecutor(bufferLength, operations, pipeline, context);

        Random rand = new Random(System.currentTimeMillis());
        byte[] randData = new byte[bufferSize];
        rand.nextBytes(randData);
        ByteBuffer input = MemoryUtil.memAlloc(bufferSize);
        input.put(randData).flip();

        ByteBuffer output = executor.execute(input);

        executor.destroy();
        pipeline.destroy();

        byte[] in = new byte[input.remaining()];
        byte[] out = new byte[output.remaining()];
        input.get(in);
        output.get(out);

        memFree(input);
        memFree(output);

        assertArrayEquals(in, out);

    }
}