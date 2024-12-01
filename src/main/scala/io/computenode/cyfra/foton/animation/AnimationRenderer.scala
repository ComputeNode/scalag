package io.computenode.cyfra.foton.animation
import io.computenode.cyfra.given
import io.computenode.cyfra
import io.computenode.cyfra.Algebra.*
import io.computenode.cyfra.Algebra.given
import io.computenode.cyfra.Value.*
import io.computenode.cyfra.foton.Renderer
import io.computenode.cyfra.foton.animation.AnimationRenderer.RaytracingIteration
import io.computenode.cyfra.*
import io.computenode.cyfra.foton.ImageRenderer.RaytracingIteration
import io.computenode.cyfra.foton.utility.Units.Milliseconds
import io.computenode.cyfra.foton.utility.Utility.timed

import java.nio.file.{Path, Paths}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AnimationRenderer(params: AnimationRenderer.Parameters) extends Renderer(params):

  private val msPerFrame = 1000.0f / params.framesPerSecond

  def renderFramesToDir(scene: AnimatedScene[_], destinationPath: Path): Unit =
    destinationPath.toFile.mkdirs()
    val images = renderFrames(scene)
    val totalFrames = Math.ceil(scene.duration.toFloat / msPerFrame).toInt
    val requiredDigits = Math.ceil(Math.log10(totalFrames)).toInt
    for (image, i) <- images.zipWithIndex do
      val frameFormatted = i.toString.reverse.padTo(requiredDigits, '0').reverse.mkString
      val destionationFile = destinationPath.resolve(s"frame$frameFormatted.png")
      ImageUtility.renderToImage(image, params.width, params.height, destionationFile)

  def renderFrames(scene: AnimatedScene[_]): LazyList[Array[RGBA]] =
    val function = renderFunction(scene)
    val totalFrames = Math.ceil(scene.duration.toFloat / msPerFrame).toInt
    val timestamps = LazyList.range(0, totalFrames).map(_ * msPerFrame)
    timestamps.zipWithIndex.map {
      case (time, frame) =>
        timed(s"Animated frame ${frame}/${totalFrames}"):
          renderFrame(scene, time, function)
    }

  private def renderFrame(
    scene: AnimatedScene[_],
    time: Float32,
    fn: GArray2DFunction[RaytracingIteration, Vec4[Float32], Vec4[Float32]]
  ): Array[RGBA] =
    val initialMem = Array.fill(params.width * params.height)((0.5f, 0.5f, 0.5f, 0.5f))
    List.iterate((initialMem, 0), params.iterations + 1) { case (mem, render) =>
      UniformContext.withUniform(RaytracingIteration(render, time)):
        val fmem = Vec4FloatMem(mem)
        val result = Await.result(fmem.map(fn), 1.minute)
        (result, render + 1)
    }.map(_._1).last


  private def renderFunction(scene: AnimatedScene[_]): GArray2DFunction[RaytracingIteration, Vec4[Float32], Vec4[Float32]] =
    GArray2DFunction(params.width, params.height, {
      case (RaytracingIteration(frame, time), (xi: Int32, yi: Int32), lastFrame) =>
        renderFrame(xi, yi, frame, lastFrame, scene.at(time))
    })

object AnimationRenderer:
  private case class RaytracingIteration(frame: Int32, time: Float32) extends GStruct[RaytracingIteration]

  case class Parameters(
    width: Int,
    height: Int,
    fovDeg: Float = 60.0f,
    superFar: Float = 1000.0f,
    maxBounces: Int = 8,
    pixelIterations: Int = 1000,
    iterations: Int = 5,
    bgColor: (Float, Float, Float) = (0.2f, 0.2f, 0.2f),
    framesPerSecond: Int = 20
  ) extends Renderer.Parameters


// ffmpeg -framerate 20 -pattern_type sequence -start_number 10 -i frame%02d.png -s:v 800x800 -c:v libx264 -crf 17 -pix_fmt yuv420p output.mp4