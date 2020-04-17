package com.unihogsoft.scalag.vulkan;

import org.junit.jupiter.api.Test;

/**
 * @author MarconZet
 * Created 17.04.2020
 */
class VulkanContextTest {

    @Test
    public void initialisingContextWithNoArguments() {
        VulkanContext context = new VulkanContext();
        context.destroy();
    }

    @Test
    public void initialingContextWithValidationLayers(){
        VulkanContext context = new VulkanContext(true);
        context.destroy();
    }

}