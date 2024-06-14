package com.unihogsoft.scalag.dsl

import com.unihogsoft.scalag.dsl.DSL._

import scala.reflect.runtime.universe.WeakTypeTag

trait AlgebraBase {
  case class Sum[T <: ValType : WeakTypeTag](a: E[T], b:  E[T]) extends E[T]
  trait Summable {
    self: ValType =>
    def +(other: self.Self)(implicit ct: WeakTypeTag[self.Self]): Self = biCombine(Sum(_, _), other)
  }

  case class Diff[T <: ValType : WeakTypeTag](a: E[T], b:  E[T]) extends E[T]
  trait Diffable {
    self: ValType =>
    def -(other: self.Self)(implicit ct: WeakTypeTag[self.Self]): Self = biCombine(Diff(_, _), other)
  }

  case class Div[T <: ValType : WeakTypeTag](a: E[T], b:  E[T]) extends E[T]
  trait Divable {
    self: ValType =>
    def /(other: self.Self)(implicit ct: WeakTypeTag[self.Self]): Self = biCombine(Div(_, _), other)
  }

  case class Mul[T <: ValType : WeakTypeTag](a: E[T], b:  E[T]) extends E[T]
  trait Mulable {
    self: ValType =>
    def *(other: self.Self)(implicit ct: WeakTypeTag[self.Self]): Self = biCombine(Mul(_, _), other)
  }

  case class Mod[T <: ValType : WeakTypeTag](a: E[T], b:  E[T]) extends E[T]
  trait Modable {
    self: ValType =>
    def mod(other: self.Self)(implicit ct: WeakTypeTag[self.Self]): Self = biCombine(Mod(_, _), other)
  }


}
