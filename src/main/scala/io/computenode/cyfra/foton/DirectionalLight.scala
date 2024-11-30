package io.computenode.cyfra.foton

import io.computenode.cyfra.Algebra.vec3
import io.computenode.cyfra.Value.*

case class DirectionalLight(
  emission: Vec3[Float32],
  direction: Vec3[Float32]
)
 
object DirectionalLight:
  val NoLight: DirectionalLight = DirectionalLight(vec3(0f), vec3(0f))