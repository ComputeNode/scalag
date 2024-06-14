package com.unihogsoft.scalag.api

import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.vulkan.compute.ComputePipeline

import scala.reflect.runtime.universe._

case class GArray2DFunction[H <: ValType : WeakTypeTag, R <: ValType : WeakTypeTag](
  width: Int,
  height: Int,
  fn: ((Int32, Int32), GArray2D[H]) => R
)(implicit context: GContext){
  def arrayInputs: List[Type] = List(implicitly[WeakTypeTag[Int32]]).map(_.tpe.dealias)
  def arrayOutputs: List[Type] = List(implicitly[WeakTypeTag[R]]).map(_.tpe.dealias)
  val pipeline: ComputePipeline = context.compile(this)
}
