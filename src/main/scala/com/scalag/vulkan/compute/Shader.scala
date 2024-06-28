package com.scalag.vulkan.compute;

import com.scalag.vulkan.core.Device
import com.scalag.vulkan.utility.VulkanAssertionError
import com.scalag.vulkan.utility.VulkanObjectHandle
import org.joml.Vector3ic
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkShaderModuleCreateInfo

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.{ByteBuffer, LongBuffer}
import java.nio.channels.FileChannel
import java.util.List
import java.util.Objects
import java.util.stream.Collectors
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*

import scala.util.Using;

/** @author
  *   MarconZet Created 25.04.2020
  */
class Shader(shaderCode: ByteBuffer, val workgroupDimensions: Vector3ic, val layoutInfos: Seq[LayoutInfo], val functionName: String, device: Device)
    extends VulkanObjectHandle {

  protected val handle: Long = Using(stackPush()) { stack =>
    val shaderModuleCreateInfo = VkShaderModuleCreateInfo
      .callocStack()
      .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
      .pNext(0)
      .flags(0)
      .pCode(shaderCode);

    val pShaderModule = stack.callocLong(1);
    val err = vkCreateShaderModule(device.get, shaderModuleCreateInfo, null, pShaderModule);
    if (err != VK_SUCCESS)
      throw new VulkanAssertionError("Failed to create shader module", err);
    pShaderModule.get();
  }.get

  protected def close(): Unit =
    vkDestroyShaderModule(device.get, handle, null);

  def getLayoutsBySets: Seq[Seq[LayoutInfo]] = layoutInfos.map(_.set).distinct.sorted.map(getLayoutsBySet)

  private def getLayoutsBySet(a: Int): Seq[LayoutInfo] =
    layoutInfos.filter(_.set == a)
}

object Shader {

  def loadShader(path: String): ByteBuffer =
    loadShader(path, getClass.getClassLoader)

  def loadShader(path: String, classLoader: ClassLoader): ByteBuffer =
    try {
      val file = new File(Objects.requireNonNull(classLoader.getResource(path)).getFile);
      val fis = new FileInputStream(file);
      val fc = fis.getChannel;
      fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    } catch
      case e: IOException =>
        throw new RuntimeException(e);

}
