package com.unihogsoft.scalag

import com.unihogsoft.scalag.vulkan.VulkanContext
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class ContextTest extends AnyFlatSpec with Matchers {
  "Context" should "initialize without params" in {
    val context = new VulkanContext()
    context.destroy()
  }

  "Context" should "initialize with validation layer" in {
    val context = new VulkanContext(true)
    context.destroy()
  }
}
