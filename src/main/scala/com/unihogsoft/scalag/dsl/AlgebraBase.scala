package com.unihogsoft.scalag.dsl

import com.unihogsoft.scalag.dsl.DSL._

import scala.reflect.ClassTag

trait AlgebraBase {
  case class Sum[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait Summable {
    self: ValType =>
    def +(other: self.Self)(implicit ct: ClassTag[self.Self]) = biCombine(Sum(_, _), other)
  }

  case class Diff[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait Diffable {
    self: ValType =>
    def -(other: self.Self)(implicit ct: ClassTag[self.Self]) = biCombine(Diff(_, _), other)
  }

  case class Div[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait Divable {
    self: ValType =>
    def /(other: self.Self)(implicit ct: ClassTag[self.Self]) = biCombine(Div(_, _), other)
  }

  case class Mul[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait Mulable {
    self: ValType =>
    def *(other: self.Self)(implicit ct: ClassTag[self.Self]) = biCombine(Mul(_, _), other)
  }
}
