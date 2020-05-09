package com.unihogsoft.scalag.api

import com.unihogsoft.scalag.dsl.DSL._
import scala.reflect.runtime.universe._

case class GMap[H <: ValType : TypeTag, R <: ValType : TypeTag](fn: (Int32, GArray[H]) => R)(implicit context: GContext) {
  def arrayInputs: List[Type] = List(implicitly[TypeTag[H]]).map(_.tpe.dealias)
  def arrayOutputs: List[Type] = List(implicitly[TypeTag[R]]).map(_.tpe.dealias)
  val executable: Executable[H, R] = context.compile(this)
}

