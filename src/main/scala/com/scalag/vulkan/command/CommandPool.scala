package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*

import java.nio.LongBuffer
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*

import scala.util.Using;

/** @author
  *   MarconZet Created 13.04.2020 Copied from Wrap
  */
abstract class CommandPool(device: Device, queue: Queue) extends VulkanObjectHandle {
  protected val handle: Long = Using(stackPush()) { stack =>
    val createInfo = VkCommandPoolCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
      .pNext(VK_NULL_HANDLE)
      .queueFamilyIndex(queue.familyIndex)
      .flags(getFlags);

    val pCommandPoll = stack.callocLong(1);
    val err = vkCreateCommandPool(device.get, createInfo, null, pCommandPoll);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create command pool", err);
    pCommandPoll.get();
  }.get

  private val commandPool = handle

  def createCommandBuffers(n: Int): Seq[VkCommandBuffer] = Using(stackPush()) { stack =>
    val allocateInfo = VkCommandBufferAllocateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
      .commandPool(commandPool)
      .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
      .commandBufferCount(n);

    val pointerBuffer = stack.callocPointer(n);
    val err = vkAllocateCommandBuffers(device.get, allocateInfo, pointerBuffer);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to allocate command buffers", err);
    0 until n map (i => pointerBuffer.get(i)) map (new VkCommandBuffer(_, device.get))
  }.get

  def createCommandBuffer(): VkCommandBuffer =
    createCommandBuffers(1).head

  def beginSingleTimeCommands(): VkCommandBuffer =
    Using(stackPush()) { stack =>
      val commandBuffer = this.createCommandBuffer();

      val beginInfo = VkCommandBufferBeginInfo
        .callocStack()
        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
        .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

      val err = vkBeginCommandBuffer(commandBuffer, beginInfo);
      if (err != VK_SUCCESS)
        throw new VulkanAssertionError("Failed to begin single time command buffer", err);
      commandBuffer;
    }.get

  def endSingleTimeCommands(commandBuffer: VkCommandBuffer): Fence =
    Using(stackPush()) { stack =>
      vkEndCommandBuffer(commandBuffer);

      val pointerBuffer = stack.callocPointer(1).put(0, commandBuffer);
      val submitInfo = VkSubmitInfo
        .callocStack()
        .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
        .pCommandBuffers(pointerBuffer);

      val fence = new Fence(device, 0, () => freeCommandBuffer(commandBuffer));
      queue.submit(submitInfo, fence);
      fence
    }.get

  def freeCommandBuffer(commandBuffer: VkCommandBuffer*): Unit =
    Using(stackPush()) { stack =>
      val pointerBuffer = stack.callocPointer(commandBuffer.length);
      commandBuffer.foreach(pointerBuffer.put)
      pointerBuffer.flip();
      vkFreeCommandBuffers(device.get, commandPool, pointerBuffer);
    }

  override protected def close(): Unit =
    vkDestroyCommandPool(device.get, commandPool, null);

  protected def getFlags: Int
}
