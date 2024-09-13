package com.scalag.vulkan.memory

import com.scalag.vulkan.command.{CommandPool, Fence}
import com.scalag.vulkan.util.Util.{check, pushStack}
import com.scalag.vulkan.util.{VulkanAssertionError, VulkanObjectHandle}
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.KHRBufferDeviceAddress.vkGetBufferDeviceAddressKHR
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.{
  VkBufferCopy,
  VkBufferCreateInfo,
  VkBufferDeviceAddressInfo,
  VkCommandBuffer,
  VkDeviceOrHostAddressConstKHR,
  VkDeviceOrHostAddressKHR
}

import java.nio.{ByteBuffer, LongBuffer}
import scala.util.Using

/** @author
  *   MarconZet Created 11.05.2019
  */
class Buffer(val size: Int, usage: Int, flags: Int, memUsage: Int, val allocator: Allocator) extends VulkanObjectHandle {

  def this(size: Long, usage: Int, flags: Int, memUsage: Int, allocator: Allocator) =
    this(size.toInt, usage, flags, memUsage, allocator)

  val (handle, allocation) = pushStack { stack =>
    val bufferInfo = VkBufferCreateInfo
      .calloc(stack)
      .sType$Default()
      .pNext(NULL)
      .size(size)
      .usage(usage)
      .flags(0)
      .sharingMode(VK_SHARING_MODE_EXCLUSIVE)

    val allocInfo = VmaAllocationCreateInfo
      .calloc(stack)
      .usage(memUsage)
      .requiredFlags(flags)

    val pBuffer = stack.callocLong(1)
    val pAllocation = stack.callocPointer(1)
    check(vmaCreateBuffer(allocator.get, bufferInfo, allocInfo, pBuffer, pAllocation, null), "Failed to create buffer")
    (pBuffer.get(), pAllocation.get())
  }

  def get(dst: Array[Byte]): Unit = {
    val len = Math.min(dst.length, size)
    val byteBuffer = memCalloc(len)
    Buffer.copyBuffer(this, byteBuffer, len)
    byteBuffer.get(dst)
    memFree(byteBuffer)
  }

  def deviceAddress(using stack: MemoryStack): VkDeviceOrHostAddressKHR =
    VkDeviceOrHostAddressKHR.calloc(stack).deviceAddress(bufferAddress)

  def deviceAddressConst(using stack: MemoryStack): VkDeviceOrHostAddressConstKHR =
    VkDeviceOrHostAddressConstKHR.calloc(stack).deviceAddress(bufferAddress)

  private def bufferAddress(using stack: MemoryStack): Long = {
    val addressInfo = VkBufferDeviceAddressInfo.calloc(stack).sType$Default.buffer(get)
    vkGetBufferDeviceAddressKHR(allocator.device.get, addressInfo)
  }

  protected def close(): Unit =
    vmaDestroyBuffer(allocator.get, handle, allocation)

}

object Buffer {
  def copyBuffer(src: ByteBuffer, dst: Buffer, bytes: Long): Unit =
    pushStack { stack =>
      val pData = stack.callocPointer(1)
      check(vmaMapMemory(dst.allocator.get, dst.allocation, pData), "Failed to map destination buffer memory")
      val data = pData.get()
      memCopy(memAddress(src), data, bytes)
      vmaFlushAllocation(dst.allocator.get, dst.allocation, 0, bytes)
      vmaUnmapMemory(dst.allocator.get, dst.allocation)
    }

  def copyBuffer(src: Buffer, dst: ByteBuffer, bytes: Long): Unit =
    pushStack { stack =>
      val pData = stack.callocPointer(1)
      check(vmaMapMemory(src.allocator.get, src.allocation, pData), "Failed to map destination buffer memory")
      val data = pData.get()
      memCopy(data, memAddress(dst), bytes)
      vmaUnmapMemory(src.allocator.get, src.allocation)
    }

  def copyBuffer(src: Buffer, dst: Buffer, bytes: Long, commandPool: CommandPool): Fence =
    pushStack { stack =>
      commandPool.singleTimeCommands { commandBuffer =>
        val copyRegion = VkBufferCopy
          .calloc(1, stack)
          .srcOffset(0)
          .dstOffset(0)
          .size(bytes)
        vkCmdCopyBuffer(commandBuffer, src.get, dst.get, copyRegion)
      }
    }

}
