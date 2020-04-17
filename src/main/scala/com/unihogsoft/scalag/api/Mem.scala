package com.unihogsoft.scalag.api

import java.nio.ByteBuffer

import org.lwjgl.BufferUtils

trait Mem {
  def getBytes(): Array[Byte]
}

case class DirectMem(data: Array[Byte]) extends Mem {
  override def getBytes(): Array[Byte] = data
}

// case class FloatMem(data: Array[Float]) extends DirectMem(BufferUtils)