package io.computenode.cyfra.foton

import io.computenode.cyfra.foton.shapes.*
import io.computenode.cyfra.Algebra.{*, given}
import io.computenode.cyfra.Control.*
import io.computenode.cyfra.Value.*
import io.computenode.cyfra.*
import io.computenode.cyfra.given
import io.computenode.cyfra.Functions.*
import Renderer.RayHitInfo
import io.computenode.cyfra.foton.shapes.{Shape, ShapeCollection}
import izumi.reflect.Tag

import scala.util.chaining.*

case class Scene(
  shapes: List[Shape],
  camera: Camera
):

  private val shapesCollection: ShapeCollection = ShapeCollection(shapes)

  def rayTest(rayPos: Vec3[Float32], rayDir: Vec3[Float32], noHit: RayHitInfo): RayHitInfo =
    shapesCollection.testRay(rayPos, rayDir, noHit)

