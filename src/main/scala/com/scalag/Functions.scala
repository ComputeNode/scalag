package com.scalag

import com.scalag.Expression.*
import com.scalag.Value.*
import izumi.reflect.Tag

import scala.annotation.targetName

sealed class FunctionSpec

object Functions:

  case object Sin extends FunctionSpec
  def sin(v: Float32): Float32 = Float32(FunctionCall(Sin, List(v)))

  case object Cos extends FunctionSpec
  def cos(v: Float32): Float32 = Float32(FunctionCall(Cos, List(v)))
  
  case object Tan extends FunctionSpec
  def tan(v: Float32): Float32 = Float32(FunctionCall(Tan, List(v)))

  case object Len2 extends FunctionSpec
  def length[T <: Scalar : Tag](v: Vec2[T]): Float32 = Float32(FunctionCall(Len2, List(v)))

  case object Len3 extends FunctionSpec
  def length[T <: Scalar : Tag](v: Vec3[T]): Float32 = Float32(FunctionCall(Len3, List(v)))

  case object PowF extends FunctionSpec
  def pow(v: Float32, p: Float32): Float32 = Float32(FunctionCall(PowF, List(v, p)))

  case object Smoothstep extends FunctionSpec
  def smoothstep(edge0: Float32, edge1: Float32, x: Float32): Float32 = Float32(FunctionCall(Smoothstep, List(edge0, edge1, x)))
  
  case object Sqrt extends FunctionSpec
  def sqrt(v: Float32): Float32 = Float32(FunctionCall(Sqrt, List(v)))
  
  case object Cross extends FunctionSpec
  def cross[T <: Scalar : Tag](v1: Vec3[T], v2: Vec3[T]): Vec3[T] = Vec3(FunctionCall(Cross, List(v1, v2)))



