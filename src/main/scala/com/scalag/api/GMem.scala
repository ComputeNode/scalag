package com.scalag.api

import com.scalag.Value
import com.scalag.Value.Float32

import java.nio.ByteBuffer
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil
import com.scalag.vulkan.executor.{AbstractExecutor, BufferAction, MapExecutor, SortByKeyExecutor}

import scala.concurrent.ExecutionContext.Implicits.*
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.language.postfixOps

trait GMem[H <: Value] {
  def getSize: Int

  def getData(): ByteBuffer
}

trait WritableGMem[T <: Value] extends GMem[T] {
  def getData(): ByteBuffer

  def write(index: Int, data: ByteBuffer): Unit
}

class FloatMem(size: Int) extends WritableGMem[Float32] {

  private val data = MemoryUtil.memAlloc(size * 4)

  override def getData(): ByteBuffer = data

  override def write(index: Int, writeData: ByteBuffer): Unit = {
    data.rewind()
    data.put(writeData)
    data.rewind()
  }

  override def getSize: Int = size

  private def execute[R <: Value](executor: AbstractExecutor): Future[Array[Float]] = {
    Future {
      val out = executor.execute(List(data).asJava).asScala
      val res = out.head.asFloatBuffer()
      executor.destroy()
      0 until size map res.get toArray
    }
  }

  def sort[R <: Value](fn: GFunction[Float32, Float32])(implicit context: GContext): Future[Array[Float]] = {
    execute(new SortByKeyExecutor(size, fn.pipeline, context.vkContext))
  }

  def map[R <: Value](fn: GFunction[Float32, Float32])(implicit context: GContext): Future[Array[Float]] = {
    val actions = List(new BufferAction(BufferAction.LOAD_INTO), new BufferAction(BufferAction.LOAD_FROM))
    execute(new MapExecutor(size, actions.asJava, fn.pipeline, context.vkContext))
  }

  def map[R <: Value](fn: GArray2DFunction[Float32, Float32])(implicit context: GContext): Future[Array[Float]] = {
    val actions = List(new BufferAction(BufferAction.LOAD_INTO), new BufferAction(BufferAction.LOAD_FROM))
    execute(new MapExecutor(size, actions.asJava, fn.pipeline, context.vkContext))
  }

  def write(floats: Array[Float]): Unit = {
    data.rewind()
    data.asFloatBuffer().put(floats)
    data.rewind()
  }
}

object FloatMem {
  def apply(floats: Array[Float]): FloatMem = {
    val floatMem = new FloatMem(floats.length)
    floatMem.write(floats)
    floatMem
  }

  def apply(size: Int): FloatMem = {
    new FloatMem(size)
  }
}