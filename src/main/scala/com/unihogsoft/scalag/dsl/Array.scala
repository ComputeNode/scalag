package com.unihogsoft.scalag.dsl

import com.unihogsoft.scalag.dsl.DSL._

import java.lang.invoke.{MethodHandle, MethodHandles}
import scala.reflect.runtime.universe.WeakTypeTag

trait Array {
  val m = scala.reflect.runtime.currentMirror
  private def getExprConstr[H : WeakTypeTag]: MethodHandle = {
    val ct = implicitly[WeakTypeTag[H]]
    val mirror = ct.mirror
    val constr = mirror.runtimeClass(ct.tpe).getConstructors.head
    val lookup = MethodHandles.lookup()
    lookup.unreflectConstructor(constr)
  }

  case class GArray[H <: ValType : WeakTypeTag](index: Int) {

    private val exprConstr: MethodHandle = getExprConstr

    def at(i: Int32): H = {
      exprConstr.invoke(new Scalar {}, GArrayElem[H](index, i.tree))
    }
  }

  class GArray2D[H <: ValType : WeakTypeTag](width: Int, height: Int, val arr: GArray[H]) {
    implicit val i32ct = implicitly[WeakTypeTag[Int32]]
    def at(x: Int32, y: Int32): H = {
      arr.at(width * y + x)
    }
  }


  case class GArrayElem[T <: ValType : WeakTypeTag](index: Int, i: E[Int32]) extends Expression[T]
}

