package com.scalag.vulkan.command

import com.scalag.vulkan.core.Device

/** @author
  *   MarconZet Created 13.04.2020 Copied from Wrap
  */
class StandardCommandPool(device: Device, queue: Queue) extends CommandPool(device, queue) {
  protected def getFlags: Int = 0
}
