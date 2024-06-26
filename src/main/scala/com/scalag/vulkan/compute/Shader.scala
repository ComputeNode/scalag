package com.scalag.vulkan.compute;

import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.utility.VulkanAssertionError;
import com.scalag.vulkan.utility.VulkanObjectHandle;
import lombok.Getter;
import org.joml.Vector3ic;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 25.04.2020
 */
class Shader extends VulkanObjectHandle {
    private final ByteBuffer shaderCode;

    @Getter
    private final Vector3ic workgroupDimensions;
    @Getter
    private final List<LayoutInfo> layoutInfos;
    @Getter
    private final String functionName;

    private final Device device;

    Shader(ByteBuffer shaderCode, Vector3ic workgroupDimensions, List<LayoutInfo> layoutInfos, String functionName, Device device) {
        this.shaderCode = shaderCode;
        this.workgroupDimensions = workgroupDimensions;
        this.layoutInfos = layoutInfos;
        this.functionName = functionName;
        this.device = device;
        create();
    }

    List<LayoutInfo> getLayoutsBySets(int a) {
        return layoutInfos.stream().filter(x -> x.getSet() == a).collect(Collectors.toList());
    }

    List<List<LayoutInfo>> getLayoutsBySets(){
        return layoutInfos.stream().mapToInt(LayoutInfo::getSet).distinct().sorted().mapToObj(this::getLayoutsBySets).collect(Collectors.toList());
    }

    override     protected void init() {
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

    override     protected void close() {
        vkDestroyShaderModule(device.get(), handle, null);
    }


    static ByteBuffer loadShader(String path) {
        return loadShader(path, Shader.class.getClassLoader());
    }

    static ByteBuffer loadShader(String path, ClassLoader classLoader) {
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
