package com.scalag.api

import com.scalag.Value
import com.scalag.Value.*

import java.nio.ByteBuffer
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil
import com.scalag.vulkan.executor.{AbstractExecutor, BufferAction, MapExecutor, SortByKeyExecutor}

import scala.concurrent.ExecutionContext.Implicits.*
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.language.postfixOps

trait GMem[H <: Value]:
  def size: Int
  def data: ByteBuffer

trait WritableGMem[T <: Value, R] extends GMem[T]:
  def stride: Int
  def data = MemoryUtil.memAlloc(size * stride)

  def write(index: Int, writeData: ByteBuffer): Unit = {
    data.rewind()
    data.put(writeData)
    data.rewind()
  }

  protected def toResultArray(buffer: ByteBuffer): Array[R]

  def execute(executor: AbstractExecutor): Future[Array[R]] = {
    Future {
      val out = executor.execute(List(data).asJava).asScala
      executor.destroy()
      toResultArray(out.head)
    }
  }

  def map(fn: GFunction[T, T])(implicit context: GContext): Future[Array[R]] = {
    val actions = List(new BufferAction(BufferAction.LOAD_INTO), new BufferAction(BufferAction.LOAD_FROM))
    execute(new MapExecutor(size, actions.asJava, fn.pipeline, context.vkContext))
  }

  def map(fn: GArray2DFunction[T, T])(implicit context: GContext): Future[Array[R]] = {
    val actions = List(new BufferAction(BufferAction.LOAD_INTO), new BufferAction(BufferAction.LOAD_FROM))
    execute(new MapExecutor(size, actions.asJava, fn.pipeline, context.vkContext))
  }

  def write(data: Array[R]): Unit

class FloatMem(val size: Int) extends WritableGMem[Float32, Float]:

  def stride: Int = 4

  override protected def toResultArray(buffer: ByteBuffer): Array[Float] = {
    val res = buffer.asFloatBuffer()
    val result = new Array[Float](size)
    res.get(result)
    result
  }

  def sort[R <: Value](fn: GFunction[Float32, Float32])(implicit context: GContext): Future[Array[Float]] = {
    execute(new SortByKeyExecutor(size, fn.pipeline, context.vkContext))
  }

  def write(floats: Array[Float]): Unit = {
    data.rewind()
    data.asFloatBuffer().put(floats)
    data.rewind()
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

type RGBA = (Float, Float, Float, Float)
class Vec4FloatMem(val size: Int) extends WritableGMem[Vec4[Float32], RGBA]:
  def stride: Int = 16

  override protected def toResultArray(buffer: ByteBuffer): Array[RGBA] = {
    val res = buffer.asFloatBuffer()
    val result = new Array[RGBA](size)
    for (i <- 0 until size) {
      result(i) = (res.get(), res.get(), res.get(), res.get())
    }
    result
  }

  def write(vecs: Array[RGBA]): Unit = {
    val floatBuffer = data.asFloatBuffer()
    data.rewind()
    vecs.foreach { case (x, y, z, a) =>
      floatBuffer.put(x)
      floatBuffer.put(y)
      floatBuffer.put(z)
      floatBuffer.put(a)
    }
    data.rewind()
  }

object Vec4FloatMem:
  def apply(vecs: Array[RGBA]): Vec4FloatMem = {
    val vec4FloatMem = new Vec4FloatMem(vecs.length)
    vec4FloatMem.write(vecs)
    vec4FloatMem
  }

  def apply(size: Int): Vec4FloatMem = {
    new Vec4FloatMem(size)
  }

