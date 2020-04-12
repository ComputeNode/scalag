package com.unihogsoft.scalag.cl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/** OpenCL object info utilities. */
public final class InfoUtil {

    private InfoUtil() {
    }

    public static String getPlatformInfoStringASCII(long cl_platform_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetPlatformInfo(cl_platform_id, param_name, (ByteBuffer)null, pp));
            int bytes = (int)pp.get(0);
            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetPlatformInfo(cl_platform_id, param_name, buffer, null));
            return memASCII(buffer, bytes - 1);
        }
    }

    public static String getPlatformInfoStringUTF8(long cl_platform_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetPlatformInfo(cl_platform_id, param_name, (ByteBuffer)null, pp));
            int bytes = (int) pp.get(0);
            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetPlatformInfo(cl_platform_id, param_name, buffer, null));
            return memUTF8(buffer, bytes - 1);
        }
    }

    public static int getDeviceInfoInt(long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pl = stack.mallocInt(1);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, pl, null));
            return pl.get(0);
        }
    }

    public static long getDeviceInfoLong(long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pl = stack.mallocLong(1);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, pl, null));
            return pl.get(0);
        }
    }

    public static long getDeviceInfoPointer(long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, pp, null));
            return pp.get(0);
        }
    }

    public static String getDeviceInfoStringUTF8(long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, (ByteBuffer)null, pp));
            int bytes = (int)pp.get(0);

            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetDeviceInfo(cl_device_id, param_name, buffer, null));

            return memUTF8(buffer, bytes - 1);
        }
    }

    public static int getProgramBuildInfoInt(long cl_program_id, long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pl = stack.mallocInt(1);
            checkCLError(clGetProgramBuildInfo(cl_program_id, cl_device_id, param_name, pl, null));
            return pl.get(0);
        }
    }

    public static String getProgramBuildInfoStringASCII(long cl_program_id, long cl_device_id, int param_name) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetProgramBuildInfo(cl_program_id, cl_device_id, param_name, (ByteBuffer)null, pp));
            int bytes = (int)pp.get(0);

            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetProgramBuildInfo(cl_program_id, cl_device_id, param_name, buffer, null));

            return memASCII(buffer, bytes - 1);
        }
    }

    public static void checkCLError(IntBuffer errcode) {
        checkCLError(errcode.get(errcode.position()));
    }

    public static void printPlatformInfo(long platform, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + getPlatformInfoStringUTF8(platform, param));
    }

    public static void printDeviceInfo(long device, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + getDeviceInfoStringUTF8(device, param));
    }

    public static void checkCLError(int errcode) {
        if (errcode != CL_SUCCESS) {
            throw new RuntimeException(String.format("OpenCL error [%d]", errcode));
        }
    }

    public static void printRichPlatformInfo(long platform) {
        printPlatformInfo(platform, "CL_PLATFORM_PROFILE", CL_PLATFORM_PROFILE);
        printPlatformInfo(platform, "CL_PLATFORM_VERSION", CL_PLATFORM_VERSION);
        printPlatformInfo(platform, "CL_PLATFORM_NAME", CL_PLATFORM_NAME);
        printPlatformInfo(platform, "CL_PLATFORM_VENDOR", CL_PLATFORM_VENDOR);
        printPlatformInfo(platform, "CL_PLATFORM_EXTENSIONS", CL_PLATFORM_EXTENSIONS);
    }

    public static void printRichDeviceInfo(long device) {
        System.out.println("\tCL_DEVICE_TYPE = " + getDeviceInfoLong(device, CL_DEVICE_TYPE));
        System.out.println("\tCL_DEVICE_VENDOR_ID = " + getDeviceInfoInt(device, CL_DEVICE_VENDOR_ID));
        System.out.println("\tCL_DEVICE_MAX_COMPUTE_UNITS = " + getDeviceInfoInt(device, CL_DEVICE_MAX_COMPUTE_UNITS));
        System.out
                .println("\tCL_DEVICE_MAX_WORK_ITEM_DIMENSIONS = " + getDeviceInfoInt(device, CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS));
        System.out.println("\tCL_DEVICE_MAX_WORK_GROUP_SIZE = " + getDeviceInfoPointer(device, CL_DEVICE_MAX_WORK_GROUP_SIZE));
        System.out.println("\tCL_DEVICE_MAX_CLOCK_FREQUENCY = " + getDeviceInfoInt(device, CL_DEVICE_MAX_CLOCK_FREQUENCY));
        System.out.println("\tCL_DEVICE_ADDRESS_BITS = " + getDeviceInfoInt(device, CL_DEVICE_ADDRESS_BITS));
        System.out.println("\tCL_DEVICE_AVAILABLE = " + (getDeviceInfoInt(device, CL_DEVICE_AVAILABLE) != 0));
        System.out.println("\tCL_DEVICE_COMPILER_AVAILABLE = " + (getDeviceInfoInt(device, CL_DEVICE_COMPILER_AVAILABLE) != 0));

        printDeviceInfo(device, "CL_DEVICE_NAME", CL_DEVICE_NAME);
        printDeviceInfo(device, "CL_DEVICE_VENDOR", CL_DEVICE_VENDOR);
        printDeviceInfo(device, "CL_DRIVER_VERSION", CL_DRIVER_VERSION);
        printDeviceInfo(device, "CL_DEVICE_PROFILE", CL_DEVICE_PROFILE);
        printDeviceInfo(device, "CL_DEVICE_VERSION", CL_DEVICE_VERSION);
        printDeviceInfo(device, "CL_DEVICE_EXTENSIONS", CL_DEVICE_EXTENSIONS);
    }
}
