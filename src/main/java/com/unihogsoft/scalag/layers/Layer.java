package com.unihogsoft.scalag.layers;

import com.unihogsoft.scalag.vulkan.memory.Buffer;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.List;

/**
 * @author MarconZet
 * Created 09.05.2020
 */
public interface Layer {
    void create();
    void destroy();
    void record(VkCommandBuffer commandBuffer);
    void getBufferInfo();
    void bindBuffers(List<Buffer> buffers);
}
