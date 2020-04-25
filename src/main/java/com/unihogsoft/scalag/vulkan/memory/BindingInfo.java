package com.unihogsoft.scalag.vulkan.memory;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;

/**
 * @author MarconZet
 * Created 25.04.2020
 */
public class BindingInfo {
    public final static int
            OP_DO_NOTHING = 0,
            OP_WRITE_BEFORE_EXECUTION = VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            OP_READ_AFTER_OPERATION = VK_BUFFER_USAGE_TRANSFER_SRC_BIT;


    private final int binding;
    private final int size;

    public BindingInfo(int binding, int size) {
        this.binding = binding;
        this.size = size;
    }

    public int getBinding() {
        return binding;
    }

    public int getSize() {
        return size;
    }
}
