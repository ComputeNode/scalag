package com.scalag.vulkan.core;

import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObject
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo

import scala.util.Using
import com.scalag.vulkan.VulkanContext.VALIDATION_LAYERS
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1

import scala.collection.mutable

/** @author
  *   MarconZet Created 13.04.2020
  */
object Instance {
  val VALIDATION_LAYERS_INSTANCE_EXTENSIONS: Seq[String] = List(VK_EXT_DEBUG_REPORT_EXTENSION_NAME)
  val INSTANCE_EXTENSIONS: Seq[String] = List()
}

class Instance(enableValidationLayers: Boolean) extends VulkanObject {

  private val instance: VkInstance = Using(stackPush()) { stack =>
    val appInfo = VkApplicationInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
      .pNext(NULL)
      .pApplicationName(stack.UTF8("ScalaG MVP"))
      .pEngineName(stack.UTF8("ScalaG Computing Engine"))
      .applicationVersion(VK_MAKE_VERSION(0, 1, 0))
      .engineVersion(VK_MAKE_VERSION(0, 1, 0))
      .apiVersion(VK_API_VERSION_1_1);

    val ppEnabledExtensionNames = getInstanceExtensions(stack);
    val ppEnabledLayerNames = getValidationLayers(stack);

    val pCreateInfo = VkInstanceCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
      .pNext(NULL)
      .pApplicationInfo(appInfo)
      .ppEnabledExtensionNames(ppEnabledExtensionNames)
      .ppEnabledLayerNames(ppEnabledLayerNames);
    val pInstance = stack.mallocPointer(1);
    val err = vkCreateInstance(pCreateInfo, null, pInstance);
    val instance = pInstance.get(0);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create VkInstance", err);
    new VkInstance(instance, pCreateInfo);
  }.get

  private def getValidationLayers(stack: MemoryStack) =
    if (enableValidationLayers) {
      val validationLayers = stack.callocPointer(VALIDATION_LAYERS.length);
      VALIDATION_LAYERS.foreach(x => validationLayers.put(stack.ASCII(x)));
      validationLayers.flip();
    } else
      null

  private def getInstanceExtensions(stack: MemoryStack) = {
    val extensions = mutable.Buffer.from(Instance.INSTANCE_EXTENSIONS)
    if (enableValidationLayers)
      extensions.addAll(Instance.VALIDATION_LAYERS_INSTANCE_EXTENSIONS)

    val ppEnabledExtensionNames = stack.callocPointer(extensions.size);
    extensions.foreach(x => ppEnabledExtensionNames.put(stack.ASCII(x)));
    ppEnabledExtensionNames.flip();
  }

  override protected def close(): Unit =
    vkDestroyInstance(instance, null)

  def get: VkInstance = instance
}
