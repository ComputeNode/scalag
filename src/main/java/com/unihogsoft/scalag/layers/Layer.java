package com.unihogsoft.scalag.layers;

import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.compute.BindingInfo;
import com.unihogsoft.scalag.vulkan.memory.Buffer;
import com.unihogsoft.scalag.vulkan.memory.DescriptorPool;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.List;

/**
 * @author MarconZet
 * Created 09.05.2020
 */
public interface Layer {
    void create(Device device, DescriptorPool descriptorPool);
    void destroy();
    void record(VkCommandBuffer commandBuffer, Buffer workgroup);
    List<BindingInfo> getBufferInfos();
    void bindBuffers(List<Buffer> inputData, List<Buffer> outputData, List<Buffer> additionalData);
}
