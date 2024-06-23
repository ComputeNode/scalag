package com.scalag

import com.scalag.Algebra.FromExpr
import com.scalag.Expression.E
import izumi.reflect.Tag

sealed trait Value {
  def tree: E[_]
}

object Value {
  sealed trait Scalar extends Value

  trait FloatType extends Scalar
  case class Float32(tree: E[Float32]) extends FloatType
  given FromExpr[Float32] with
    def fromExpr(f: E[Float32]) = Float32(f)

  trait IntType extends Scalar
  case class Int32(tree: E[Int32]) extends IntType
  given FromExpr[Int32] with
    def fromExpr(f: E[Int32]) = Int32(f)
    
  case class GBoolean(tree: E[GBoolean]) extends Scalar
  given FromExpr[GBoolean] with
    def fromExpr(f: E[GBoolean]) = GBoolean(f)

  sealed trait Vec[T <: Value] extends Value

  case class Vec2[T <: Value](tree: E[Vec2[T]]) extends Vec[T]
  given [T <: Scalar]: FromExpr[Vec2[T]] with
    def fromExpr(f: E[Vec2[T]]) = Vec2(f)
    
  case class Vec3[T <: Value](tree: E[Vec3[T]]) extends Vec[T]
  given [T <: Scalar]: FromExpr[Vec3[T]] with
    def fromExpr(f: E[Vec3[T]]) = Vec3(f)

  case class Vec4[T <: Value](tree: E[Vec4[T]]) extends Vec[T]
  given [T <: Scalar]: FromExpr[Vec4[T]] with
    def fromExpr(f: E[Vec4[T]]) = Vec4(f)
}