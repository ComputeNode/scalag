package com.scalag.api
import com.scalag.Algebra.FromExpr
import com.scalag.*
import com.scalag.Value.*

import com.scalag.vulkan.compute.{ComputePipeline, Shader}
import izumi.reflect.Tag

import scala.deriving.Mirror

case class GFunction[H <: Value : Tag: FromExpr, R <: Value : Tag : FromExpr](fn: H => R)(implicit context: GContext){
  def arrayInputs: List[Tag[_]] = List(summon[Tag[H]])
  def arrayOutputs: List[Tag[_]] = List(summon[Tag[R]])
  val pipeline: ComputePipeline = context.compile(this)
}

case class GArray2DFunction[H <: Value : Tag : FromExpr, R <: Value : Tag : FromExpr](
  width: Int,
  height: Int,
  fn: ((Int32, Int32), GArray2D[H]) => R
)(implicit context: GContext){
  def arrayInputs: List[Tag[_]] = List(summon[Tag[H]])
  def arrayOutputs: List[Tag[_]] = List(summon[Tag[R]])
  val pipeline: ComputePipeline = context.compile(this)
}