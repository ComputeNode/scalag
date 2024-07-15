package com.scalag.api

import com.scalag.*
import com.scalag.Algebra.*
import com.scalag.Algebra.given
import com.scalag.Expression.Dynamic
import com.scalag.Value.Int32
import com.scalag.compiler.DSLCompiler
import com.scalag.vulkan.VulkanContext
import com.scalag.vulkan.compute.*
import izumi.reflect.Tag

import java.io.{File, FileOutputStream}
import java.nio.channels.FileChannel
import java.nio.file.{Files, Paths}
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.language.postfixOps
trait Executable[H <: Value, R <: Value] {
  def execute(input: GMem[H], output: WritableGMem[R, _]): Future[Unit]
}

trait GContext {
  
  val vkContext = new VulkanContext()

  def compile[H <: Value: Tag: FromExpr, R <: Value: Tag](function: GFunction[H, R]): ComputePipeline
  def compile[H <: Value: Tag: FromExpr, R <: Value: Tag: FromExpr](function: GArray2DFunction[H, R]): ComputePipeline
}

val WorkerIndex: Int32 = Int32(Dynamic("worker_index"))

class MVPContext extends GContext {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

  override def compile[H <: Value: Tag: FromExpr, R <: Value: Tag](function: GFunction[H, R]): ComputePipeline = {
    val tree = function.fn.apply(GArray[H](0).at(WorkerIndex))
    val shaderCode = DSLCompiler.compile(tree, function.arrayInputs, function.arrayOutputs)

    val layoutInfo = LayoutInfo(Seq(LayoutSet(0, 0 to 1 map (Binding(_, InputBufferSize(DSLCompiler.typeStride(summon[Tag[H]])))))))
    val shader = new Shader(shaderCode, new org.joml.Vector3i(128, 1, 1), layoutInfo, "main", vkContext.device)

    new ComputePipeline(shader, vkContext)
  }

  override def compile[H <: Value : Tag : FromExpr, R <: Value : Tag : FromExpr](function: GArray2DFunction[H, R]): ComputePipeline = {
    val tree = function.fn.apply((WorkerIndex mod function.width, WorkerIndex / function.width), new GArray2D(function.width, function.height, GArray[H](0)))
 
    val file = new File("output.txt")
    val writer = new FileOutputStream(file, true)
    def appendToFile(s: String): Unit = {
      writer.write((s + "\n").getBytes)
      writer.flush()
    }
    def printTree(product: Product) = {
      val indent = 2
      def printTreeRec(product: Product, indent: Int): Unit =
        product.productIterator.foreach {
          case p: Product =>
            appendToFile(" " * indent + p.productPrefix)
            printTreeRec(p, indent + 2)
          case x => appendToFile(" " * indent + x)
        }
      printTreeRec(product, indent)
    }
    println("Printing tree!")
    // printTree(tree.asInstanceOf[Product])

    val shaderCode = DSLCompiler.compile(tree, function.arrayInputs, function.arrayOutputs)
    println("Compiled!")
    // dump spv to file
    val fc: FileChannel = new FileOutputStream("program.spv").getChannel
    fc.write(shaderCode)
    fc.close()
    shaderCode.rewind()
    val layoutInfo = LayoutInfo(Seq(LayoutSet(0, 0 to 1 map (Binding(_, InputBufferSize(DSLCompiler.typeStride(summon[Tag[H]])))))))
    val shader = new Shader(shaderCode, new org.joml.Vector3i(128, 1, 1), layoutInfo, "main", vkContext.device)

    new ComputePipeline(shader, vkContext)
  }

}
