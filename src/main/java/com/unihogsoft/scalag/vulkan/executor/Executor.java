package com.unihogsoft.scalag.vulkan.executor;

import java.nio.ByteBuffer;

public interface Executor {
    ByteBuffer[] execute(ByteBuffer[] input);
    void destroy();
}
