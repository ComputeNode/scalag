package com.unihogsoft.scalag.dsl

import com.unihogsoft.scalag.dsl.DSL._
import scala.reflect.runtime.universe.TypeTag

trait Scalar {



  abstract class ScalarType extends ValType with Summable with Diffable with Divable with Mulable

  trait FloatType extends ScalarType
  trait IntegerType extends ScalarType

  case class Float32(tree: E[Float32]) extends FloatType {
    override type Self = Float32
    override val init = Float32
  }

  case class Float64(tree: E[Float64]) extends FloatType {
    override type Self = Float64
    override val init = Float64
  }

  case class Int32(tree: E[Int32]) extends IntegerType {
    override type Self = Int32
    override val init = Int32
  }

  case class Int64(tree: E[Int64]) extends IntegerType {
    override type Self = Int64
    override val init = Int64
  }
  
  def float32(f: Float32): Float32 = f
  def float64(f: Float64): Float64 = f
  def int32(f: Int32): Int32 = f
  def int64(f: Int64): Int64 = f

  case class Const[T <: ValType : TypeTag, R](r: R) extends E[T]
  case class Dynamic[T <: ValType : TypeTag, R](source: String) extends E[T]
}
