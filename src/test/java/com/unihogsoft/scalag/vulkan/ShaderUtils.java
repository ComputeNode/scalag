package com.unihogsoft.scalag.vulkan;

import com.unihogsoft.scalag.vulkan.compute.Shader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class ShaderUtils {
    public static ByteBuffer loadShader(String path) {
        return loadShader(path, ShaderUtils.class.getClassLoader());
    }

    public static ByteBuffer loadShader(String path, ClassLoader classLoader) {
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
