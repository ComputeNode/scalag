package com.scalag

import com.scalag.Algebra.{FromExpr, vec3}
import com.scalag.Expression.*
import com.scalag.Value.*
import izumi.reflect.Tag

import scala.annotation.targetName

sealed class FunctionName

object Functions:

  case object Sin extends FunctionName
  def sin(v: Float32): Float32 = Float32(FunctionCall(Sin, List(v)))

  case object Cos extends FunctionName
  def cos(v: Float32): Float32 = Float32(FunctionCall(Cos, List(v)))
  
  case object Tan extends FunctionName
  def tan(v: Float32): Float32 = Float32(FunctionCall(Tan, List(v)))
  
  case object Acos extends FunctionName
  def acos(v: Float32): Float32 = Float32(FunctionCall(Acos, List(v)))
  
  case object Asin extends FunctionName
  def asin(v: Float32): Float32 = Float32(FunctionCall(Asin, List(v)))
  
  case object Atan extends FunctionName
  def atan(v: Float32): Float32 = Float32(FunctionCall(Atan, List(v)))
  
  case object Atan2 extends FunctionName
  def atan2(y: Float32, x: Float32): Float32 = Float32(FunctionCall(Atan2, List(y, x)))

  case object Len2 extends FunctionName
  def length[T <: Scalar : Tag](v: Vec2[T]): Float32 = Float32(FunctionCall(Len2, List(v)))

  case object Len3 extends FunctionName
  def length[T <: Scalar : Tag](v: Vec3[T]): Float32 = Float32(FunctionCall(Len3, List(v)))

  case object Pow extends FunctionName
  def pow(v: Float32, p: Float32): Float32 = 
    Float32(FunctionCall(Pow, List(v, p)))
  def pow[V <: Vec[_] : Tag : FromExpr](v: V, p: V): V =
    summon[FromExpr[V]].fromExpr(FunctionCall(Pow, List(v, p)))
  
  case object Smoothstep extends FunctionName
  def smoothstep(edge0: Float32, edge1: Float32, x: Float32): Float32 = Float32(FunctionCall(Smoothstep, List(edge0, edge1, x)))
  
  case object Sqrt extends FunctionName
  def sqrt(v: Float32): Float32 = Float32(FunctionCall(Sqrt, List(v)))
  
  case object Cross extends FunctionName
  def cross[T <: Scalar : Tag](v1: Vec3[T], v2: Vec3[T]): Vec3[T] = Vec3(FunctionCall(Cross, List(v1, v2)))

  case object Clamp extends FunctionName
  def clamp(f: Float32, from: Float32, to: Float32): Float32 =
    Float32(FunctionCall(Clamp, List(f, from, to)))

  case object Exp extends FunctionName
  def exp(f: Float32): Float32 = Float32(FunctionCall(Exp, List(f)))
  def exp[V <: Vec[Float32] : Tag : FromExpr](v: V): V =
    summon[FromExpr[V]].fromExpr(FunctionCall(Exp, List(v)))

  case object Max extends FunctionName
  def max(f1: Float32, f2: Float32): Float32 = Float32(FunctionCall(Max, List(f1, f2)))
  def max[V <: Vec[Float32] : Tag : FromExpr](v1: V, v2: V): V =
    summon[FromExpr[V]].fromExpr(FunctionCall(Max, List(v1, v2)))
    
  // todo add F/U/S to all functions that need it
  case object Abs extends FunctionName
  def abs(f: Float32): Float32 = Float32(FunctionCall(Abs, List(f)))
  def abs[V <: Vec[Float32] : Tag : FromExpr](v: V): V =
    summon[FromExpr[V]].fromExpr(FunctionCall(Abs, List(v)))

  case object Mix extends FunctionName
  def mix[V <: Vec[Float32] : Tag : FromExpr](a: V, b: V, t: V) =
    summon[FromExpr[V]].fromExpr(FunctionCall(Mix, List(a, b, t)))
  def mix(a: Float32, b: Float32, t: Float32) = Float32(FunctionCall(Mix, List(a, b, t)))
  def mix[V <: Vec[Float32] : Tag : FromExpr](a: V, b: V, t: Float32) =
    summon[FromExpr[V]].fromExpr(FunctionCall(Mix, List(a, b, vec3(t))))
    
  case object Reflect extends FunctionName
  def reflect[I <: Vec[Float32] : Tag : FromExpr, N <: Vec[Float32] : Tag : FromExpr](I: I, N: N): I =
    summon[FromExpr[I]].fromExpr(FunctionCall(Reflect, List(I, N)))

  case object Refract extends FunctionName
  def refract[V <: Vec[Float32] : Tag : FromExpr](I: V, N: V, eta: Float32): V =
    summon[FromExpr[V]].fromExpr(FunctionCall(Refract, List(I, N, eta)))
