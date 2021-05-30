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

  val addOne: GFunction[DSL.Float32, DSL.Float32] = GFunction {
    (x: Float32) =>
      val a = x + 1.0
      a
  }

  val data = FloatMem(1 until 1024 map (_.toFloat) toArray)

  val fut = data.map(addOne)

  val r = Await.result(fut, 10 seconds)

  println("Output!")
  println(r.mkString("Array(", ", ", ")"))
}
