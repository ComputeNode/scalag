package com.unihogsoft.scalag.vulkan.compute;

import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.memory.BindingInfo;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObjectHandle;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 25.04.2020
 */
public class Shader extends VulkanObjectHandle {
    private final ByteBuffer shaderCode;
    private final Vector3fc workgroupDimensions;
    private final List<BindingInfo> inputSizes;
    private final String functionName;

    private final Device device;

    public Shader(ByteBuffer shaderCode, Vector3fc workgroupDimensions, List<BindingInfo> inputSizes, String functionName, Device device) {
        this.shaderCode = shaderCode;
        this.workgroupDimensions = workgroupDimensions;
        this.inputSizes = inputSizes;
        this.functionName = functionName;
        this.device = device;
        create();
    }

    @Override
    protected void init() {
        try (MemoryStack stack = stackPush()) {
            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .pCode(shaderCode);

            LongBuffer pShaderModule = stack.callocLong(1);
            int err = vkCreateShaderModule(device.get(), shaderModuleCreateInfo, null, pShaderModule);
            if (err != VK_SUCCESS) {
                throw new VulkanAssertionError("Failed to create shader module", err);
            }
            handle = pShaderModule.get();
        }
    }

    @Override
    protected void close() {
        vkDestroyShaderModule(device.get(), handle, null);
    }

    public Vector3fc getWorkgroupDimensions() {
        return workgroupDimensions;
    }

    public List<BindingInfo> getInputSizes() {
        return inputSizes;
    }

    public String getFunctionName() {
        return functionName;
    }
}
