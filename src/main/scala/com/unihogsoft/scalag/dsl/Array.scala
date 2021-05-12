package com.unihogsoft.scalag.dsl

import com.unihogsoft.scalag.dsl.DSL._

import java.lang.invoke.{MethodHandle, MethodHandles}
import scala.reflect.runtime.universe.TypeTag

trait Array {
  val m = scala.reflect.runtime.currentMirror
  private def getExprConstr[H : TypeTag]: MethodHandle = {
    val ct = implicitly[TypeTag[H]]
    val mirror = ct.mirror
    val constr = mirror.runtimeClass(ct.tpe).getConstructors.head
    val lookup = MethodHandles.lookup()
    lookup.unreflectConstructor(constr)
  }

  case class GArray[H <: ValType : TypeTag](index: Int) {

    private val exprConstr: MethodHandle = getExprConstr

    def at(i: Int32): H = {
      exprConstr.invoke(new Scalar {}, GArrayElem[H](index, i.tree))
    }
  }

  case class GArrayElem[T <: ValType : TypeTag](index: Int, i: E[Int32]) extends Expression[T]
}

