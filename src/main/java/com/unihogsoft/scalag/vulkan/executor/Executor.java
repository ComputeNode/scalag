package com.unihogsoft.scalag.vulkan.executor;

import java.nio.ByteBuffer;
import java.util.List;

public interface Executor {
    List<ByteBuffer> execute(List<ByteBuffer> input);
    void destroy();
}
