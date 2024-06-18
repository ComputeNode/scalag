package com.scalag.api


import com.scalag.Algebra.FromExpr
import com.scalag.{GArray2D, Value}
import com.scalag.Value.Int32
import com.scalag.vulkan.compute.ComputePipeline
import izumi.reflect.Tag

case class GArray2DFunction[H <: Value : Tag : FromExpr, R <: Value : Tag : FromExpr](
  width: Int,
  height: Int,
  fn: ((Int32, Int32), GArray2D[H]) => R
)(implicit context: GContext){
  def arrayInputs: List[Tag[_]] = List(summon[Tag[H]])
  def arrayOutputs: List[Tag[_]] = List(summon[Tag[R]])
  val pipeline: ComputePipeline = context.compile(this)
}
