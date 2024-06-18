package com.scalag.vulkan.utility;

/**
 * @author MarconZet
 * Created 13.04.2020
 */
public abstract class VulkanObjectHandle extends VulkanObject {
    protected long handle;

    public long get(){
        if(!isAlive())
            throw new IllegalStateException();
        return handle;
    }
}
