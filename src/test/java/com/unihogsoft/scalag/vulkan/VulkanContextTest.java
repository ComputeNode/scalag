package com.unihogsoft.scalag.vulkan;

import junit.textui.TestRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.scalatestplus.junit.JUnitRunner;

/**
 * @author MarconZet
 * Created 17.04.2020
 */

class VulkanContextTest {

    @Test
    public void initializingContextWithNoArguments() {
        VulkanContext context = new VulkanContext();
        context.destroy();
    }

    @Test
    public void initializingContextWithValidationLayers(){
        VulkanContext context = new VulkanContext(true);
        context.destroy();
    }

}