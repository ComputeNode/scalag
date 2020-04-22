package com.unihogsoft.scalag.api

import java.nio.ByteBuffer

import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil
import shapeless._
import com.unihogsoft.scalag.dsl.DSL._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

trait GMem[H <: ValType] {
  def getSize(): Int
  def getData(index: Int): ByteBuffer
}

trait WritableGMem[T <: ValType] extends GMem[T] {
  def getData(index: Int): ByteBuffer
  def write(index: Int, data: ByteBuffer): Unit
}

class FloatMem(size: Int) extends WritableGMem[Float32] {

  private val data = MemoryUtil.memAlloc(size * 4)

  override def getData(index: Int): ByteBuffer = index match {
    case 0 => data
    case _ => throw new IndexOutOfBoundsException()
  }

  override def write(index: Int, writeData: ByteBuffer): Unit = {
    data.rewind()
    data.put(writeData)
    data.rewind()
  }

  override def getSize(): Int = size

  def map[R <: ValType](fn: GMap[Float32, Float32]): Future[FloatMem] = {
    val resultMem = new FloatMem(this.size)
    val fut = fn.executable.execute(this, resultMem)
    fut.map(_ => resultMem)
  }

  def write(index: Int, floats: Array[Float]): Unit = {
    data.rewind()
    data.asFloatBuffer().put(floats)
    data.rewind()
  }
}

object FloatMem {
  def apply(floats: Array[Float]): FloatMem = {
    val floatMem = new FloatMem(floats.length)
    floatMem.write(0, floats)
    floatMem
  }
  def apply(size: Int): FloatMem = {
    new FloatMem(size)
  }
}