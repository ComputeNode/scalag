package com.scalag.vulkan.executor;

import lombok.Data;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;

@Data
public class BufferAction {
    public static final int
        DO_NOTHING = 0,
        LOAD_INTO = VK_BUFFER_USAGE_TRANSFER_DST_BIT,
        LOAD_FROM = VK_BUFFER_USAGE_TRANSFER_SRC_BIT;

    private final int action;
}
