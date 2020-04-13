package com.unihogsoft.scalag.vulkan.command;


import com.unihogsoft.scalag.vulkan.core.Device;

import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;

/**
 * @author MarconZet
 * Created 13.04.2020
 * Copied from Wrap
 */
public class OneTimeCommandPool extends CommandPool {
    public OneTimeCommandPool(Device device, Queue queue) {
        super(device, queue);
    }

    @Override
    protected int getFlags() {
        return VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
    }
}
