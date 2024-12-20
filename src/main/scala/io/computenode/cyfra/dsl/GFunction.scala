package io.computenode.cyfra.dsl

import io.computenode.cyfra.dsl.Algebra.FromExpr
import io.computenode.cyfra.dsl.Value.*
import io.computenode.cyfra.vulkan.compute.{ComputePipeline, Shader}
import io.computenode.cyfra.*
import izumi.reflect.Tag

import scala.deriving.Mirror

case class GFunction[H <: Value : Tag: FromExpr, R <: Value : Tag : FromExpr](fn: H => R)(implicit context: GContext){
  def arrayInputs: List[Tag[_]] = List(summon[Tag[H]])
  def arrayOutputs: List[Tag[_]] = List(summon[Tag[R]])
  val pipeline: ComputePipeline = context.compile(this)
}

case class GArray2DFunction[G <: GStruct[G] : GStructSchema : Tag, H <: Value : Tag : FromExpr, R <: Value : Tag : FromExpr](
  width: Int,
  height: Int,
  fn: (G, (Int32, Int32), GArray2D[H]) => R
)(implicit context: GContext){
  def arrayInputs: List[Tag[_]] = List(summon[Tag[H]])
  def arrayOutputs: List[Tag[_]] = List(summon[Tag[R]])
  val pipeline: ComputePipeline = context.compile(this)
}