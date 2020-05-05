package com.unihogsoft.scalag.dsl

import java.lang.invoke.{MethodHandle, MethodHandles, MethodType}

import com.unihogsoft.scalag.dsl.DSL._

import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime._
trait Array {


  val m = scala.reflect.runtime.currentMirror
  private def getExprConstr[H : TypeTag]: MethodHandle = { //todo find a better way, maybe some typeclass :)
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

