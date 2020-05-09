package com.unihogsoft.scalag;

import java.nio.ByteBuffer;

/**
 * @author MarconZet
 * Created 09.05.2020
 */
public interface Executor {
    ByteBuffer[] execute(ByteBuffer[] input);
    void destroy();
}
