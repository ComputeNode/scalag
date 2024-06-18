package com.scalag

import com.scalag.Expression.Const
import izumi.reflect.Tag
import com.scalag.Value
import com.scalag.Value.*

trait Expression[T <: Value : Tag] extends Product {
  def tag: Tag[T] = summon[Tag[T]]
}

object Expression:
  type E[T <: Value] = Expression[T]
  case class Sum[T <: Value : Tag](a: T, b: T) extends Expression[T]
  case class Diff[T <: Value : Tag](a: T, b: T) extends Expression[T]
  case class Mul[T <: Scalar : Tag](a: T, b: T) extends Expression[T]
  case class Div[T <: Scalar : Tag](a: T, b: T) extends Expression[T]
  case class Mod[T <: Scalar : Tag](a: T, b: T) extends Expression[T]
  case class ScalarProd[S <: Scalar, V <: Vec[S] : Tag](a: V, b: S) extends Expression[V]
  case class DotProd[S <: Scalar : Tag, V <: Vec[S]](a: V, b: V) extends Expression[S]
  case class CrossProd[V <: Vec[_] : Tag](a: V, b: V) extends Expression[V]
  
  case class ToFloat32[T <: Scalar : Tag](a: T) extends Expression[Float32]
  case class ToInt32[T <: Scalar : Tag](a: T) extends Expression[Int32]
  
  sealed trait Const[T <: Value : Tag] extends Expression[T] {
    def value: Any
  }
  object Const {
    def unapply[T <: Value](c: Const[T]): Option[Any] = Some(c.value)
  }
  case class ConstFloat32(value: Float) extends Const[Float32]
  case class ConstInt32(value: Int) extends Const[Int32]
  case class ConstVec2[T <: Scalar: Tag](value: (T, T)) extends Const[Vec2[T]]
  case class ConstVec3[T <: Scalar: Tag](value: (T, T, T)) extends Const[Vec3[T]]

  case class Dynamic[T <: Value : Tag](source: String) extends E[T]
