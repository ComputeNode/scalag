package com.unihogsoft.scalag.vulkan.utility;

/**
 * @author MarconZet
 * Created 13.04.2020
 */
public abstract class VulkanObject {
    private boolean alive;

    public VulkanObject() {
        alive = false;
    }

    public void destroy() {
        if(!alive)
            throw new IllegalStateException();
        close();
        alive = false;
    }

    protected void create() {
        if(alive)
            throw new IllegalStateException();
        init();
        alive = true;
    }

    protected abstract void init();

    protected abstract void close();

    public boolean isAlive() {
        return alive;
    }
}
