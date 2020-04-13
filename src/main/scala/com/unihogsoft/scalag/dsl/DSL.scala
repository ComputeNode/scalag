package com.unihogsoft.scalag.dsl

import scala.reflect.ClassTag

object DSL extends App{
  trait ValType {
    type Self >: this.type <: ValType
    def tree: E[Self]
    def init: E[Self] => Self
    private def adapt[R <: ValType](e: E[R])= e.asInstanceOf[E[Self]]
    protected def biCombine[R <: ValType](f: (E[Self], E[Self]) => E[Self], e: R)(implicit ev: R =:= Self) = {
      init(f(tree, adapt(e.tree)))
    }
  }

  class Expression[T <: ValType : ClassTag] {
    val valClassTag: ClassTag[T] = implicitly[ClassTag[T]]
  }
  type E[T <: ValType] = Expression[T]

  case class Sum[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait Summable {
    self: ValType =>
    def +(other: self.Self)(implicit ct: ClassTag[self.Self]) = biCombine(Sum(_, _), other)  
  }

  case class Diff[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait Diffable {
    self: ValType =>
    def -(other: self.Self)(implicit ct: ClassTag[self.Self]) = biCombine(Diff(_, _), other)
  }

  case class Div[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait Divable {
    self: ValType =>
    def /(other: self.Self)(implicit ct: ClassTag[self.Self]) = biCombine(Div(_, _), other)
  }

  case class Mul[T <: ValType : ClassTag](a: E[T], b:  E[T]) extends E[T]
  trait Mulable {
    self: ValType =>
    def *(other: self.Self)(implicit ct: ClassTag[self.Self]) = biCombine(Mul(_, _), other)
  }

  trait FloatType extends ValType with Summable with Diffable with Divable with Mulable

  case class Float32(tree: E[Float32]) extends FloatType {
    override type Self = Float32
    override val init = Float32
  }

  case class Float64(tree: E[Float64]) extends FloatType {
    override type Self = Float64
    override val init = Float64
  }

  case class Const[T <: ValType : ClassTag, R](r: R) extends E[T]

  implicit def floatToFloat32(f: Float): Float32 = Float32(Const[Float32, Float](f))

  val someVar: Float32 = Float32(Const[Float32, Float](2.0f))
  val someVar2: Float32 = Float32(Const[Float32, Float](2.0f))

  val ops = someVar * 3.0f + someVar

  println(ops.tree)

}
