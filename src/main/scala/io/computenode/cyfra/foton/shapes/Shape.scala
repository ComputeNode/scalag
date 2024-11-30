package io.computenode.cyfra.foton.shapes

import io.computenode.cyfra.Value.*
import io.computenode.cyfra.Algebra.{*, given}
import io.computenode.cyfra.GStruct
import io.computenode.cyfra.foton.Renderer.RayHitInfo

// type Shape = Box | Sphere | Quad

trait Shape private[shapes]():
  def testRay(
    rayPos: Vec3[Float32],
    rayDir: Vec3[Float32],
    currentHit: RayHitInfo,
  ): RayHitInfo
