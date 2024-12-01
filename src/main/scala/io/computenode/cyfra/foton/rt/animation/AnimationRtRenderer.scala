package io.computenode.cyfra.foton.rt.animation

import io.computenode.cyfra
import io.computenode.cyfra.Algebra.{*, given}
import io.computenode.cyfra.Value.*
import io.computenode.cyfra.foton.animation.AnimationRenderer
import io.computenode.cyfra.foton.rt.ImageRtRenderer.RaytracingIteration
import io.computenode.cyfra.foton.rt.animation.AnimationRtRenderer.RaytracingIteration
import io.computenode.cyfra.foton.rt.RtRenderer
import io.computenode.cyfra.foton.utility.Units.Milliseconds
import io.computenode.cyfra.foton.utility.Utility.timed
import io.computenode.cyfra.{*, given}

import java.nio.file.{Path, Paths}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AnimationRtRenderer(params: AnimationRtRenderer.Parameters) extends RtRenderer(params) with AnimationRenderer[AnimatedScene, AnimationRtRenderer.RenderFn](params):

  protected def renderFrame(
    scene: AnimatedScene,
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


  protected def renderFunction(scene: AnimatedScene): GArray2DFunction[RaytracingIteration, Vec4[Float32], Vec4[Float32]] =
    GArray2DFunction(params.width, params.height, {
      case (RaytracingIteration(frame, time), (xi: Int32, yi: Int32), lastFrame) =>
        renderFrame(xi, yi, frame, lastFrame, scene.at(time))
    })

object AnimationRtRenderer:
  
  type RenderFn = GArray2DFunction[RaytracingIteration, Vec4[Float32], Vec4[Float32]]
  case class RaytracingIteration(frame: Int32, time: Float32) extends GStruct[RaytracingIteration]

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
  ) extends RtRenderer.Parameters with AnimationRenderer.Parameters