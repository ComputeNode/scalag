package com.unihogsoft.scalag.cl;

import org.lwjgl.*;
import org.lwjgl.opencl.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;


import static com.unihogsoft.scalag.cl.InfoUtil.checkCLError;
import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL10GL.*;
import static org.lwjgl.opencl.KHRGLSharing.*;
import static org.lwjgl.opengl.ARBCLEvent.*;
import static org.lwjgl.opengl.CGL.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.WGL.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class InitCL {

  public static class CLInitContext {
    private long device;
    private long platform;
    private long context;

    public CLInitContext(long device, long platform, long context) {
      this.device = device;
      this.platform = platform;
      this.context = context;
    }

    public long getDevice() {
      return device;
    }

    public long getPlatform() {
      return platform;
    }

    public long getContext() {
      return context;
    }
  }

  private static long loadPlatform(PointerBuffer ctxProps, MemoryStack stack) {
    IntBuffer errorcode = BufferUtils.createIntBuffer(1);
    IntBuffer pi = stack.mallocInt(1);
    checkCLError(clGetPlatformIDs(null, pi));
    if (pi.get(0) == 0) {
      throw new RuntimeException("No OpenCL platforms found.");
    }
    PointerBuffer platforms = stack.mallocPointer(pi.get(0));
    checkCLError(clGetPlatformIDs(platforms, (IntBuffer) null));
    ctxProps.put(0, CL_CONTEXT_PLATFORM)
            .put(2, 0);
    long platform = platforms.get(0);
    ctxProps.put(1, platform);
    return platform;
  }

  private static long getDevice(long platform, MemoryStack stack) {
    IntBuffer pi = stack.mallocInt(1);
    checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, null, pi));
    PointerBuffer devices = stack.mallocPointer(pi.get(0));
    checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, devices, (IntBuffer) null));
    return devices.get(1);
  }

  public static CLInitContext initCL() {
    try (MemoryStack stack = stackPush()) {
      PointerBuffer ctxProps = stack.mallocPointer(3);
      long platform = loadPlatform(ctxProps, stack);
      long device = getDevice(platform, stack);
      IntBuffer errcode_ret = stack.callocInt(1);
      CLContextCallbackI contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) -> {
        System.err.println("[LWJGL] cl_context_callback");
        System.err.println("\tInfo: " + memUTF8(errinfo));
      });
      long ctx = clCreateContext(ctxProps, device, contextCB, NULL, errcode_ret);
      InfoUtil.printRichDeviceInfo(device);
      InfoUtil.printRichPlatformInfo(platform);
      return new CLInitContext(device, platform, ctx);
    }
  }
}
