package com.scalag.vulkan.utility;

/** @author
  *   MarconZet Created 13.04.2020
  */
abstract class VulkanObjectHandle extends VulkanObject {
  protected var handle: Long = 0L;

  def get: Long =
    if (!alive)
      throw new IllegalStateException();
    else
      handle;
}
