package com.unihogsoft.scalag.vulkan.executor;

import com.unihogsoft.scalag.vulkan.VulkanContext;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

class SortByKeyExecutorTest {
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
    @Ignore
    void simpleIntSort() {
        ByteBuffer shaderCode = loadShader("");
    }

    private ByteBuffer loadShader(String path) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(Objects.requireNonNull(classLoader.getResource(path)).getFile());
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            return fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}