package com.unihogsoft.scalag.vulkan.utility;


import java.io.IOException;
import java.io.InputStream;
import java.nio.*;

/**
 * @author Jaca777
 * Also copied from Wrap :)
 */
public class BufferTools {

  /**
   * Allocates a direct native-ordered bytebuffer with the specified capacity.
   *
   * @param capacity The capacity, in bytes
   * @return a ByteBuffer
   */
  public static ByteBuffer createByteBuffer(int capacity) {
    return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
  }

  /**
   * Allocates a direct native-order shortbuffer with the specified number
   * of elements.
   *
   * @param capacity The capacity, in shorts
   * @return a ShortBuffer
   */
  public static ShortBuffer createShortBuffer(int capacity) {
    return createByteBuffer(capacity << 1).asShortBuffer();
  }

  /**
   * Allocates a direct native-order charbuffer with the specified number
   * of elements.
   *
   * @param capacity The capacity, in chars
   * @return an CharBuffer
   */
  public static CharBuffer createCharBuffer(int capacity) {
    return createByteBuffer(capacity << 1).asCharBuffer();
  }

  /**
   * Allocates a direct native-order intbuffer with the specified number
   * of elements.
   *
   * @param capacity The capacity, in ints
   * @return an IntBuffer
   */
  public static IntBuffer createIntBuffer(int capacity) {
    return createByteBuffer(capacity << 2).asIntBuffer();
  }

  /**
   * Allocates a direct native-order longbuffer with the specified number
   * of elements.
   *
   * @param capacity The capacity, in longs
   * @return an LongBuffer
   */
  public static LongBuffer createLongBuffer(int capacity) {
    return createByteBuffer(capacity << 3).asLongBuffer();
  }

  /**
   * Allocates a direct native-order floatbuffer with the specified number
   * of elements.
   *
   * @param capacity The capacity, in floats
   * @return a FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(int capacity) {
    return createByteBuffer(capacity << 2).asFloatBuffer();
  }

  /**
   * Allocates a direct native-order doublebuffer with the specified number
   * of elements.
   *
   * @param capacity The capacity, in doubles
   * @return a DoubleBuffer
   */
  public static DoubleBuffer createDoubleBuffer(int capacity) {
    return createByteBuffer(capacity << 3).asDoubleBuffer();
  }

  /**
   * Stores array in a direct byte buffer.
   *
   * @param data Array to be stored in a direct buffer.
   * @return Direct buffer containing array.
   */
  public static ByteBuffer toDirectByteBuffer(float[] data) {
    ByteBuffer buffer = createByteBuffer(data.length * 4);
    buffer.asFloatBuffer()
            .put(data);
    return buffer;
  }

  /**
   * Stores array in a direct byte buffer.
   *
   * @param data Array to be stored in a direct buffer.
   * @return Direct buffer containing array.
   */
  public static ByteBuffer toDirectByteBuffer(int[] data) {
    ByteBuffer buffer = createByteBuffer(data.length  * 4);
    buffer.asIntBuffer()
            .put(data);
    return buffer;
  }

  /**
   * Stores array in a direct buffer.
   *
   * @param data Array to be stored in a direct buffer.
   * @return Direct buffer containing array.
   */
  public static FloatBuffer toDirectBuffer(float[] data) {
    FloatBuffer buffer = createFloatBuffer(data.length)
            .put(data);
    buffer.flip();
    return buffer;
  }

  /**
   * Stores array in a direct buffer.
   *
   * @param data Array to be stored in a direct buffer.
   * @return Direct buffer containing array.
   */
  public static IntBuffer toDirectBuffer(int[] data) {
    IntBuffer buffer = createIntBuffer(data.length)
            .put(data);
    buffer.flip();
    return buffer;
  }

  /**
   * Stores array in a direct buffer.
   *
   * @param data Array to be stored in a direct buffer.
   * @return Direct buffer containing array.
   */
  public static ShortBuffer toDirectBuffer(short[] data) {
    ShortBuffer buffer = createShortBuffer(data.length)
            .put(data);
    buffer.flip();
    return buffer;
  }

  /**
   * Stores array in a direct buffer.
   *
   * @param data Array to be stored in a direct buffer.
   * @return Direct buffer containing array.
   */
  public static ByteBuffer toDirectBuffer(byte[] data) {
    ByteBuffer buffer = createByteBuffer(data.length)
            .put(data);
    buffer.flip();
    return buffer;
  }

}
