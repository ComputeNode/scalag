package com.unihogsoft.scalag

import com.unihogsoft.scalag.Test.function
import com.unihogsoft.scalag.api.{FloatMem, GArray2DFunction, GContext, GFunction, MVPContext}
import com.unihogsoft.scalag.dsl.DSL
import com.unihogsoft.scalag.dsl.DSL._
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps
import scala.reflect.runtime.universe.WeakTypeTag


object Test extends App {
  implicit val f32ct = implicitly[WeakTypeTag[Float32]]

  implicit val gcontext: GContext = new MVPContext()
  implicit val econtext: ExecutionContext = Implicits.global
  val dim = 512
  val max = 130000

  val function: GArray2DFunction[DSL.Float32, DSL.Float32] = GArray2DFunction(dim, dim, {
    case ((x: Int32, y: Int32), _) =>
      val xSquared = (x - 256) * (x - 256)
      val ySquared = (y - 256) * (y - 256)
      xSquared.asFloat + ySquared.asFloat
  })

  val data = (0 until dim*dim).map(_.toFloat).toArray
  val r = Await.result(FloatMem(data).map(function), 10 seconds)

  ImageDrawer.draw(r, dim, max)
}

object ImageDrawer {

  def draw(arr: Array[Float], n: Int, max: Float): Unit = {
    val byteArray = arr.map(i => interpolateColor(i.min(max), max))

    val image = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB)

    for (y <- 0 until n) {
      for (x <- 0 until n) {
        val pixelValue = byteArray(y * n + x)
        val rgb = pixelValue
        image.setRGB(x, y, rgb)
      }
    }

    val output = new File("output.png")
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