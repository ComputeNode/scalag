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
    
  trait ScalarNegatable[T <: Scalar : FromExpr : Tag]:
    def negate(a: T): T = summon[FromExpr[T]].fromExpr(Negate(a))
  extension [T <: Scalar : ScalarNegatable : Tag](a: T)
    @targetName("negate")
    def unary_- : T = summon[ScalarNegatable[T]].negate(a)
    
  trait ScalarModable[T <: Scalar : FromExpr : Tag]:
    def mod(a: T, b: T): T = summon[FromExpr[T]].fromExpr(Mod(a, b))
  extension [T <: Scalar : ScalarModable : Tag](a: T)
    def mod(b: T): T = summon[ScalarModable[T]].mod(a, b)
    
  trait Comparable[T <: Scalar : FromExpr : Tag]:
    def greaterThan(a: T, b: T): GBoolean = GBoolean(GreaterThan(a, b))
    def lessThan(a: T, b: T): GBoolean = GBoolean(LessThan(a, b))
    def greaterThanEqual(a: T, b: T): GBoolean = GBoolean(GreaterThanEqual(a, b))
    def lessThanEqual(a: T, b: T): GBoolean = GBoolean(LessThanEqual(a, b))
    def equal(a: T, b: T): GBoolean = GBoolean(Equal(a, b))
  extension [T <: Scalar : Comparable : Tag](a: T)
    def >(b: T): GBoolean = summon[Comparable[T]].greaterThan(a, b)
    def <(b: T): GBoolean = summon[Comparable[T]].lessThan(a, b)
    def >=(b: T): GBoolean = summon[Comparable[T]].greaterThanEqual(a, b)
    def <=(b: T): GBoolean = summon[Comparable[T]].lessThanEqual(a, b)
    def ===(b: T): GBoolean = summon[Comparable[T]].equal(a, b)

  trait BasicScalarAlgebra[T <: Scalar : FromExpr : Tag] 
    extends ScalarSummable[T]
      with ScalarDiffable[T] 
      with ScalarMulable[T] 
      with ScalarDivable[T]
      with ScalarModable[T]
      with Comparable[T] 
      with ScalarNegatable[T]
  
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
    
  trait VectorNegatable[V <: Vec[_] : FromExpr : Tag]:
    def negate(a: V): V = summon[FromExpr[V]].fromExpr(Negate(a))
  extension[V <: Vec[_] : VectorNegatable : Tag](a: V)
    @targetName("negateVector")
    def unary_- : V = summon[VectorNegatable[V]].negate(a)

  trait BasicVectorAlgebra[S <: Scalar, V <: Vec[S] : FromExpr : Tag]
    extends VectorSummable[V]
      with VectorDiffable[V]
      with VectorDotable[S, V]
      with VectorCrossable[V]
      with VectorScalarMulable[S, V]
      with VectorNegatable[V]

  given BasicScalarAlgebra[Float32] = new BasicScalarAlgebra[Float32] {}
  given BasicScalarAlgebra[Int32] = new BasicScalarAlgebra[Int32] {}

  given [T <: Scalar : FromExpr : Tag]: BasicVectorAlgebra[T, Vec2[T]] = new BasicVectorAlgebra[T, Vec2[T]] {}
  given [T <: Scalar : FromExpr : Tag]: BasicVectorAlgebra[T, Vec3[T]] = new BasicVectorAlgebra[T, Vec3[T]] {}
  given [T <: Scalar : FromExpr : Tag]: BasicVectorAlgebra[T, Vec4[T]] = new BasicVectorAlgebra[T, Vec4[T]] {}
  
  given Conversion[Float, Float32] = f => Float32(ConstFloat32(f))
  given Conversion[Int, Int32] = i => Int32(ConstInt32(i))
  
  type FloatOrFloat32 = Float | Float32
  
  def toFloat32(f: FloatOrFloat32): Float32 = f match
    case f: Float => Float32(ConstFloat32(f))
    case f: Float32 => f
  given Conversion[(FloatOrFloat32, FloatOrFloat32), Vec2[Float32]] = {
    case (x, y) => Vec2(ComposeVec2(toFloat32(x), toFloat32(y)))
  }
  
  given Conversion[(Int, Int), Vec2[Int32]] = {
    case (x, y) => Vec2(ComposeVec2(Int32(ConstInt32(x)), Int32(ConstInt32(y))))
  }

  given Conversion[(Int32, Int32), Vec2[Int32]] = {
    case (x, y) => Vec2(ComposeVec2(x, y))
  }
  
  given Conversion[(Int32, Int32, Int32), Vec3[Int32]] = {
    case (x, y, z) => Vec3(ComposeVec3(x, y, z))
  }
  
  given Conversion[(FloatOrFloat32, FloatOrFloat32, FloatOrFloat32), Vec3[Float32]] = {
    case (x, y, z) => Vec3(ComposeVec3(toFloat32(x), toFloat32(y), toFloat32(z)))
  }
  
  given Conversion[(Int, Int, Int), Vec3[Int32]] = {
    case (x, y, z) => Vec3(ComposeVec3(Int32(ConstInt32(x)), Int32(ConstInt32(y)), Int32(ConstInt32(z))))
  }

  given Conversion[(Int32, Int32, Int32, Int32), Vec4[Int32]] = {
    case (x, y, z, w) => Vec4(ComposeVec4(x, y, z, w))
  }
  given Conversion[(FloatOrFloat32, FloatOrFloat32, FloatOrFloat32, FloatOrFloat32), Vec4[Float32]] = {
    case (x, y, z, w) => Vec4(ComposeVec4(toFloat32(x), toFloat32(y), toFloat32(z), toFloat32(w)))
  }
  
  extension [T <: Scalar: FromExpr: Tag] (v2: Vec2[T])
    def x: T = summon[FromExpr[T]].fromExpr(ExtractScalar(v2, Int32(ConstInt32(0))))
    def y: T = summon[FromExpr[T]].fromExpr(ExtractScalar(v2, Int32(ConstInt32(1))))
    
  extension [T <: Scalar: FromExpr: Tag] (v3: Vec3[T])
    def x: T = summon[FromExpr[T]].fromExpr(ExtractScalar(v3, Int32(ConstInt32(0))))
    def y: T = summon[FromExpr[T]].fromExpr(ExtractScalar(v3, Int32(ConstInt32(1))))
    def z: T = summon[FromExpr[T]].fromExpr(ExtractScalar(v3, Int32(ConstInt32(2))))
    def r: T = x
    def g: T = y
    def b: T = z

  extension [T <: Scalar: FromExpr: Tag] (v4: Vec4[T])
    def x: T = summon[FromExpr[T]].fromExpr(ExtractScalar(v4, Int32(ConstInt32(0))))
    def y: T = summon[FromExpr[T]].fromExpr(ExtractScalar(v4, Int32(ConstInt32(1))))
    def z: T = summon[FromExpr[T]].fromExpr(ExtractScalar(v4, Int32(ConstInt32(2))))
    def w: T = summon[FromExpr[T]].fromExpr(ExtractScalar(v4, Int32(ConstInt32(3))))
    def r: T = x
    def g: T = y
    def b: T = z
    def a: T = w


  extension (b: GBoolean)
    def &&(other: GBoolean): GBoolean = GBoolean(And(b, other))
    def ||(other: GBoolean): GBoolean = GBoolean(Or(b, other))
    def unary_! : GBoolean = GBoolean(Not(b))