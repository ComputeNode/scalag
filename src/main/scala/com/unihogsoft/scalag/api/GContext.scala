package com.unihogsoft.scalag.api

import better.files.File

import java.util.concurrent.Executors
import com.unihogsoft.scalag.compiler.DSLCompiler
import com.unihogsoft.scalag.dsl.DSL
import shapeless.HList
import shapeless.ops.hlist.{HKernelAux, Length}
import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.vulkan.VulkanContext
import com.unihogsoft.scalag.vulkan.compute.{ComputePipeline, LayoutInfo, Shader}
import com.unihogsoft.scalag.vulkan.executor.{MapExecutor, SortByKeyExecutor}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.language.postfixOps
import scala.reflect.runtime.universe.WeakTypeTag

trait Executable[H <: ValType, R <: ValType] {
  def execute(input: GMem[H], output: WritableGMem[R]): Future[Unit]
}

trait GContext {
  val vkContext = new VulkanContext(false)

  def compile[H <: ValType : WeakTypeTag, R <: ValType : WeakTypeTag](map: GFunction[H, R]): ComputePipeline
  def compile[H <: ValType : WeakTypeTag, R <: ValType : WeakTypeTag](map: GArray2DFunction[H, R]): ComputePipeline

}

object WorkerIndex extends Int32(Dynamic("worker_index"))

class MVPContext extends GContext {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))
  val compiler: DSLCompiler = new DSLCompiler

  override def compile[H <: DSL.ValType : WeakTypeTag, R <: DSL.ValType : WeakTypeTag](function: GFunction[H, R]): ComputePipeline = {
    val tree = function.fn.apply(GArray[H](0).at(WorkerIndex))
    val shaderCode = compiler.compile(tree, function.arrayInputs, function.arrayOutputs)

    val layoutInfos = 0 to 1 map (new LayoutInfo(0, _, 4)) toList
    val shader = new Shader(shaderCode, new org.joml.Vector3i(128, 1, 1), layoutInfos.asJava, "main", vkContext.getDevice)

    new ComputePipeline(shader, vkContext)
  }

  override def compile[H <: DSL.ValType : WeakTypeTag, R <: DSL.ValType : WeakTypeTag](function: GArray2DFunction[H, R]): ComputePipeline = {
    val tree = function.fn.apply((WorkerIndex mod function.width, WorkerIndex / function.width), new GArray2D(function.width, function.height, GArray[H](0)))
    val shaderCode = compiler.compile(tree, function.arrayInputs, function.arrayOutputs)

    val layoutInfos = 0 to 1 map (new LayoutInfo(0, _, 4)) toList
    val shader = new Shader(shaderCode, new org.joml.Vector3i(128, 1, 1), layoutInfos.asJava, "main", vkContext.getDevice)

    new ComputePipeline(shader, vkContext)
  }


}
