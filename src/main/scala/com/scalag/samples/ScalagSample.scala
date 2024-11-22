package com.scalag.samples
import com.scalag.Value.*
import com.scalag.{FloatMem, GArray, GFunction, given}
import com.scalag.Algebra.*
import com.scalag.Algebra.given
import scala.concurrent.duration.given
import scala.concurrent.Await


@main
def sample =
  val gpuFunction = GFunction:
    (value: Float32) => value * 2f

  val data = FloatMem((1 to 128).map(_.toFloat).toArray)

  val result = Await.result(data.map(gpuFunction), 1.second)
  println(result.mkString(", "))
  
  
  