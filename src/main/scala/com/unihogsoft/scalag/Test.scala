package com.unihogsoft.scalag

import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.api.{FloatMem, GContext, GFunction, MVPContext}
import com.unihogsoft.scalag.dsl.{Array, DSL}
import shapeless._
import shapeless.ops.hlist._
import shapeless._
import shapeless.syntax.std.product._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits
import scala.language.postfixOps

object Test extends App {

  implicit val gcontext: GContext = new MVPContext()
  implicit val econtext: ExecutionContext = Implicits.global

  val addOne: GFunction[DSL.Float32, DSL.Float32] = GFunction {
    (x: Float32) =>
      val a = x + 1.0
      val b = x - 1.0 + 1000
      (a / 2) + ((b + 1) * 2)
  }

  val data = FloatMem(1 until 1024 map(_.toFloat) toArray)

  data.map(addOne).map(r => {
    println("Output!")
    println(r.getData().asFloatBuffer().array().mkString(", "))
  })

}
