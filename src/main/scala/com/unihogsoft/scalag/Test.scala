package com.unihogsoft.scalag

import com.unihogsoft.scalag.api.{FloatMem, GContext, GFunction, MVPContext}
import com.unihogsoft.scalag.dsl.DSL
import com.unihogsoft.scalag.dsl.DSL._

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

object Test extends App {

  implicit val gcontext: GContext = new MVPContext()
  implicit val econtext: ExecutionContext = Implicits.global

  val function: GFunction[DSL.Float32, DSL.Float32] = GFunction {
    (x: Float32) =>
      0 - x
  }

  val input = 0 until 1024 map (_.toFloat) toArray

  val r = Await.result(FloatMem(input).sort(function), 10 seconds)

  println(r.mkString("Array(", ", ", ")"))
}
