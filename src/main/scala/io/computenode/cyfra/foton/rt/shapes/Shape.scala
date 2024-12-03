package io.computenode.cyfra.foton.rt.shapes

import io.computenode.cyfra.dsl.Value.*
import io.computenode.cyfra.dsl.Algebra.{*, given}
import io.computenode.cyfra.dsl.GStruct
import io.computenode.cyfra.foton.rt.RtRenderer.RayHitInfo

// type Shape = Box | Sphere | Quad

trait Shape private[shapes]():
  def testRay(
    rayPos: Vec3[Float32],
    rayDir: Vec3[Float32],
    currentHit: RayHitInfo,
  ): RayHitInfo
