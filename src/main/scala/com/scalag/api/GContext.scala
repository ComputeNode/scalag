package com.scalag.api


import com.scalag.Algebra.FromExpr
import com.scalag.Algebra.given
import com.scalag.Expression.Dynamic
import com.scalag.Value
import com.scalag.Value.Int32
import com.scalag.*

import java.util.concurrent.Executors
import com.scalag.compiler.DSLCompiler
import izumi.reflect.Tag

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.language.postfixOps
import com.scalag.Algebra.*
import com.scalag.vulkan.compute.{ComputePipeline, LayoutInfo, Shader}
import vulkan.VulkanContext

import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.{Files, Paths}

trait Executable[H <: Value, R <: Value] {
  def execute(input: GMem[H], output: WritableGMem[R, _]): Future[Unit]
}

trait GContext {
  val vkContext = new VulkanContext()

  def compile[H <: Value : Tag : FromExpr, R <: Value : Tag](function: GFunction[H, R]): ComputePipeline
  def compile[H <: Value : Tag : FromExpr, R <: Value : Tag : FromExpr](function: GArray2DFunction[H, R]): ComputePipeline
}

val WorkerIndex: Int32 = Int32(Dynamic("worker_index"))

class MVPContext extends GContext {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

  override def compile[H <: Value : Tag : FromExpr, R <: Value : Tag](function: GFunction[H, R]): ComputePipeline = {
    val tree = function.fn.apply(GArray[H](0).at(WorkerIndex))
    val shaderCode = DSLCompiler.compile(tree, function.arrayInputs, function.arrayOutputs)

    val layoutInfos = 0 to 1 map ( LayoutInfo(0, _, DSLCompiler.typeStride(summon[Tag[H]]))) toList
    val shader = new Shader(shaderCode, new org.joml.Vector3i(128, 1, 1), layoutInfos, "main", vkContext.device)

    new ComputePipeline(shader, vkContext)
  }

  override def compile[H <: Value : Tag : FromExpr, R <: Value : Tag : FromExpr](function: GArray2DFunction[H, R]): ComputePipeline = {
    val tree = function.fn.apply((WorkerIndex mod function.width, WorkerIndex / function.width), new GArray2D(function.width, function.height, GArray[H](0)))
    val shaderCode = DSLCompiler.compile(tree, function.arrayInputs, function.arrayOutputs)
    // dump spv to file
    val fc: FileChannel = new FileOutputStream("program.spv").getChannel
    fc.write(shaderCode)
    fc.close()
    shaderCode.rewind()
    val layoutInfos = 0 to 1 map ( LayoutInfo(0, _, DSLCompiler.typeStride(summon[Tag[H]]))) toList
    val shader = new Shader(shaderCode, new org.joml.Vector3i(128, 1, 1), layoutInfos, "main", vkContext.device)

    new ComputePipeline(shader, vkContext)
  }


}
