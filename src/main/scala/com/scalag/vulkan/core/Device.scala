package com.scalag.vulkan.core

import com.scalag.vulkan.VulkanContext.ValidationLayer
import com.scalag.vulkan.core.Device.{MacOsExtension, SyncExtension, RayTracingExtensions}
import com.scalag.vulkan.util.Util.{check, pushStack}
import com.scalag.vulkan.util.VulkanObject
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME
import org.lwjgl.vulkan.KHRSynchronization2.VK_KHR_SYNCHRONIZATION_2_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK11.*
import org.lwjgl.vulkan.KHRDeferredHostOperations.VK_KHR_DEFERRED_HOST_OPERATIONS_EXTENSION_NAME
import java.nio.ByteBuffer
import scala.jdk.CollectionConverters.given
import org.lwjgl.vulkan.KHRAccelerationStructure.VK_KHR_ACCELERATION_STRUCTURE_EXTENSION_NAME
import org.lwjgl.vulkan.KHRRayTracingPipeline.VK_KHR_RAY_TRACING_PIPELINE_EXTENSION_NAME
import org.lwjgl.vulkan.KHRRayTracingPipeline.*

/** @author
  *   MarconZet Created 13.04.2020
  */

object Device {
  final val MacOsExtension = VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME
  final val SyncExtension = VK_KHR_SYNCHRONIZATION_2_EXTENSION_NAME

  final val RayTracingExtensions =
    Seq(VK_KHR_ACCELERATION_STRUCTURE_EXTENSION_NAME, VK_KHR_RAY_TRACING_PIPELINE_EXTENSION_NAME, VK_KHR_DEFERRED_HOST_OPERATIONS_EXTENSION_NAME)
}

class Device(instance: Instance) extends VulkanObject {

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

      val deviceExtensions = pProperties.iterator().asScala.map(_.extensionNameString())
      val deviceExtensionsSet = deviceExtensions.toSet

      val vulkan12Features = VkPhysicalDeviceVulkan12Features
        .calloc(stack)
        .sType$Default()

      val vulkan13Features = VkPhysicalDeviceVulkan13Features
        .calloc(stack)
        .sType$Default()

      val rayTracingPipelineFeatures = VkPhysicalDeviceRayTracingPipelineFeaturesKHR.calloc(stack).sType$Default()
      val accelerationStructureFeatures = VkPhysicalDeviceAccelerationStructureFeaturesKHR.calloc(stack).sType$Default()

      val physicalDeviceFeatures = VkPhysicalDeviceFeatures2
        .calloc(stack)
        .sType$Default()
        .pNext(vulkan12Features)
        .pNext(vulkan13Features)
        .pNext(rayTracingPipelineFeatures)
        .pNext(accelerationStructureFeatures)

      vkGetPhysicalDeviceFeatures2(physicalDevice, physicalDeviceFeatures)

      val rayTracingPipelineProperties = VkPhysicalDeviceRayTracingPipelinePropertiesKHR.calloc(stack).sType$Default()
      val physicalDeviceProperties = VkPhysicalDeviceProperties2.calloc(stack).sType$Default().pNext(rayTracingPipelineProperties)
      vkGetPhysicalDeviceProperties2(physicalDevice, physicalDeviceProperties)

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

      val extensions = Seq(MacOsExtension, SyncExtension).concat(RayTracingExtensions).filter(deviceExtensionsSet)
      val ppExtensionNames = stack.callocPointer(extensions.length)
      extensions.foreach(extension => ppExtensionNames.put(stack.ASCII(extension)))
      ppExtensionNames.flip()

      val sync2 = VkPhysicalDeviceSynchronization2Features
        .calloc(stack)
        .sType$Default()
        .synchronization2(true)

      val pCreateInfo = VkDeviceCreateInfo
        .create()
        .sType$Default()
        .pNext(sync2)
        .pNext(rayTracingPipelineFeatures)
        .pNext(accelerationStructureFeatures)
        .pQueueCreateInfos(pQueueCreateInfo)
        .ppEnabledExtensionNames(ppExtensionNames)

      if (instance.enabledLayers.contains(ValidationLayer)) {
        val ppValidationLayers = stack.callocPointer(1).put(stack.ASCII(ValidationLayer))
        pCreateInfo.ppEnabledLayerNames(ppValidationLayers.flip())
      }

      assert(vulkan13Features.synchronization2() || extensions.contains(SyncExtension))
      assert(rayTracingPipelineFeatures.rayTracingPipeline() && accelerationStructureFeatures.accelerationStructure())

      val pDevice = stack.callocPointer(1)
      check(vkCreateDevice(physicalDevice, pCreateInfo, null, pDevice), "Failed to create device")
      new VkDevice(pDevice.get(0), physicalDevice, pCreateInfo)
    }

  def get: VkDevice = device

  override protected def close(): Unit =
    vkDestroyDevice(device, null)
}
