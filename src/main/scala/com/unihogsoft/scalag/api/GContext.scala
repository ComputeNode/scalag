package com.unihogsoft.scalag.api

import com.unihogsoft.scalag.compiler.DSLCompiler
import com.unihogsoft.scalag.dsl.DSL
import shapeless.HList
import shapeless.ops.hlist.{HKernelAux, Length}
import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.vulkan.{MapExecutor, VulkanContext}
import com.unihogsoft.scalag.vulkan.compute.MapPipeline

import scala.concurrent.Future
import scala.reflect.ClassTag

trait Executable[H <: HList, R <: HList] {
  def execute(input: GMem[H], output: WritableGMem[R]): Future[Unit]
}

trait GContext {
  def compile[H <: ValType, R <: ValType](map: GMap[H, R]): Executable[H, R]
}

class MVPContext extends GContext {

  val vkContext = new VulkanContext(true)
  val compiler: DSLCompiler = new DSLCompiler

  override def compile[H <: DSL.ValType : ClassTag, R <: DSL.ValType](map: GMap[H, R]): Executable[H, R] = {
    val shader = compiler.compile()
    val pipeline = new MapPipeline(shader, vkContext)
    val executor = new MapExecutor(bufferSize, bufferSize, bufferLength, pipeline, context)
  }

}
