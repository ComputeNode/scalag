package com.unihogsoft.scalag.api

import java.nio.ByteBuffer
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil
import shapeless._
import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.vulkan.executor.{AbstractExecutor, BufferAction, MapExecutor, SortByKeyExecutor}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.language.postfixOps

trait GMem[H <: ValType] {
  def getSize: Int

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

  private def execute[R <: ValType](executor: AbstractExecutor): Future[FloatMem] = {
    Future {
      val out = executor.execute(List(data).asJava).asScala
      FloatMem(out.head.asFloatBuffer().array().array)
    }
  }

  def sort[R <: ValType](fn: GFunction[Float32, Float32])(implicit context: GContext): Future[FloatMem] = {
    execute(new SortByKeyExecutor(getSize(), fn.pipeline, context.vkContext))
  }

  def map[R <: ValType](fn: GFunction[Float32, Float32])(implicit context: GContext): Future[FloatMem] = {
    val actions = List(new BufferAction(BufferAction.LOAD_INTO), new BufferAction(BufferAction.LOAD_FROM))
    execute(new MapExecutor(getSize(), actions.asJava, fn.pipeline, context.vkContext))
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