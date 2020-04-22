package com.unihogsoft.scalag.dsl

import java.lang.invoke.{MethodHandle, MethodHandles, MethodType}

import com.unihogsoft.scalag.dsl.DSL._

import scala.reflect.ClassTag
import scala.reflect.runtime._
trait Array {


  val m = scala.reflect.runtime.currentMirror
  private def getExprConstr[H : ClassTag]: MethodHandle = { //todo find a better way, maybe some typeclass :)
    val ct = implicitly[ClassTag[H]]
    val constr = ct.runtimeClass.getConstructors.head
    println(ct.runtimeClass.getConstructors.mkString(", "))
    val lookup = MethodHandles.lookup()
    lookup.unreflectConstructor(constr)
  }

  case class GArray[H <: ValType : ClassTag](index: Int) {

    private val exprConstr: MethodHandle = getExprConstr

    def at(i: Int32): H = {
      exprConstr.invoke(new Scalar {}, GArrayElem[H](index, i))
    }
  }

  case class GArrayElem[T <: ValType : ClassTag](index: Int, i: Int32) extends Expression[T]
}

