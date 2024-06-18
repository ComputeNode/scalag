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

@main
def main =

  implicit val gcontext: GContext = new MVPContext()
  implicit val econtext: ExecutionContext = Implicits.global

  val dim = 2048
  val max = 1300000

  val function: GArray2DFunction[Float32, Float32] = GArray2DFunction(dim, dim, {
    case ((x: Int32, y: Int32), _) =>
      val xSquared = (x - (dim / 2)) * (x - (dim / 2))
      val ySquared = (y - (dim / 2)) * (y - (dim / 2))
      xSquared.asFloat + ySquared.asFloat
  })


  val data = (0 until dim * dim).map(_.toFloat).toArray
  val r = Await.result(FloatMem(data).map(function), 10.seconds)

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

    val c1 = (76, 3, 84)
    val c2 = (1, 162, 128)
    val c3 = (139, 219, 58)

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