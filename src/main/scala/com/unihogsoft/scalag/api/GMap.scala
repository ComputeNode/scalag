package com.unihogsoft.scalag.api

import shapeless._
import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.dsl.Array
import poly._
import shapeless.ops.hlist.Mapper

import scala.reflect.runtime.universe.TypeTag

case class GMap[H <: ValType : TypeTag, R <: ValType : TypeTag](fn: (Int32, GArray[H]) => R)(implicit context: GContext) {
  def arrayInputs: List[TypeTag[_]] = List(implicitly[TypeTag[H]])
  def arrayOutputs: List[TypeTag[_]] = List(implicitly[TypeTag[R]])
  val executable: Executable[H, R] = context.compile(this)
}

