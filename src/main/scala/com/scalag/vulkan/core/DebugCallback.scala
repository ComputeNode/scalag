package com.scalag.vulkan.core;

import com.scalag.vulkan.core.DebugCallback.DEBUG_REPORT
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import org.lwjgl.BufferUtils
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT
import org.lwjgl.vulkan.VkDebugReportCallbackEXT
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.LongBuffer
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.EXTDebugReport.vkDestroyDebugReportCallbackEXT
import org.lwjgl.vulkan.VK10.VK_SUCCESS

import java.lang.Integer.highestOneBit;

/** @author
  *   MarconZet Created 13.04.2020
  */
object DebugCallback {
  val DEBUG_REPORT = VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT | VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT
}

class DebugCallback(instance: Instance) extends VulkanObjectHandle {
  private val logger = LoggerFactory.getLogger(this.getClass);

  override protected val handle: Long = {
    val debugCallback = new VkDebugReportCallbackEXT() {
      def invoke(flags: Int, objectType: Int, `object`: Long, location: Long, messageCode: Int, pLayerPrefix: Long, pMessage: Long, pUserData: Long)
        : Int = {
        val decodedMessage = VkDebugReportCallbackEXT.getString(pMessage);
        highestOneBit(flags) match {
          case VK_DEBUG_REPORT_DEBUG_BIT_EXT =>
            logger.debug(decodedMessage);
          case VK_DEBUG_REPORT_ERROR_BIT_EXT =>
            logger.error(decodedMessage, new RuntimeException());
          case VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT =>
            logger.warn(decodedMessage);
          case VK_DEBUG_REPORT_INFORMATION_BIT_EXT =>
            logger.info(decodedMessage);
          case x => logger.error(s"Unexpected value: x, message: $decodedMessage");
        }
        0
      }
    };
    setupDebugging(DEBUG_REPORT, debugCallback);
  }

  override protected def close(): Unit =
    vkDestroyDebugReportCallbackEXT(instance.get, handle, null);

  private def setupDebugging(flags: Int, callback: VkDebugReportCallbackEXT): Long = {
    val dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT
      .create()
      .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
      .pNext(NULL)
      .pfnCallback(callback)
      .pUserData(NULL)
      .flags(flags);
    val pCallback = BufferUtils.createLongBuffer(1);
    val err = vkCreateDebugReportCallbackEXT(instance.get, dbgCreateInfo, null, pCallback);
    val callbackHandle = pCallback.get(0);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create DebugCallback", err);
    callbackHandle;
  }
}
