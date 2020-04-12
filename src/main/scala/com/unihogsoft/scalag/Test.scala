package com.unihogsoft.scalag

import shapeless._

object Test {

//  val position: Mem[Vec2] = Mem.load(List(
//    Vec2(1,1),
//    Vec2(0,0),
//    Vec2(2,2)
//  ))
//
//  val velocity: Mem[Vec2] = Mem.load(List(
//    Vec2(1,0),
//    Vec2(0,-1),
//    Vec2(0,2)
//  ))
//
//  val mass: Mem[Float32] = Mem.load(List(
//    1,
//    1,
//    1
//  ))

  // trait Mem[T] {
  //   def at(index: Int): T
  // }

  // trait Vec2 {
  //   def dupa
  // }
  // trait Float32

  // trait GMap[T <: HList, R <: HList] {
  //   val f: (Int, T) => R
  // }


  // object GMap {
  //   def apply[T, R](fun: (Int, T) => R) = {
  //     new GMap[T, R] { val f = fun}
  //   }
  // }



//  val nBodyStep = GMap[Mem[Float] :: Mem[Float] :: HNil, Float :: HNil] {
//    case (i: Int, a :: b :: HNil) =>
//      val position = a.
//  }


}
