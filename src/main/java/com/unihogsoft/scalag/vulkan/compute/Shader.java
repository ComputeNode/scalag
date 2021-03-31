package com.unihogsoft.scalag.vulkan.compute;

import com.unihogsoft.scalag.vulkan.core.Device;
import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObjectHandle;
import lombok.Getter;
import org.joml.Vector3ic;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 25.04.2020
 */
public class Shader extends VulkanObjectHandle {
    private final ByteBuffer shaderCode;

    @Getter
    private final Vector3ic workgroupDimensions;
    @Getter
    private final List<LayoutInfo> layoutInfos;
    @Getter
    private final String functionName;

    private final Device device;

    public Shader(ByteBuffer shaderCode, Vector3ic workgroupDimensions, List<LayoutInfo> layoutInfos, String functionName, Device device) {
        this.shaderCode = shaderCode;
        this.workgroupDimensions = workgroupDimensions;
        this.layoutInfos = layoutInfos;
        this.functionName = functionName;
        this.device = device;
        create();
    }

    public List<LayoutInfo> getLayoutsBySets(int a) {
        return layoutInfos.stream().filter(x -> x.getSet() == a).collect(Collectors.toList());
    }

    public List<List<LayoutInfo>> getLayoutsBySets(){
        return layoutInfos.stream().mapToInt(LayoutInfo::getSet).distinct().sorted().mapToObj(this::getLayoutsBySets).collect(Collectors.toList());
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
}
