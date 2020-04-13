package com.unihogsoft.scalag.dsl

import scala.reflect.ClassTag

object DSL extends DSLStructure with Primitives with Scalar with Vector with AlgebraBase with App {

   val someVar: Float32 = Float32(Const[Float32, Float](4.0f))
   val someVar2: Float32 = Float32(Const[Float32, Float](6.0f))
  
   val someVarI: Int32 = Int32(Const[Int32, Int](2))
   val f = float32(3.0)
   val ops = 8 + ((1 + someVar * someVar2) / someVar) * 2.0f
  
   println(formatTree(ops.tree))

  val someVarV: Vector2[Float32] = Vector2[Float32](Const[Vector2[Float32], Float](4.0f))
  val someVarV2: Vector2[Float32] = Vector2[Float32](Const[Vector2[Float32], Float](4.0f))
  someVarV + someVarV2

  MulVecScalar(someVarV.tree, someVar.tree)
}
