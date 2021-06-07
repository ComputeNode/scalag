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
      x * x
  }

//  val input = 0 until 1024 map (_.toFloat) toArray
//
//  val r = Await.result(FloatMem(input).sort(function), 10 seconds)

  def time(fun: () => Unit) = {
    val start = System.currentTimeMillis()
    fun.apply()
    System.currentTimeMillis() - start
  }

  def pow(a: Int, b: Int) = 0 until b map(_ => a) product

  val datas = for {
    i <- 10 to 20
  } yield (0 until pow(2, i)).map(_.toFloat)

  datas.foreach { l =>
    val la = l.toArray
    val timesG = for {
      _ <- 0 until 100
    } yield time(() => Await.result(FloatMem(la).sort(function), 10 seconds)).toDouble

    val timesN = for {
      _ <- 0 until 100
    } yield time(() => l.sortBy(x => x * x)).toDouble

    println(s"${l.length}, ${timesG.sum / timesG.length}, ${timesN.sum / timesN.length}")

  }

//  println(r.mkString("Array(", ", ", ")"))
}
