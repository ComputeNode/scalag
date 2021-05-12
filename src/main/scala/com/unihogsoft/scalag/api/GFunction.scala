package com.unihogsoft.scalag.api

import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.vulkan.compute.{ComputePipeline, Shader}

import scala.reflect.runtime.universe._

case class GFunction[H <: ValType : TypeTag, R <: ValType : TypeTag](fn: H => R)(implicit context: GContext){
  def arrayInputs: List[Type] = List(implicitly[TypeTag[H]]).map(_.tpe.dealias)
  def arrayOutputs: List[Type] = List(implicitly[TypeTag[R]]).map(_.tpe.dealias)
  val pipeline: ComputePipeline = context.compile(this)
}
