package com.scalag.vulkan.core;

import com.scalag.vulkan.utility.VulkanAssertionError;
import com.scalag.vulkan.utility.VulkanObjectHandle;
import org.lwjgl.BufferUtils;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.EXTDebugReport.vkDestroyDebugReportCallbackEXT;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * @author MarconZet
 * Created 13.04.2020
 */
public class DebugCallback extends VulkanObjectHandle {
    private static final Logger logger = LoggerFactory.getLogger(DebugCallback.class);
    private static final int DEBUG_REPORT =
            VK_DEBUG_REPORT_ERROR_BIT_EXT |
                    VK_DEBUG_REPORT_WARNING_BIT_EXT |
                    VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT;

    private final Instance instance;

    public DebugCallback(Instance instance) {
        this.instance = instance;
        create();
    }

    @Override
    protected void init() {
        VkDebugReportCallbackEXT debugCallback = new VkDebugReportCallbackEXT() {
            public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
                String decodedMessage = VkDebugReportCallbackEXT.getString(pMessage);
                int bit = 1;
                while ((flags >>= 1)>0)
                    bit <<= 1;
                switch (bit) {
                    case VK_DEBUG_REPORT_DEBUG_BIT_EXT:
                        logger.debug(decodedMessage);
                        break;
                    case VK_DEBUG_REPORT_ERROR_BIT_EXT:
                        logger.error(decodedMessage, new RuntimeException());
                        break;
                    case VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT:
                    case VK_DEBUG_REPORT_WARNING_BIT_EXT:
                        logger.warn(decodedMessage);
                        break;
                    case VK_DEBUG_REPORT_INFORMATION_BIT_EXT:
                        logger.info(decodedMessage);
                        break;
                    default:
                        logger.error("Unexpected value: " + bit);
                }
                return 0;
            }
        };
        handle = setupDebugging(DEBUG_REPORT, debugCallback);
    }

    @Override
    protected void close() {
        vkDestroyDebugReportCallbackEXT(instance.get(), handle, null);
    }

    private long setupDebugging(int flags, VkDebugReportCallbackEXT callback) {
        VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.create()
                .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
                .pNext(NULL)
                .pfnCallback(callback)
                .pUserData(NULL)
                .flags(flags);
        LongBuffer pCallback = BufferUtils.createLongBuffer(1);
        int err = vkCreateDebugReportCallbackEXT(instance.get(), dbgCreateInfo, null, pCallback);
        long callbackHandle = pCallback.get(0);
        if (err != VK_SUCCESS) {
            throw new VulkanAssertionError("Failed to create DebugCallback", err);
        }
        return callbackHandle;
    }
}
