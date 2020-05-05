package com.unihogsoft.scalag.dsl

import DSL._

import scala.reflect.runtime.universe._

trait Vector {

  case class MulVecScalar[T <: ScalarType, R <: VectorType[T] : TypeTag](vec: E[R], scalar: E[T]) extends Expression[R]
  trait MulVecScalarOp {
    self: VectorType[_ <: ScalarType] =>
    type Arb <: ScalarType
    def *(other: self.ElemType)(implicit ct: TypeTag[self.Self], ev: other.Self =:= self.ElemType): Self = {
      self.init(MulVecScalar(
        self.tree.asInstanceOf[E[VectorType[Arb]]],
        other.tree.asInstanceOf[E[Arb]]
      )(typeTag[self.Self].asInstanceOf[TypeTag[VectorType[Arb]]])
        .asInstanceOf[E[Self]]) // we don't lose type safety here, as it's guaranteed by the evidence - just going around the type system - may find a better way later
    }
  }

  case class DivVecScalar[T <: ScalarType, R <: VectorType[_] : TypeTag](vec: E[R], scalar: E[T]) extends Expression[R]
  trait DivVecScalarOp {
    self: VectorType[_ <: ScalarType] =>
    def /(other: self.ElemType)(implicit ct: TypeTag[self.Self], ev: other.Self =:= self.ElemType): Self =
      self.init(DivVecScalar(self.tree, other.tree.asInstanceOf[E[ScalarType]]))
  }

  case class DotProd[T <: ValType : TypeTag](a: E[T], b:  E[T]) extends E[T]
  trait DotProdable {
    self: ValType =>
    def dot(other: self.Self)(implicit ct: TypeTag[self.Self]): Self = biCombine(DotProd(_, _), other)
  }

  case class CrossProd[T <: ValType : TypeTag](a: E[T], b:  E[T]) extends E[T]
  trait CrossProdable {
    self: ValType =>
    def cross(other: self.Self)(implicit ct: TypeTag[self.Self]): Self = biCombine(CrossProd(_, _), other)
  }

  trait VectorType[T <: ScalarType] extends ValType with Summable with Diffable with DotProdable with MulVecScalarOp {
    type ElemType = T
    override type Self >: this.type <: VectorType[T]
  }

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
