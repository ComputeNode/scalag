package com.scalag.api
import com.scalag.Algebra.FromExpr
import com.scalag.Value
import com.scalag.vulkan.compute.{ComputePipeline, Shader}
import izumi.reflect.Tag

case class GFunction[H <: Value : Tag: FromExpr, R <: Value : Tag : FromExpr](fn: H => R)(implicit context: GContext){
  def arrayInputs: List[Tag[_]] = List(summon[Tag[H]])
  def arrayOutputs: List[Tag[_]] = List(summon[Tag[R]])
  val pipeline: ComputePipeline = context.compile(this)
}
