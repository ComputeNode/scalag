package com.unihogsoft.scalag.api

import shapeless._
import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.dsl.Array
import poly._
import shapeless.ops.hlist.Mapper

import scala.reflect.ClassTag



case class GMap[H <: ValType : ClassTag, R <: ValType : ClassTag](fn: (Int32, GArray[H]) => R)(implicit context: GContext) {
  val executable: Executable[H, R] = context.compile(this)
}

