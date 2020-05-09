package com.unihogsoft.scalag;

import com.unihogsoft.scalag.Executor;
import com.unihogsoft.scalag.vulkan.VulkanContext;
import com.unihogsoft.scalag.vulkan.compute.Shader;
import com.unihogsoft.scalag.vulkan.compute.ShaderRunner;

import java.nio.ByteBuffer;

/**
 * @author MarconZet
 * Created 15.04.2020
 */
public class MapExecutor implements Executor {
    private ShaderRunner shaderRunner;

    public MapExecutor(int groupCount, Shader shader, VulkanContext context) {
        shaderRunner = new ShaderRunner(groupCount, shader, context);
    }

    @Override
    public ByteBuffer[] execute(ByteBuffer[] input) {
        return shaderRunner.execute(input);
    }

    @Override
    public void destroy() {
        shaderRunner.destroy();
    }
}
