package com.scalag.vulkan.executor;

import com.scalag.vulkan.VulkanContext
import com.scalag.vulkan.command.CommandPool
import com.scalag.vulkan.command.Fence
import com.scalag.vulkan.command.Queue
import com.scalag.vulkan.core.Device
import com.scalag.vulkan.memory.Allocator
import com.scalag.vulkan.memory.Buffer
import com.scalag.vulkan.memory.DescriptorPool
import com.scalag.vulkan.memory.DescriptorSet
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObject
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*

import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.List
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_UNKNOWN
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK10.VK_SUCCESS

import scala.util.Using;

abstract class AbstractExecutor(dataLength: Int, val bufferActions: Seq[BufferAction], context: VulkanContext) {
  protected val device: Device = context.device
  protected val queue: Queue = context.computeQueue
  protected val allocator: Allocator = context.allocator
  protected val descriptorPool: DescriptorPool = context.descriptorPool
  protected val commandPool: CommandPool = context.commandPool

  protected val (descriptorSets, buffers) = setupBuffers()
  protected def setupBuffers(): (Seq[DescriptorSet], Seq[Buffer])

  protected def recordCommandBuffer(commandBuffer: VkCommandBuffer): Unit
  private val commandBuffer: VkCommandBuffer =
    Using(stackPush()) { stack =>
      val commandBuffer = commandPool.createCommandBuffer();

      val commandBufferBeginInfo = VkCommandBufferBeginInfo
        .callocStack()
        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
        .flags(0);

      var err = vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);
      if (err != VK_SUCCESS)
        throw new VulkanAssertionError("Failed to begin recording command buffer", err);

      recordCommandBuffer(commandBuffer);

      err = vkEndCommandBuffer(commandBuffer);
      if (err != VK_SUCCESS)
        throw new VulkanAssertionError("Failed to finish recording command buffer", err);
      commandBuffer;
    }.get

  protected def getBiggestTransportData: Int

  def execute(input: Seq[ByteBuffer]): Seq[ByteBuffer] = {
    val stagingBuffer = new Buffer(
      getBiggestTransportData * dataLength,
      VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
      VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
      VMA_MEMORY_USAGE_UNKNOWN,
      allocator
    );
    for (i <- bufferActions.indices if bufferActions(i) == BufferAction.LOAD_INTO) do {
      val buffer = input(i)
      Buffer.copyBuffer(buffer, stagingBuffer, buffer.remaining());
      Buffer.copyBuffer(stagingBuffer, buffers(i), buffer.remaining(), commandPool).block().destroy();
    }

    Using(stackPush()) { stack =>
      val fence = new Fence(device);
      val pCommandBuffer = stack.callocPointer(1).put(0, commandBuffer);
      val submitInfo = VkSubmitInfo
        .callocStack()
        .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
        .pCommandBuffers(pCommandBuffer);

      val err = VK10.vkQueueSubmit(queue.get, submitInfo, fence.get);
      if (err != VK_SUCCESS)
        throw new VulkanAssertionError("Failed to submit command buffer to queue", err);
      fence.block().destroy();
    }

    val output = for (i <- bufferActions.indices if bufferActions(i) == BufferAction.LOAD_FROM) yield {
      val fence = Buffer.copyBuffer(buffers(i), stagingBuffer, buffers(i).size, commandPool);
      val outBuffer = BufferUtils.createByteBuffer(buffers(i).size);
      fence.block().destroy();
      Buffer.copyBuffer(stagingBuffer, outBuffer, outBuffer.remaining());
      outBuffer

    }
    stagingBuffer.destroy();
    output;
  }

  protected def createUpdatedDescriptorSet(descriptorSetLayout: Long, buffers: Seq[Buffer]): DescriptorSet = {
    val descriptorSet = new DescriptorSet(device, descriptorSetLayout, descriptorPool);
    val writeDescriptorSet = VkWriteDescriptorSet.callocStack(buffers.length);
    buffers.indices foreach { i =>
      val descriptorBufferInfo = VkDescriptorBufferInfo
        .callocStack(1)
        .buffer(buffers(i).get)
        .offset(0)
        .range(VK_WHOLE_SIZE);

      writeDescriptorSet
        .get(i)
        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
        .dstSet(descriptorSet.get)
        .dstBinding(i)
        .descriptorCount(1)
        .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
        .pBufferInfo(descriptorBufferInfo);
    }
    vkUpdateDescriptorSets(device.get, writeDescriptorSet, null);
    descriptorSet;
  }

  def destroy(): Unit = {
    commandPool.freeCommandBuffer(commandBuffer);
    descriptorSets.foreach(_.destroy());
    buffers.foreach(_.destroy());
  }
}
