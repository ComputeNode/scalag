package com.unihogsoft.scalag.vulkan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author MarconZet
 * Created 17.04.2020
 */
class VulkanContextTest {

    @Test
    public void initialisingContextWithNoArguments() {
        VulkanContext context = new VulkanContext();
        context.close();
    }

    @Test
    public void initialingContextWithValidationLayers(){
        VulkanContext context = new VulkanContext(true);
        context.close();
    }

}