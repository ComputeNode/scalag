package com.unihogsoft.scalag.cl;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class BufferUtilsExt {
  public static FloatBuffer toFloatBuffer(float[] floats) {
    FloatBuffer buf = BufferUtils.createFloatBuffer(floats.length).put(floats);
    buf.rewind();
    return buf;
  }
  public static void printFloatBuffer(FloatBuffer buffer) {
    for (int i = 0; i < buffer.capacity(); i++) {
      System.out.print(buffer.get(i)+" ");
    }
    System.out.println("");
  }
}
