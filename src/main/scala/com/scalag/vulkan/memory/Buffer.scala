package com.scalag.vulkan.memory;

import com.scalag.vulkan.command.Fence
import com.scalag.vulkan.command.CommandPool
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkCommandBuffer

import java.nio.ByteBuffer
import java.nio.LongBuffer
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.VK10.*

import scala.util.Using;

/** @author
  *   MarconZet Created 11.05.2019
  */
class Buffer(val size: Int, usage: Int, flags: Int, memUsage: Int, val allocator: Allocator) extends VulkanObjectHandle {

  val (handle, allocation) = Using(stackPush()) { stack =>
    val bufferInfo = VkBufferCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
      .pNext(NULL)
      .size(size)
      .usage(usage)
      .flags(0)
      .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

    val allocInfo = VmaAllocationCreateInfo
      .callocStack()
      .usage(memUsage)
      .requiredFlags(flags);

    val pBuffer = stack.callocLong(1);
    val pAllocation = stack.callocPointer(1);
    val err = vmaCreateBuffer(allocator.get, bufferInfo, allocInfo, pBuffer, pAllocation, null);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create buffer", err);
    (pBuffer.get(), pAllocation.get())
  }.get

  protected def close(): Unit =
    vmaDestroyBuffer(allocator.get, handle, allocation);

  def get(dst: Array[Byte]): Unit = {
    val len = Math.min(dst.length, size);
    val byteBuffer = memCalloc(len);
    Buffer.copyBuffer(this, byteBuffer, len);
    byteBuffer.get(dst);
    memFree(byteBuffer);
  }
}

object Buffer {
  def copyBuffer(src: ByteBuffer, dst: Buffer, bytes: Long): Unit =
    Using(stackPush()) { stack =>
      val pData = stack.callocPointer(1);
      val err = vmaMapMemory(dst.allocator.get, dst.allocation, pData);
      if (err != VK_SUCCESS)
        throw new VulkanAssertionError("Failed to map destination buffer memory", err);
      val data = pData.get();
      memCopy(memAddress(src), data, bytes);
      vmaFlushAllocation(dst.allocator.get, dst.allocation, 0, bytes);
      vmaUnmapMemory(dst.allocator.get, dst.allocation);
    }

  def copyBuffer(src: Buffer, dst: ByteBuffer, bytes: Long): Unit =
    Using(stackPush()) { stack =>
      val pData = stack.callocPointer(1);
      val err = vmaMapMemory(src.allocator.get, src.allocation, pData);
      if (err != VK_SUCCESS)
        throw new VulkanAssertionError("Failed to map destination buffer memory", err);
      val data = pData.get();
      memCopy(data, memAddress(dst), bytes);
      vmaUnmapMemory(src.allocator.get, src.allocation);
    }

  def copyBuffer(src: Buffer, dst: Buffer, bytes: Long, commandPool: CommandPool): Fence =
    Using(stackPush()) { stack =>
      val commandBuffer = commandPool.beginSingleTimeCommands();

      val copyRegion = VkBufferCopy
        .callocStack(1)
        .srcOffset(0)
        .dstOffset(0)
        .size(bytes);
      vkCmdCopyBuffer(commandBuffer, src.get, dst.get, copyRegion);

      commandPool.endSingleTimeCommands(commandBuffer);
    }.get

}
