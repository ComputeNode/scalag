package com.scalag

import com.scalag.Expression.Const
import izumi.reflect.Tag
import com.scalag.Value
import com.scalag.Value.*
import Functions.*
trait Expression[T <: Value : Tag] extends Product {
  def tag: Tag[T] = summon[Tag[T]]
}

object Expression:
  type E[T <: Value] = Expression[T]

  sealed trait BinaryOpExpression[T <: Value: Tag] extends Expression[T] {
    def a: T
    def b: T
  }
  case class Sum[T <: Value : Tag](a: T, b: T) extends BinaryOpExpression[T]
  case class Diff[T <: Value : Tag](a: T, b: T) extends BinaryOpExpression[T]
  case class Mul[T <: Scalar : Tag](a: T, b: T) extends BinaryOpExpression[T]
  case class Div[T <: Scalar : Tag](a: T, b: T) extends BinaryOpExpression[T]
  case class Mod[T <: Scalar : Tag](a: T, b: T) extends BinaryOpExpression[T]
  case class ScalarProd[S <: Scalar, V <: Vec[S] : Tag](a: V, b: S) extends Expression[V]
  case class DotProd[S <: Scalar : Tag, V <: Vec[S]](a: V, b: V) extends Expression[S]
  case class CrossProd[V <: Vec[_] : Tag](a: V, b: V) extends Expression[V]

  case class ExtractScalar[V <: Vec[_] : Tag, S <: Scalar : Tag](a: V, i: Int32) extends Expression[S]

  sealed trait ConvertExpression[T <: Scalar : Tag] extends Expression[T]
  case class ToFloat32[T <: Scalar : Tag](a: T) extends ConvertExpression[Float32]
  case class ToInt32[T <: Scalar : Tag](a: T) extends ConvertExpression[Int32]
  
  sealed trait Const[T <: Scalar : Tag] extends Expression[T] {
    def value: Any
  }
  object Const {
    def unapply[T <: Scalar](c: Const[T]): Option[Any] = Some(c.value)
  }
  case class ConstFloat32(value: Float) extends Const[Float32]
  case class ConstInt32(value: Int) extends Const[Int32]

  case class ComposeVec2[T <: Scalar: Tag](a: T, b: T) extends Expression[Vec2[T]]
  case class ComposeVec3[T <: Scalar: Tag](a: T, b: T, c: T) extends Expression[Vec3[T]]
  case class FunctionCall[R <: Value : Tag](fn: FunctionSpec, args: List[Value]) extends Expression[R]

  case class Pass[T <: Value : Tag](value: T) extends E[T]

  case class Dynamic[T <: Value : Tag](source: String) extends E[T]
