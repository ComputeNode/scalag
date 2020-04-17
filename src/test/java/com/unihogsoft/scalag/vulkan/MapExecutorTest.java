package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.command.CommandPool;
import com.unihogsoft.scalag.vulkan.compute.MapPipeline;
import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.Allocator;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author MarconZet
 * Created 17.04.2020
 */
class MapExecutorTest {

    private static VulkanContext context;

    private final static int bufferLength = 16384;
    private final static int bufferSize = 4 * bufferLength;

    private static int[] shader = {
            119734787, 65536, 0, 23, 0, 131089, 1, 196622, 0, 0, 262159, 5, 1, 102, 393232, 1, 17, 1, 1, 1, 196679, 9,
            3, 262215, 4, 11, 28, 262215, 2, 34, 0, 262215, 2, 33, 0, 262215, 3, 34, 0, 262215, 3, 33, 1, 262215, 8, 6,
            4, 327752, 9, 0, 35, 0, 131091, 5, 196641, 6, 5, 262165, 7, 32, 1, 262187, 7, 16, bufferLength, 262172, 8, 7, 16,
            196638, 9, 8, 262176, 10, 2, 9, 262176, 11, 2, 7, 262167, 12, 7, 3, 262176, 13, 1, 12, 262176, 14, 1, 7,
            262187, 7, 15, 0, 262203, 10, 2, 2, 262203, 10, 3, 2, 262203, 13, 4, 1, 327734, 5, 1, 0, 6, 131320, 17,
            327745, 14, 21, 4, 15, 262205, 7, 20, 21, 393281, 11, 18, 2, 15, 20, 262205, 7, 22, 18, 393281, 11, 19,
            3, 15, 20, 196670, 19, 22, 65789, 65592
    };

    @BeforeAll
    static void setUp() {
        context = new VulkanContext(true);
    }

    @AfterAll
    static void tearDown() {
        context.close();
    }

    @Test
    void execute() {
        Random rand = new Random(System.currentTimeMillis());

        ByteBuffer input = BufferUtils.createByteBuffer(bufferSize);
        IntBuffer data = input.asIntBuffer();
        while(data.hasRemaining()){
            data.put(rand.nextInt());
        }
        data.flip();

        MapPipeline pipeline = new MapPipeline(shader, context);
        MapExecutor executor = new MapExecutor(bufferSize, bufferSize, bufferLength, pipeline, context);

        ByteBuffer result = executor.execute(input);

        IntBuffer test = result.asIntBuffer();

        while(test.hasRemaining()){
            assertEquals(data.get(), test.get());
        }
    }
}