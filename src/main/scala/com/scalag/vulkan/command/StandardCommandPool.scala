package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device;

/**
 * @author MarconZet
 * Created 13.04.2020
 * Copied from Wrap
 */

class StandardCommandPool extends CommandPool {
    StandardCommandPool(Device device, Queue queue) {
        super(device, queue);
    }

    override     protected int getFlags() {
        return 0;
    }
}
