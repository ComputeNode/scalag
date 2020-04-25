package com.unihogsoft.scalag

import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.api.{FloatMem, GContext, GMap, MVPContext}
import com.unihogsoft.scalag.dsl.{Array, DSL}
import shapeless._
import shapeless.ops.hlist._
import shapeless._
import shapeless.syntax.std.product._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits

object Test extends App {

  implicit val gcontext: GContext = new MVPContext()
  implicit val econtext: ExecutionContext = Implicits.global

  val addOne: GMap[DSL.Float32, DSL.Float32] = GMap {
    (i: Int32, f: GArray[Float32]) =>
      val prev = f.at(i - 1) + 1
      val next = f.at(i + 1) + 1
      (prev / 2) + ((prev + 1) * 2) + next
  }

  val data = FloatMem(Array(1.0f, 2.0f, 3.0f))

  data.map(addOne).map(r => {
    println("Output!")
    println(r.getData(0).asFloatBuffer().array().mkString(", "))
  })

}
