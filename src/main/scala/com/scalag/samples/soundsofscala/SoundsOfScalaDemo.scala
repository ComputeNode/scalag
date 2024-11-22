package com.scalag.samples.soundsofscala

import com.scalag.{GArray2DFunction, GSeq}
import com.scalag.Value.{Float32, Int32, Vec4}
import com.scalag.Expression.*
import com.scalag.Value.*
import com.scalag.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{Await, ExecutionContext}
import Algebra.*
import Algebra.given
import com.scalag.given
import scala.concurrent.duration.DurationInt
import Functions.*
import Control.*
import java.nio.file.Paths
import scala.compiletime.error
import scala.collection.mutable

object SoundsOfScalaDemo:
  given GContext = new MVPContext()

  val dim = 728
  case class RenderParams(
    color: Vec4[Float32],
  ) extends GStruct[RenderParams]

  @main
  def main =
    val raytracing: GArray2DFunction[RenderParams, Vec4[Float32], Vec4[Float32]] = GArray2DFunction(dim, dim, {
      case (params, (xi: Int32, yi: Int32), _) =>
        params.color
    })

    val mem = Vec4FloatMem(Array.fill(dim * dim)((0f,0f,0f,0f)))
    UniformContext.withUniform(RenderParams((1f, 0f, 0f, 1f))):
      val result = Await.result(mem.map(raytracing), 1.second)
      ImageUtility.renderToImage(result, dim, Paths.get(s"generated2.png"))
    
