package com.scalag

import com.scalag.Algebra.FromExpr
import com.scalag.Expression.*
import com.scalag.Value.*
import izumi.reflect.Tag

import scala.annotation.targetName
import scala.language.implicitConversions

object Algebra:

  trait FromExpr[T <: Value]:
    def fromExpr(expr: E[T]): T

  trait ScalarSummable[T <: Scalar: FromExpr: Tag]:
    def sum(a: T, b: T): T = summon[FromExpr[T]].fromExpr(Sum(a, b))
  extension[T <: Scalar: ScalarSummable : Tag](a: T)
    @targetName("add")
    def +(b: T): T = summon[ScalarSummable[T]].sum(a, b)

  trait ScalarDiffable[T <: Scalar: FromExpr: Tag]:
    def diff(a: T, b: T): T = summon[FromExpr[T]].fromExpr(Diff(a, b))
  extension[T <: Scalar: ScalarDiffable : Tag](a: T)
    @targetName("sub")
    def -(b: T): T = summon[ScalarDiffable[T]].diff(a, b)

  // T and S ??? so two
  trait ScalarMulable[T <: Scalar : FromExpr : Tag]:
    def mul(a: T, b: T): T = summon[FromExpr[T]].fromExpr(Mul(a, b))
  extension [T <: Scalar : ScalarMulable : Tag](a: T)
    @targetName("mul")
    def *(b: T): T = summon[ScalarMulable[T]].mul(a, b)

  trait ScalarDivable[T <: Scalar : FromExpr : Tag]:
    def div(a: T, b: T): T = summon[FromExpr[T]].fromExpr(Div(a, b))
  extension [T <: Scalar : ScalarDivable : Tag](a: T)
    @targetName("div")
    def /(b: T): T = summon[ScalarDivable[T]].div(a, b)
    
  trait ScalarModable[T <: Scalar : FromExpr : Tag]:
    def mod(a: T, b: T): T = summon[FromExpr[T]].fromExpr(Mod(a, b))
  extension [T <: Scalar : ScalarModable : Tag](a: T)
    def mod(b: T): T = summon[ScalarModable[T]].mod(a, b)

  trait BasicScalarAlgebra[T <: Scalar : FromExpr : Tag] 
    extends ScalarSummable[T]
      with ScalarDiffable[T] 
      with ScalarMulable[T] 
      with ScalarDivable[T]
      with ScalarModable[T] 
  
  extension (f32: Float32)
    def asInt: Int32 = Int32(ToInt32(f32))
  
  extension (i32: Int32)
    def asFloat: Float32 = Float32(ToFloat32(i32))
    

  trait VectorSummable[V <: Vec[_] : FromExpr : Tag]:
    def sum(a: V, b: V): V = summon[FromExpr[V]].fromExpr(Sum(a, b))
  extension[V <: Vec[_] : VectorSummable : Tag](a: V)
    @targetName("addVector")
    def +(b: V): V = summon[VectorSummable[V]].sum(a, b)

  trait VectorDiffable[V <: Vec[_] : FromExpr : Tag]:
    def diff(a: V, b: V): V = summon[FromExpr[V]].fromExpr(Diff(a, b))
  extension[V <: Vec[_] : VectorDiffable : Tag](a: V)
    @targetName("subVector")
    def -(b: V): V = summon[VectorDiffable[V]].diff(a, b)

  trait VectorDotable[S <: Scalar : FromExpr : Tag, V <: Vec[S] : Tag]:
    def dot(a: V, b: V): S = summon[FromExpr[S]].fromExpr(DotProd[S, V](a, b))
  extension[S <: Scalar : Tag, V <: Vec[S] : Tag](a: V)(using VectorDotable[S, V])
    def dot(b: V): S = summon[VectorDotable[S, V]].dot(a, b)

  trait VectorCrossable[V <: Vec[_] : FromExpr : Tag]:
    def cross(a: V, b: V): V = summon[FromExpr[V]].fromExpr(CrossProd(a, b))
  extension[V <: Vec[_] : VectorCrossable : Tag](a: V)
    def cross(b: V): V = summon[VectorCrossable[V]].cross(a, b)

  trait VectorScalarMulable[S <: Scalar : Tag, V <: Vec[S] : FromExpr : Tag]:
    def mul(a: V, b: S): V = summon[FromExpr[V]].fromExpr(ScalarProd[S, V](a, b))
  extension[S <: Scalar : Tag, V <: Vec[S] : Tag](a: V)(using VectorScalarMulable[S, V])
    def *(b: S): V = summon[VectorScalarMulable[S, V]].mul(a, b)

  trait BasicVectorAlgebra[S <: Scalar, V <: Vec[S] : FromExpr : Tag]
    extends VectorSummable[V]
      with VectorDiffable[V]
      with VectorDotable[S, V]
      with VectorCrossable[V]
      with VectorScalarMulable[S, V]

  given BasicScalarAlgebra[Float32] = new BasicScalarAlgebra[Float32] {}
  given BasicScalarAlgebra[Int32] = new BasicScalarAlgebra[Int32] {}

  given [T <: Scalar : FromExpr : Tag]: BasicVectorAlgebra[T, Vec2[T]] = new BasicVectorAlgebra[T, Vec2[T]] {}
  given [T <: Scalar : FromExpr : Tag]: BasicVectorAlgebra[T, Vec3[T]] = new BasicVectorAlgebra[T, Vec3[T]] {}
  
  given Conversion[Float, Float32] = f => Float32(ConstFloat32(f))
  given Conversion[Int, Int32] = i => Int32(ConstInt32(i))
  
  given Conversion[(Float, Float), Vec2[Float32]] = {
    case (x, y) => Vec2(ConstVec2((Float32(ConstFloat32(x)), Float32(ConstFloat32(y)))))
  }
  
  given Conversion[(Int, Int), Vec2[Int32]] = {
    case (x, y) => Vec2(ConstVec2((Int32(ConstInt32(x)), Int32(ConstInt32(y)))))
  }
  given Conversion[(Float, Float, Float), Vec3[Float32]] = {
    case (x, y, z) => Vec3(ConstVec3((Float32(ConstFloat32(x)), Float32(ConstFloat32(y)), Float32(ConstFloat32(z)))))
  }
  given Conversion[(Int, Int, Int), Vec3[Int32]] = {
    case (x, y, z) => Vec3(ConstVec3((Int32(ConstInt32(x)), Int32(ConstInt32(y)), Int32(ConstInt32(z)))))
  }
  