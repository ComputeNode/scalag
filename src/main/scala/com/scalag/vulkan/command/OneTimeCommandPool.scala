package com.scalag.vulkan.command;


import com.scalag.vulkan.core.Device;

import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;

/**
 * @author MarconZet
 * Created 13.04.2020
 * Copied from Wrap
 */
class OneTimeCommandPool extends CommandPool {
    OneTimeCommandPool(Device device, Queue queue) {
        super(device, queue);
    }

    override     protected int getFlags() {
        return VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
    }
}
