package com.scalag.vulkan.memory

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.util.Util.pushStack
import com.scalag.vulkan.util.VulkanObjectHandle
import org.joml.Matrix4x3f
import org.lwjgl.vulkan.KHRAccelerationStructure.*
import org.lwjgl.vulkan.*

class TopLevelAcceleratedStructure(as: Long, buffer: Buffer, device: Device) extends VulkanObjectHandle {
  override protected val handle: Long = as

  override protected def close(): Unit = {
    vkDestroyAccelerationStructureKHR(device.get, handle, null)
    buffer.destroy()
  }
}

object TopLevelAcceleratedStructure {
  def apply(blas: BottomLevelAcceleratedStructure, device: Device): TopLevelAcceleratedStructure = pushStack { stack =>
    val transformMatrix = VkTransformMatrixKHR.calloc(stack).matrix(new Matrix4x3f().getTransposed(stack.mallocFloat(12)))

    val asInstance = VkAccelerationStructureInstanceKHR
      .calloc(stack)
      .transform(transformMatrix)
      .instanceCustomIndex(0)
      .accelerationStructureReference(blas.deviceAddress)
      .mask(~0)
      .flags(VK_GEOMETRY_INSTANCE_TRIANGLE_FACING_CULL_DISABLE_BIT_KHR)
      .instanceShaderBindingTableRecordOffset(0)



    new TopLevelAcceleratedStructure(???, ???, device)
  }
}
