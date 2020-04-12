package com.unihogsoft.scalag.dsl

import scala.reflect.ClassTag

object DSL {
  trait ValType
  trait ScalarType extends ValType
  trait Expression[+T]
  class Algebra[-T]
  trait SumAlgebra[-T] extends Algebra[T] {
    def sum[R<:T with Expression[_]](a: R, b: R): Sum[R] = Sum(a, b)

  }
  trait MulAlgebra[T] extends Algebra[T] {
    def mul(a: Expression[T], b: Expression[T]) = Mul(a, b)
  }
  trait DivAlgebra[T] extends Algebra[T]  {
    def div(a: Expression[T], b: Expression[T]) = Div(a, b)
  }

  case class Sum[T <: Expression[_]](a: T, b:  T) extends Expression[T]
  case class Diff[T](a:  Expression[T], b:  Expression[T]) extends Expression[T]
  case class Mul[T](a: Expression[T], b:  Expression[T]) extends Expression[T]
  case class Div[T](a:  Expression[T], b:  Expression[T]) extends Expression[T]

  trait FloatType extends ValType
  implicit val floatAlgebra: SumAlgebra[Expression[FloatType]] = new Algebra[Expression[FloatType]]  with SumAlgebra[Expression[FloatType]] 
  case class Float32(float: Float) extends FloatType

  case class Const[T <: ValType](t: T) extends Expression[T]

  implicit class SimpleArithmeticOps[T <: Expression[_]](expr: T)(implicit algebra: SumAlgebra[T]){
    def +(other: T) = algebra.sum(expr, other)
    def -(other: T) = Diff(expr, other)
  }

  implicit def floatToFloat32Const(f: Float): Expression[Float32] = Const(Float32(f))

  val x = Const(Float32(2)) + Const(Float32(2))
}
