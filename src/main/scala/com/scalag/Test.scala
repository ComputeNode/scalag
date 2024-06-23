package com.scalag


import com.scalag.Expression.*
import com.scalag.Value.*

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{Await, ExecutionContext}
import Algebra.*
import Algebra.given
import scala.concurrent.duration.DurationInt
import com.scalag.api.{FloatMem, GArray2DFunction, GContext, MVPContext}
import ImageDrawer.*
import Functions.*
import Control.*

@main
def main =

  implicit val gcontext: GContext = new MVPContext()
  implicit val econtext: ExecutionContext = Implicits.global

  val dim = 4096 * 2
  val max = 1
  val RECURSION_LIMIT = 1000
  val const = (0.355f, 0.355f)
  val function: GArray2DFunction[Float32, Float32] = GArray2DFunction(dim, dim, {
    case ((xi: Int32, yi: Int32), _) =>
      val x = 6.0f * (xi - (dim / 2)).asFloat / dim.toFloat
      val y = 6.0f * (yi - (dim / 2)).asFloat / dim.toFloat
      val uv = (x, y)
      val len = length((x,y)) * 1000f

      def juliaSet(uv: Vec2[Float32]): Int32 = {
        GSeq.gen(uv, next = v => {
          ((v.x * v.x) - (v.y * v.y), 2.0f * v.x * v.y) +  const
        }).limit(RECURSION_LIMIT).map(length).takeUntil(_ < 2.0f).count
      }

      def rotate(uv: Vec2[Float32], angle: Float32): Vec2[Float32] = {
        val newXAxis = (cos(angle), sin(angle))
        val newYAxis = (-newXAxis.y, newXAxis.x)
        (uv dot newXAxis, uv dot newYAxis) * 0.9f
      }

      val angle = Math.PI.toFloat / 3.0f
      val rotatedUv = rotate(uv, angle)

      val recursionCount = juliaSet(rotatedUv)

      val f = recursionCount.asFloat / 50f
      f
  })


  val data = (0 until dim * dim).map(_.toFloat).toArray
  val r = Await.result(FloatMem(data).map(function), 10.hours).map(f => Math.abs(f))

  ImageDrawer.draw(r.map(c => interpolateColor(c.min(max), max)), dim, "pow.png")


object ImageDrawer {

  def draw(arr: Array[Int], n: Int, name: String): Unit = {

    val image = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB)

    for (y <- 0 until n) {
      for (x <- 0 until n) {
        val pixelValue = arr(y * n + x)
        val rgb = pixelValue
        image.setRGB(x, y, rgb)
      }
    }

    val output = new File(name)
    ImageIO.write(image, "png", output)
    println(s"Image saved to ${output.getAbsolutePath}")
  }

  def interpolateColor(value: Float, maxX: Float): Int = {
    val scaledValue = value / maxX

    val c1 = (45, 15, 65)
    val c2 = (219, 80, 135)
    val c3 = (249, 205, 172)

    val interpolatedColor = interpolateRGB(c1, c2, c3, scaledValue)

    interpolatedColor
  }

  def interpolateRGB(color1: (Int, Int, Int), color2: (Int, Int, Int), color3: (Int, Int, Int), ratio: Float): Int = {
    val (r1, g1, b1) = color1
    val (r2, g2, b2) = color2
    val (r3, g3, b3) = color3

    val ratio1 = (1 - ratio) * (1 - ratio)
    val ratio2 = 2 * ratio * (1 - ratio)
    val ratio3 = ratio * ratio

    val rFinal = (r1 * ratio1 + r2 * ratio2 + r3 * ratio3).toInt & 0xFF
    val gFinal = (g1 * ratio1 + g2 * ratio2 + g3 * ratio3).toInt & 0xFF
    val bFinal = (b1 * ratio1 + b2 * ratio2 + b3 * ratio3).toInt & 0xFF

    (rFinal << 16) | (gFinal << 8) | bFinal
  }

}