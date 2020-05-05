package com.unihogsoft.scalag.dsl

import com.unihogsoft.scalag.dsl.DSL._

import scala.reflect.runtime.universe.TypeTag

trait AlgebraBase {
  case class Sum[T <: ValType : TypeTag](a: E[T], b:  E[T]) extends E[T]
  trait Summable {
    self: ValType =>
    def +(other: self.Self)(implicit ct: TypeTag[self.Self]) = biCombine(Sum(_, _), other)
  }

  case class Diff[T <: ValType : TypeTag](a: E[T], b:  E[T]) extends E[T]
  trait Diffable {
    self: ValType =>
    def -(other: self.Self)(implicit ct: TypeTag[self.Self]) = biCombine(Diff(_, _), other)
  }

  case class Div[T <: ValType : TypeTag](a: E[T], b:  E[T]) extends E[T]
  trait Divable {
    self: ValType =>
    def /(other: self.Self)(implicit ct: TypeTag[self.Self]) = biCombine(Div(_, _), other)
  }

  case class Mul[T <: ValType : TypeTag](a: E[T], b:  E[T]) extends E[T]
  trait Mulable {
    self: ValType =>
    def *(other: self.Self)(implicit ct: TypeTag[self.Self]) = biCombine(Mul(_, _), other)
  }
}
