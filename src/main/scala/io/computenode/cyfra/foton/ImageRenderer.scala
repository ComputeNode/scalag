package io.computenode.cyfra.foton

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{Await, ExecutionContext}
import io.computenode.cyfra.*
import io.computenode.cyfra.Algebra.*
import io.computenode.cyfra.Algebra.given

import scala.concurrent.duration.DurationInt
import io.computenode.cyfra.Functions.*
import io.computenode.cyfra.Control.*

import java.nio.file.{Path, Paths}
import scala.collection.mutable
import io.computenode.cyfra.given
import Renderer.RayHitInfo
import io.computenode.cyfra
import io.computenode.cyfra.Value.*
import io.computenode.cyfra.foton.ImageRenderer.RaytracingIteration
import io.computenode.cyfra.foton.shapes.{Box, Sphere}
import io.computenode.cyfra.{GArray2DFunction, GContext, GSeq, GStruct, ImageUtility, MVPContext, RGBA, UniformContext, Vec4FloatMem}
import io.computenode.cyfra.foton.utility.Color.*
import io.computenode.cyfra.foton.utility.Math3D.*
import io.computenode.cyfra.foton.utility.Random
import io.computenode.cyfra.foton.utility.Utility.timed

class ImageRenderer(params: ImageRenderer.Parameters) extends Renderer(params):

  def renderToFile(scene: Scene, destinationPath: Path): Unit =
    val images = render(scene)
    for image <- images do
      ImageUtility.renderToImage(image, params.width, params.height, destinationPath)

  def render(scene: Scene): LazyList[Array[RGBA]] =
    render(scene, renderFunction(scene))

  private def render(scene: Scene, fn: GArray2DFunction[RaytracingIteration, Vec4[Float32], Vec4[Float32]]): LazyList[Array[RGBA]] =
    val initialMem = Array.fill(params.width * params.height)((0.5f, 0.5f, 0.5f, 0.5f))
    LazyList.iterate((initialMem, 0), params.iterations + 1) { case (mem, render) =>
      UniformContext.withUniform(RaytracingIteration(render)):
        val fmem = Vec4FloatMem(mem)
        val result = timed(s"Rendered iteration $render")(Await.result(fmem.map(fn), 1.minute))
        (result, render + 1)
    }.drop(1).map(_._1)

  private def renderFunction(scene: Scene): GArray2DFunction[RaytracingIteration, Vec4[Float32], Vec4[Float32]] =
    GArray2DFunction(params.width, params.height, {
      case (RaytracingIteration(frame), (xi: Int32, yi: Int32), lastFrame) =>
        renderFrame(xi, yi, frame, lastFrame, scene)
    })

object ImageRenderer:

  private case class RaytracingIteration(frame: Int32) extends GStruct[RaytracingIteration]

  case class Parameters(
    width: Int,
    height: Int,
    fovDeg: Float = 60.0f,
    superFar: Float = 1000.0f,
    maxBounces: Int = 8,
    pixelIterations: Int = 1000,
    iterations: Int = 5,
    bgColor: (Float, Float, Float) = (0.2f, 0.2f, 0.2f)
  ) extends Renderer.Parameters