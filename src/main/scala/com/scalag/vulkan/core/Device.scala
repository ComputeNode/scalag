package com.scalag.vulkan.core;

import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObject
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*

import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import com.scalag.vulkan.VulkanContext.VALIDATION_LAYERS
import com.scalag.vulkan.core.Device.{DEVICE_EXTENSIONS, vk_khr_portability_subset}
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*

import scala.util.Using;

/** @author
  *   MarconZet Created 13.04.2020
  */

object Device {
  val DEVICE_EXTENSIONS = List(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME)
  val vk_khr_portability_subset = "VK_KHR_portability_subset";
}

class Device(enableValidationLayers: Boolean, instance: Instance) extends VulkanObject {

  val physicalDevice: VkPhysicalDevice = Using(stackPush()) { stack =>

    val pPhysicalDeviceCount = stack.callocInt(1);
    var err = vkEnumeratePhysicalDevices(instance.get, pPhysicalDeviceCount, null);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to get number of physical devices", err);
    val deviceCount = pPhysicalDeviceCount.get(0);
    if (deviceCount == 0)
      throw new AssertionError("Failed to find GPUs with Vulkan support");

    val pPhysicalDevices = stack.callocPointer(deviceCount);
    err = vkEnumeratePhysicalDevices(instance.get, pPhysicalDeviceCount, pPhysicalDevices);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to get physical devices", err);

    new VkPhysicalDevice(pPhysicalDevices.get(), instance.get);
  }.get

  val computeQueueFamily: Int = Using(stackPush()) { stack =>

    val pQueueFamilyCount = stack.callocInt(1);
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, null);
    val queueFamilyCount = pQueueFamilyCount.get(0);

    val pQueueFamilies = VkQueueFamilyProperties.callocStack(queueFamilyCount);
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, pQueueFamilies);

    val queues = 0 to queueFamilyCount
    queues
      .find { i =>
        val queueFamily = pQueueFamilies.get(i);
        val maskedFlags = (~(VK_QUEUE_TRANSFER_BIT | VK_QUEUE_SPARSE_BINDING_BIT) & queueFamily.queueFlags())
        ~(VK_QUEUE_GRAPHICS_BIT & maskedFlags) > 0 && (VK_QUEUE_COMPUTE_BIT & maskedFlags) > 0
      }
      .orElse(queues.find { i =>
        val queueFamily = pQueueFamilies.get(i);
        val maskedFlags = (~(VK_QUEUE_TRANSFER_BIT | VK_QUEUE_SPARSE_BINDING_BIT) & queueFamily.queueFlags())
        (VK_QUEUE_COMPUTE_BIT & maskedFlags) > 0
      })
      .getOrElse(throw new AssertionError("No suitable queue family found for computing"))
  }.get

  private val device: VkDevice =
    Using(stackPush()) { stack =>
      val pPropertiesCount = stack.callocInt(1);
      var err = vkEnumerateDeviceExtensionProperties(physicalDevice, null.asInstanceOf[ByteBuffer], pPropertiesCount, null);
      if (err != VK_SUCCESS)
        throw new AssertionError("Failed to get number of properties extension");
      val propertiesCount = pPropertiesCount.get(0);

      val pProperties = VkExtensionProperties.callocStack(propertiesCount);
      err = vkEnumerateDeviceExtensionProperties(physicalDevice, null.asInstanceOf[ByteBuffer], pPropertiesCount, pProperties);
      if (err != VK_SUCCESS)
        throw new VulkanAssertionError("Failed to extension properties", err);

      val additionalExtension = pProperties.stream().anyMatch(x => x.extensionNameString().equals(vk_khr_portability_subset));

      val pQueuePriorities = stack.callocFloat(1).put(1.0f);
      pQueuePriorities.flip();

      val pQueueCreateInfo = VkDeviceQueueCreateInfo.callocStack(1);
      pQueueCreateInfo
        .get(0)
        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
        .pNext(0)
        .flags(0)
        .queueFamilyIndex(computeQueueFamily)
        .pQueuePriorities(pQueuePriorities);

      val ppExtensionNames = stack.callocPointer(DEVICE_EXTENSIONS.length + 1);
      DEVICE_EXTENSIONS.foreach(extension => ppExtensionNames.put(stack.ASCII(extension)))

      if (additionalExtension)
        ppExtensionNames.put(stack.ASCII(vk_khr_portability_subset));
      ppExtensionNames.flip();

      val deviceFeatures = VkPhysicalDeviceFeatures.callocStack();
      val pCreateInfo = VkDeviceCreateInfo
        .create()
        .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
        .pNext(NULL)
        .pQueueCreateInfos(pQueueCreateInfo)
        .pEnabledFeatures(deviceFeatures)
        .ppEnabledExtensionNames(ppExtensionNames);

      if (enableValidationLayers) {
        val ppValidationLayers = stack.callocPointer(VALIDATION_LAYERS.length);
        VALIDATION_LAYERS.foreach(layer => ppValidationLayers.put(stack.ASCII(layer)))
        pCreateInfo.ppEnabledLayerNames(ppValidationLayers.flip());
      }

      val pDevice = stack.callocPointer(1);
      err = vkCreateDevice(physicalDevice, pCreateInfo, null, pDevice);
      if (err != VK_SUCCESS)
        throw new VulkanAssertionError("Failed to create device", err);
      new VkDevice(pDevice.get(0), physicalDevice, pCreateInfo);
    }.get

  override protected def close(): Unit =
    vkDestroyDevice(device, null);

  def get: VkDevice = device
}
