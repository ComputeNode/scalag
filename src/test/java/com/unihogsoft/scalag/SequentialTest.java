package com.unihogsoft.scalag;

import com.unihogsoft.scalag.layers.StreamMapLayer;
import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.compute.BindingInfo;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.unihogsoft.scalag.vulkan.compute.BindingInfo.BINDING_TYPE_INPUT;
import static com.unihogsoft.scalag.vulkan.compute.BindingInfo.BINDING_TYPE_OUTPUT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * @author MarconZet
 * Created 12.05.2020
 */
class SequentialTest {
    private VulkanContext context;
    private final static int bufferLength = 1024;
    private final static int bufferSize = 4 * bufferLength;

    @BeforeEach
    public void before() {
        context = new VulkanContext(true);
    }

    @AfterEach
    public void after() {context.destroy();}

    @Test
    public void copyInputToOutput(){
        Sequential sequential = new Sequential(context);

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

        StreamMapLayer map = new StreamMapLayer(shader);

        sequential.addLayer(map);
        sequential.compile();
        sequential.allocateMemory(null, bufferLength);

        Random rand = new Random(System.currentTimeMillis());
        byte[] randData = new byte[bufferSize];
        rand.nextBytes(randData);
        ByteBuffer inputBuffer = MemoryUtil.memAlloc(bufferSize);
        inputBuffer.put(randData).flip();
        List<ByteBuffer> input = new ArrayList<>(1);
        input.set(0,inputBuffer);

        sequential.sendBatch(input);
        sequential.execute();
        List<ByteBuffer> output = sequential.retrieveResults();

        byte[] in = new byte[input.get(0).remaining()];
        byte[] out = new byte[output.get(0).remaining()];
        input.get(0).get(in);
        output.get(0).get(out);

        memFree(input.get(0));
        memFree(output.get(0));

        assertArrayEquals(in, out);

    }

}