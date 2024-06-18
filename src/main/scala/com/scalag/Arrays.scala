package com.scalag

import com.scalag.Value.*
import izumi.reflect.Tag
import Algebra.*
import Algebra.given

case class GArray[T <: Value : Tag : FromExpr](index: Int) {
  def at(i: Int32): T = {
    summon[FromExpr[T]].fromExpr(GArrayElem(index, i.tree))
  }
}

class GArray2D[T <: Value : Tag: FromExpr](width: Int, height: Int, val arr: GArray[T]) {
  def at(x: Int32, y: Int32): T = {
    arr.at(width * y + x)
  }
}

case class GArrayElem[T <: Value : Tag](index: Int, i: Expression[Int32]) extends Expression[T]

