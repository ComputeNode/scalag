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
import com.scalag.api.*
import ImageDrawer.*
import Functions.*
import Control.*

@main
def main =

  given GContext = new MVPContext()
  given ExecutionContext = Implicits.global

  val dim = 4096 * 2
  val max = 1
  val RECURSION_LIMIT = 1000
  val const = (0.355f, 0.355f)
  val function: GArray2DFunction[Vec4[Float32], Vec4[Float32]] = GArray2DFunction(dim, dim, {
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

      val f = recursionCount.asFloat / 100f
      (f,f,f,Float32(ConstFloat32(1.0f)))
  })

  val r = Await.result(Vec4FloatMem(dim*dim).map(function), 10.hours)

  ImageDrawer.draw(r, dim, "pow.png")


object ImageDrawer {

  def draw(arr: Array[(Float, Float, Float, Float)], n: Int, name: String): Unit = {

    val image = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB)

    for (y <- 0 until n) {
      for (x <- 0 until n) {
        val (r,g,b, _) = arr(y * n + x)
        val (iR, iG, iB) = ((r * 255).toInt, (g * 255).toInt, (b * 255).toInt)
        image.setRGB(x, y, (iR << 16) | (iG << 8) | iB)
      }
    }

    val output = new File(name)
    ImageIO.write(image, "png", output)
    println(s"Image saved to ${output.getAbsolutePath}")
  }

}