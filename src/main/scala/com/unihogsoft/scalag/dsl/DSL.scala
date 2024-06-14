package com.unihogsoft.scalag.dsl

import scala.reflect.ClassTag
object DSL extends DSLStructure with Primitives with Scalar with Vector with AlgebraBase with App with Array {
  case class ToInt32(a: E[Float32]) extends E[Int32]
  case class ToFloat32(a: E[Int32]) extends E[Float32]

  implicit class ConvertInt32(i32: Int32) {
    def asFloat: Float32 = Float32(ToFloat32(i32.tree))
  }

  implicit class ConvertFloat32(f32: Float32) {
    def asFloat: Int32 = Int32(ToInt32(f32.tree))
  }
}
