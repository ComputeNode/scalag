package com.scalag.vulkan.memory

import com.scalag.vulkan.command.CommandPool
import com.scalag.vulkan.core.Device
import com.scalag.vulkan.util.Util.{Mesh, check, pushStack}
import com.scalag.vulkan.util.VulkanObjectHandle
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRAccelerationStructure.{VK_ACCESS_ACCELERATION_STRUCTURE_WRITE_BIT_KHR, *}
import org.lwjgl.vulkan.KHRBufferDeviceAddress.{VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT_KHR, vkGetBufferDeviceAddressKHR}
import org.lwjgl.vulkan.VK10.{VK_ACCESS_SHADER_READ_BIT, *}
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.util.vma.Vma.*

import scala.util.chaining.*
import java.nio.{ByteBuffer, LongBuffer}
import java.lang.Long as JLong

class BottomLevelAcceleratedStructure(as: Long, buffer: Buffer, device: Device) extends VulkanObjectHandle {
  override protected val handle: Long = as

  override protected def close(): Unit = {
    vkDestroyAccelerationStructureKHR(device.get, handle, null)
    buffer.destroy()
  }
}

object BottomLevelAcceleratedStructure {
  def create(model: Mesh, device: Device, allocator: Allocator, commandPool: CommandPool): BottomLevelAcceleratedStructure = pushStack { stack =>
    given MemoryStack = stack

    val vertBuffer = new Buffer(
      model.vertices.capacity(),
      VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
      0,
      VMA_MEMORY_USAGE_AUTO_PREFER_HOST,
      allocator
    )
    Buffer.copyBuffer(model.vertices, vertBuffer, model.vertices.capacity())

    val facesBuffer = new Buffer(
      model.faces.capacity(),
      VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
      0,
      VMA_MEMORY_USAGE_AUTO_PREFER_HOST,
      allocator
    )
    Buffer.copyBuffer(model.faces, facesBuffer, model.faces.capacity())

    val triangles = VkAccelerationStructureGeometryTrianglesDataKHR
      .calloc(stack)
      .sType$Default()
      .vertexFormat(VK10.VK_FORMAT_R32G32B32_SFLOAT)
      .vertexData(vertBuffer.deviceAddressConst)
      .indexType(VK_INDEX_TYPE_UINT32)
      .indexData(facesBuffer.deviceAddressConst)
      .maxVertex(model.v - 1)

    val asGeometry = VkAccelerationStructureGeometryKHR
      .calloc(1, stack)
      .sType$Default()
      .geometryType(VK_GEOMETRY_TYPE_TRIANGLES_KHR)
      .flags(VK_GEOMETRY_OPAQUE_BIT_KHR)
      .tap(_.geometry.triangles(triangles))

    val offset = VkAccelerationStructureBuildRangeInfoKHR
      .calloc(stack)
      .firstVertex(0)
      .primitiveCount(model.f)

    val buildInfo = VkAccelerationStructureBuildGeometryInfoKHR
      .calloc(1, stack)
      .sType$Default()
      .mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR)
      .flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR | VK_BUILD_ACCELERATION_STRUCTURE_ALLOW_COMPACTION_BIT_KHR)
      .pGeometries(asGeometry)

    val buildSizesInfo = VkAccelerationStructureBuildSizesInfoKHR.calloc(stack).sType$Default()
    vkGetAccelerationStructureBuildSizesKHR(
      device.get,
      VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR,
      buildInfo.get(0),
      stack.ints(offset.primitiveCount()),
      buildSizesInfo
    )

    val scratchBuffer = new Buffer(
      buildSizesInfo.buildScratchSize(),
      VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
      0,
      VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE,
      allocator
    )

    val queryPool = {
      val pQueryPool = stack.callocLong(1)
      val qpci = VkQueryPoolCreateInfo
        .calloc(stack)
        .sType$Default()
        .queryType(VK_QUERY_TYPE_ACCELERATION_STRUCTURE_COMPACTED_SIZE_KHR)
        .queryCount(1)
      check(vkCreateQueryPool(device.get, qpci, null, pQueryPool))
      pQueryPool.get()
    }
    vkResetQueryPool(device.get, queryPool, 0, 1)

    val asBuffer = new Buffer(
      buildSizesInfo.accelerationStructureSize(),
      VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT_KHR | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
      0,
      VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE,
      allocator
    )

    val asCreateInfo = VkAccelerationStructureCreateInfoKHR
      .calloc(stack)
      .sType$Default()
      .buffer(asBuffer.get)
      .size(buildSizesInfo.accelerationStructureSize())
      .`type`(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR)

    val pAccelerationStructure = stack.mallocLong(1)
    check(vkCreateAccelerationStructureKHR(device.get, asCreateInfo, null, pAccelerationStructure))

    buildInfo.scratchData(scratchBuffer.deviceAddress).dstAccelerationStructure(pAccelerationStructure.get(0))

    commandPool
      .singleTimeCommands { commandBuffer =>

        val memoryBarrier = VkMemoryBarrier2
          .calloc(1, stack)
          .sType$Default()
          .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
          .dstAccessMask(VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR | VK_ACCESS_ACCELERATION_STRUCTURE_WRITE_BIT_KHR | VK_ACCESS_SHADER_READ_BIT)
        val dependencyInfo = VkDependencyInfo.calloc(stack).sType$Default().pMemoryBarriers(memoryBarrier)
        vkCmdPipelineBarrier2(commandBuffer, dependencyInfo)

        vkCmdBuildAccelerationStructuresKHR(commandBuffer, buildInfo, stack.pointers(offset))

        vkCmdWriteAccelerationStructuresPropertiesKHR(
          commandBuffer,
          pAccelerationStructure,
          VK_QUERY_TYPE_ACCELERATION_STRUCTURE_COMPACTED_SIZE_KHR,
          queryPool,
          0
        )
      }
      .block()
      .destroy()

    val pCompactSize = stack.callocLong(1)
    vkGetQueryPoolResults(device.get, queryPool, 0, 1, pCompactSize, JLong.BYTES, VK_QUERY_RESULT_WAIT_BIT)
    val compactSize = pCompactSize.get(0)

    val asBufferCompact = new Buffer(
      compactSize,
      VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT_KHR | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
      0,
      VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE,
      allocator
    )

    val asCreateInfo2 = VkAccelerationStructureCreateInfoKHR
      .calloc(stack)
      .sType$Default()
      .buffer(asBufferCompact.get)
      .size(compactSize)
      .`type`(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR)

    val pAccelerationStructure2 = stack.callocLong(1)
    check(vkCreateAccelerationStructureKHR(device.get, asCreateInfo2, null, pAccelerationStructure2))

    commandPool
      .singleTimeCommands { commandBuffer =>
        val copyInfo =
          VkCopyAccelerationStructureInfoKHR
            .calloc(stack)
            .sType$Default()
            .src(pAccelerationStructure.get(0))
            .dst(pAccelerationStructure2.get(0))
            .mode(VK_COPY_ACCELERATION_STRUCTURE_MODE_COMPACT_KHR)

        vkCmdCopyAccelerationStructureKHR(commandBuffer, copyInfo)
      }
      .block()
      .destroy()

    vkDestroyAccelerationStructureKHR(device.get, pAccelerationStructure.get(0), null)
    asBuffer.destroy()
    vkDestroyQueryPool(device.get, queryPool, null)
    scratchBuffer.destroy()
    vertBuffer.destroy()
    facesBuffer.destroy()

    new BottomLevelAcceleratedStructure(pAccelerationStructure2.get(0), asBufferCompact, device)
  }

}
