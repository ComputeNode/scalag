package com.scalag.vulkan.core

import com.scalag.vulkan.VulkanContext.VALIDATION_LAYERS
import com.scalag.vulkan.core.Device.{MacOsExtension, VmaAllocatorExtension}
import com.scalag.vulkan.util.Util.{check, pushStack}
import com.scalag.vulkan.util.{VulkanAssertionError, VulkanObject}
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME
import org.lwjgl.vulkan.KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*

import java.nio.{ByteBuffer, FloatBuffer, IntBuffer}
import scala.util.Using

/** @author
  *   MarconZet Created 13.04.2020
  */

object Device {
  val VmaAllocatorExtension = List(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME)
  val MacOsExtension = VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME
}

class Device(enableValidationLayers: Boolean, instance: Instance) extends VulkanObject {

  val physicalDevice: VkPhysicalDevice = pushStack { stack =>

    val pPhysicalDeviceCount = stack.callocInt(1)
    check(vkEnumeratePhysicalDevices(instance.get, pPhysicalDeviceCount, null), "Failed to get number of physical devices")
    val deviceCount = pPhysicalDeviceCount.get(0)
    if (deviceCount == 0)
      throw new AssertionError("Failed to find GPUs with Vulkan support")

    val pPhysicalDevices = stack.callocPointer(deviceCount)
    check(vkEnumeratePhysicalDevices(instance.get, pPhysicalDeviceCount, pPhysicalDevices), "Failed to get physical devices")

    new VkPhysicalDevice(pPhysicalDevices.get(), instance.get)
  }

  val computeQueueFamily: Int = pushStack { stack =>
    val pQueueFamilyCount = stack.callocInt(1)
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, null)
    val queueFamilyCount = pQueueFamilyCount.get(0)

    val pQueueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount, stack)
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, pQueueFamilies)

    val queues = 0 until queueFamilyCount
    queues
      .find { i =>
        val queueFamily = pQueueFamilies.get(i)
        val maskedFlags = (~(VK_QUEUE_TRANSFER_BIT | VK_QUEUE_SPARSE_BINDING_BIT) & queueFamily.queueFlags())
        ~(VK_QUEUE_GRAPHICS_BIT & maskedFlags) > 0 && (VK_QUEUE_COMPUTE_BIT & maskedFlags) > 0
      }
      .orElse(queues.find { i =>
        val queueFamily = pQueueFamilies.get(i)
        val maskedFlags = (~(VK_QUEUE_TRANSFER_BIT | VK_QUEUE_SPARSE_BINDING_BIT) & queueFamily.queueFlags())
        (VK_QUEUE_COMPUTE_BIT & maskedFlags) > 0
      })
      .getOrElse(throw new AssertionError("No suitable queue family found for computing"))
  }

  private val device: VkDevice =
    pushStack { stack =>
      val pPropertiesCount = stack.callocInt(1)
      check(
        vkEnumerateDeviceExtensionProperties(physicalDevice, null.asInstanceOf[ByteBuffer], pPropertiesCount, null),
        "Failed to get number of properties extension"
      )
      val propertiesCount = pPropertiesCount.get(0)

      val pProperties = VkExtensionProperties.calloc(propertiesCount, stack)
      check(
        vkEnumerateDeviceExtensionProperties(physicalDevice, null.asInstanceOf[ByteBuffer], pPropertiesCount, pProperties),
        "Failed to get extension properties"
      )

      val additionalExtension = pProperties.stream().anyMatch(x => x.extensionNameString().equals(MacOsExtension))

      val pQueuePriorities = stack.callocFloat(1).put(1.0f)
      pQueuePriorities.flip()

      val pQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
      pQueueCreateInfo
        .get(0)
        .sType$Default()
        .pNext(0)
        .flags(0)
        .queueFamilyIndex(computeQueueFamily)
        .pQueuePriorities(pQueuePriorities)

      val ppExtensionNames = stack.callocPointer(VmaAllocatorExtension.length + 1)
      VmaAllocatorExtension.foreach(extension => ppExtensionNames.put(stack.ASCII(extension)))

      if (additionalExtension)
        ppExtensionNames.put(stack.ASCII(MacOsExtension))
      ppExtensionNames.flip()

      val deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack)
      val pCreateInfo = VkDeviceCreateInfo
        .create()
        .sType$Default()
        .pNext(NULL)
        .pQueueCreateInfos(pQueueCreateInfo)
        .pEnabledFeatures(deviceFeatures)
        .ppEnabledExtensionNames(ppExtensionNames)

      if (enableValidationLayers) {
        val ppValidationLayers = stack.callocPointer(VALIDATION_LAYERS.length)
        VALIDATION_LAYERS.foreach(layer => ppValidationLayers.put(stack.ASCII(layer)))
        pCreateInfo.ppEnabledLayerNames(ppValidationLayers.flip())
      }

      val pDevice = stack.callocPointer(1)
      check(vkCreateDevice(physicalDevice, pCreateInfo, null, pDevice), "Failed to create device")
      new VkDevice(pDevice.get(0), physicalDevice, pCreateInfo)
    }

  def get: VkDevice = device

  override protected def close(): Unit =
    vkDestroyDevice(device, null)
}
