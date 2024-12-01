package io.computenode.cyfra.foton.rt.shapes

import io.computenode.cyfra.Algebra.{*, given}
import io.computenode.cyfra.Control.*
import io.computenode.cyfra.Value.*
import io.computenode.cyfra.*
import io.computenode.cyfra.given
import io.computenode.cyfra.Functions.*
import io.computenode.cyfra.GStruct
import io.computenode.cyfra.foton.rt.Material
import io.computenode.cyfra.foton.rt.RtRenderer.RayHitInfo

case class Plane(
  point: Vec3[Float32],
  normal: Vec3[Float32],
  material: Material
) extends GStruct[Plane] with Shape:
  def testRay(
    rayPos: Vec3[Float32],
    rayDir: Vec3[Float32],
    currentHit: RayHitInfo,
  ): RayHitInfo =
    val denom = normal dot rayDir
    given epsilon: Float32 = 0.1f
    when(denom =~= 0.0f) {
      currentHit
    } otherwise {
      val t = ((point - rayPos) dot normal) / denom
      when(t < 0.0f || t >= currentHit.dist) {
        currentHit
      } otherwise {
        val hitNormal = when(denom < 0.0f)(normal).otherwise(-normal)
        RayHitInfo(t, hitNormal, material)
      }
    }
