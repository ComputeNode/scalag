package com.scalag.vulkan.command;

import com.scalag.vulkan.core.Device;

import org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;

/** @author
  *   MarconZet Created 13.04.2020 Copied from Wrap
  */
class OneTimeCommandPool(device: Device, queue: Queue) extends CommandPool(device, queue) {
  protected def getFlags: Int = VK_COMMAND_POOL_CREATE_TRANSIENT_BIT

}
