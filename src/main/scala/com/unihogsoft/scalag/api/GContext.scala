package com.unihogsoft.scalag.api

import java.util.concurrent.Executors

import com.unihogsoft.scalag.compiler.DSLCompiler
import com.unihogsoft.scalag.dsl.DSL
import shapeless.HList
import shapeless.ops.hlist.{HKernelAux, Length}
import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.vulkan.{MapExecutor, VulkanContext}
import com.unihogsoft.scalag.vulkan.compute.MapPipeline

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.TypeTag

trait Executable[H <: ValType, R <: ValType] {
  def execute(input: GMem[H], output: WritableGMem[R]): Future[Unit]
}

trait GContext {
  def compile[H <: ValType : TypeTag, R <: ValType : TypeTag](map: GMap[H, R]): Executable[H, R]
}

object WorkerIndex extends Int32(Dynamic("worker_index"))

class MVPContext extends GContext {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

  val vkContext = new VulkanContext(true)
  val compiler: DSLCompiler = new DSLCompiler

  override def compile[H <: DSL.ValType : TypeTag, R <: DSL.ValType : TypeTag](map: GMap[H, R]): Executable[H, R] = {
    val tree = map.fn.apply(WorkerIndex, GArray(0))
    val shader = compiler.compile(tree, map.arrayInputs, map.arrayOutputs)
//    val pipeline = new MapPipeline(shader, vkContext)
    (input: GMem[H], output: WritableGMem[R]) => Future {
//      val executor = new MapExecutor(input.getSize(), output.getSize(), output.getSize() / 128, pipeline, vkContext)
//      val result = executor.execute(input.getData(0))
//      output.write(0, result)
      ()
    }
  }


}
