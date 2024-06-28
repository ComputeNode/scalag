package com.scalag.vulkan.executor;

import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;

enum BufferAction(val action: Int):
  case DO_NOTHING extends BufferAction(0)
  case LOAD_INTO extends BufferAction(VK_BUFFER_USAGE_TRANSFER_DST_BIT)
  case LOAD_FROM extends BufferAction(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
