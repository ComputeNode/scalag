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

    public final static int
            BINDING_TYPE_WORK = 0,
            BINDING_TYPE_INPUT = 1,
            BINDING_TYPE_OUTPUT = 2;


    private final int
            binding,
            size,
            type;

    public BindingInfo(int binding, int size, int type) {
        this.binding = binding;
        this.size = size;
        this.type = type;
    }

    public int getBinding() {
        return binding;
    }

    public int getSize() {
        return size;
    }

    public int getType() {
        return type;
    }

    public int getUsageBit(){
        switch (type){
            case BINDING_TYPE_WORK:
                return 0;
            case BINDING_TYPE_INPUT:
                return VK_BUFFER_USAGE_TRANSFER_DST_BIT;
            case BINDING_TYPE_OUTPUT:
                return VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
            default:
                throw new IllegalStateException();
        }
    }
}
