package io.computenode.cyfra.foton.rt.shapes

import io.computenode.cyfra.Algebra.{*, given}
import io.computenode.cyfra.Control.*
import io.computenode.cyfra.Functions.*
import io.computenode.cyfra.Value.*
import io.computenode.cyfra.{*, given}
import io.computenode.cyfra.GStruct
import io.computenode.cyfra.foton.rt.Material
import io.computenode.cyfra.foton.rt.RtRenderer.{MinRayHitTime, RayHitInfo}

import java.nio.file.Paths
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

case class Sphere(
  center: Vec3[Float32],
  radius: Float32,
  material: Material
) extends GStruct[Sphere] with Shape:
  def testRay(
    rayPos: Vec3[Float32],
    rayDir: Vec3[Float32],
    currentHit: RayHitInfo,
  ): RayHitInfo =
    val toRay = rayPos - center
    val b = toRay dot rayDir
    val c = (toRay dot toRay) - (radius * radius)
    val notHit = currentHit
    when(c > 0f && b > 0f) {
      notHit
    } otherwise {
      val discr = b * b - c
      when(discr > 0f) {
        val initDist = -b - sqrt(discr)
        val fromInside = initDist < 0f
        val dist = when(fromInside)(-b + sqrt(discr)).otherwise(initDist)
        when(dist > MinRayHitTime && dist < currentHit.dist) {
          val normal = normalize((rayPos + rayDir * dist - center) * (when(fromInside)(-1f).otherwise(1f)))
          RayHitInfo(dist, normal, material, fromInside)
        } otherwise {
          notHit
        }
      } otherwise {
        notHit
      }
    }