package com.scalag.samples.slides

import com.scalag.Algebra.{*, given}
import com.scalag.Value.*
import com.scalag.{FloatMem, GArray, GContext, GFunction, MVPContext, given}

import scala.concurrent.Await
import scala.concurrent.duration.given

given GContext = new MVPContext()

@main
def sample =
  val gpuFunction = GFunction:
    (value: Float32) => value * 2f

  val data = FloatMem((1 to 128).map(_.toFloat).toArray)

  val result = Await.result(data.map(gpuFunction), 1.second)
  println(result.mkString(", "))
  
  
  