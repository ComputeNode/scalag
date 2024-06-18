package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device;
import com.scalag.vulkan.core.Device;

/**
 * @author MarconZet
 * Created 13.04.2020
 * Copied from Wrap
 */

public class StandardCommandPool extends CommandPool {
    public StandardCommandPool(Device device, Queue queue) {
        super(device, queue);
    }

    @Override
    protected int getFlags() {
        return 0;
    }
}
