package io.computenode.cyfra.foton

import io.computenode.cyfra.{UniformContext, Vec4FloatMem}
import io.computenode.cyfra.foton.utility.Utility.timed

import scala.concurrent.Await

class AnimationRenderer(params: Renderer.Parameters) extends Renderer(params)
