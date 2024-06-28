package com.scalag.vulkan.core;

import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObject
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.{VK, VkApplicationInfo, VkExtensionProperties, VkInstance, VkInstanceCreateInfo, VkLayerProperties}

import scala.util.Using
import com.scalag.vulkan.VulkanContext.VALIDATION_LAYERS
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME
import org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR

import scala.collection.mutable

/** @author
  *   MarconZet Created 13.04.2020
  */
object Instance {
  val ValidationLayersExtensions: Seq[String] = List(VK_EXT_DEBUG_REPORT_EXTENSION_NAME)
  val MacOsExtensions: Seq[String] = List(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)

  def printCapibilities(stack: MemoryStack): Unit = {
    val ip = stack.ints(1)
    vkEnumerateInstanceLayerProperties(ip, null)
    val availableLayers = VkLayerProperties.malloc(ip.get(0), stack)
    vkEnumerateInstanceLayerProperties(ip, availableLayers)
    availableLayers.forEach(x => println(x.layerNameString()))

    vkEnumerateInstanceExtensionProperties(null.asInstanceOf[String], ip, null)
    val instance_extensions = VkExtensionProperties.malloc(ip.get(0), stack)
    vkEnumerateInstanceExtensionProperties(null.asInstanceOf[String], ip, instance_extensions)
    instance_extensions.forEach(x => println(x.extensionNameString()))
  }
}

class Instance(enableValidationLayers: Boolean) extends VulkanObject {

  private val instance: VkInstance = Using(stackPush()) { stack =>

    Instance.printCapibilities(stack)
    val appInfo = VkApplicationInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
      .pNext(NULL)
      .pApplicationName(stack.UTF8("ScalaG MVP"))
      .pEngineName(stack.UTF8("ScalaG Computing Engine"))
      .applicationVersion(VK_MAKE_VERSION(0, 1, 0))
      .engineVersion(VK_MAKE_VERSION(0, 1, 0))
      .apiVersion(VK.getInstanceVersionSupported);

    val ppEnabledExtensionNames = getInstanceExtensions(stack);
    val ppEnabledLayerNames = getValidationLayers(stack);

    val pCreateInfo = VkInstanceCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
      .flags(VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR)
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
    val extensions = mutable.Buffer.from(Instance.MacOsExtensions)
    if (enableValidationLayers)
      extensions.addAll(Instance.ValidationLayersExtensions)

    val ppEnabledExtensionNames = stack.callocPointer(extensions.size);
    extensions.foreach(x => ppEnabledExtensionNames.put(stack.ASCII(x)));
    ppEnabledExtensionNames.flip();
  }

  override protected def close(): Unit =
    vkDestroyInstance(instance, null)

  def get: VkInstance = instance
}
