package com.unihogsoft.scalag.dsl

import com.unihogsoft.scalag.dsl.DSL._

trait Primitives {

  trait Converter[T, R <: ScalarType] {
    def convert(t: T): R
  }

  implicit def intToDSLType[R <: ScalarType](f: Int)(implicit converter: Converter[Int, R]): R = converter.convert(f)
  implicit def floatToFloat32(f: Float): Float32 = Float32(Const[Float32, Float](f))
  implicit def floatToFloat64(f: Float): Float64 = Float64(Const[Float64, Float](f))
  implicit def doubleToFloat32(d: Double): Float32 = Float32(Const[Float32, Double](d))
  implicit def doubleToFloat64(d: Double): Float64 = Float64(Const[Float64, Double](d))
  implicit def intToFloat32(i: Int): Float32 = Float32(Const[Float32, Int](i))
  implicit def intToFloat64(i: Int): Float64 = Float64(Const[Float64, Int](i))
  implicit def intToInt32(i: Int): Int32 = Int32(Const[Int32, Int](i))
  implicit def intToInt64(i: Int): Int64 = Int64(Const[Int64, Int](i))


}