package com.unihogsoft.scalag.dsl

import DSL._

import scala.reflect.ClassTag

trait Vector {

  case class MulVecScalar[T <: ScalarType: ClassTag, R <: VectorType[T]](vec: E[R], scalar: E[T])
  // trait MulVecScalarable[R <: ScalarType] {
  //   self: VectorType[R] =>
  //   def *(other: self.Self)(implicit ct: ClassTag[self.Self]): Self = 
  // }

  case class DotProd[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait DotProdable {
    self: ValType =>
    def dot(other: self.Self)(implicit ct: ClassTag[self.Self]): Self = biCombine(DotProd(_, _), other)
  }

  case class CrossProd[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait CrossProdable {
    self: ValType =>
    def cross(other: self.Self)(implicit ct: ClassTag[self.Self]): Self = biCombine(CrossProd(_, _), other)
  }

  trait VectorType[T <: ScalarType] extends ValType with Summable with Diffable with DotProdable

  case class Vector2[T <: ScalarType](tree: E[Vector2[T]]) extends VectorType[T] {
    override type Self = Vector2[T]
    override val init = Vector2[T]
  }

  case class Vector3[T <: ScalarType](tree: E[Vector3[T]]) extends VectorType[T] {
    override type Self = Vector3[T]
    override val init = Vector3[T]
  }

  case class Vector4[T <: ScalarType](tree: E[Vector4[T]]) extends VectorType[T] {
    override type Self = Vector4[T]
    override val init = Vector4[T]
  }


}
