package com.scalag


import com.scalag.Expression.*
import com.scalag.Value.*

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{Await, ExecutionContext}
import Algebra.*
import Algebra.given

import scala.concurrent.duration.DurationInt
import com.scalag.api.*
import Functions.*
import Control.*

import java.nio.file.Paths
import scala.compiletime.error

given GContext = new MVPContext()
given ExecutionContext = Implicits.global

@main
def main =
  val dim = 4096
  val max = 1
  val RECURSION_LIMIT = 1000
  val const = (0.355f, 0.355f)
  case class Test(i: Int32, j: Int32) extends GStruct[Test]

  val function: GArray2DFunction[Vec4[Float32], Vec4[Float32]] = GArray2DFunction(dim, dim, {
    case ((xi: Int32, yi: Int32), _) =>
      val x = when(xi > 2000):
          Test(xi,yi)
        .otherwise:
          Test(yi,xi)

      (x.i.asFloat / dim.toFloat, x.j.asFloat / dim.toFloat, 0.5f, 1.0f)
  })

  val r = Await.result(Vec4FloatMem(dim * dim).map(function), 10.hours)
  val outputTemp = new File("HOW.png")
  ImageUtility.renderToImage(r, dim, outputTemp.toPath)


//  val dim = 2048
//  val minRayHitTime = 0.01f
//  val rayPosNormalNudge = 0.01f
//  val superFar = 1000.0f
//  val fovDeg = 90
//  val fovRad = fovDeg * math.Pi.toFloat / 180.0f
//  val numBounces = 8
//  val rendersPerFrame = 1
//  case class Random[T <: Value](value: T, nextSeed: UInt32)
//  case class RayHitInfo(
//    dist: Float32,
//    normal: Vec3[Float32],
//    albedo: Vec3[Float32],
//    emissive: Vec3[Float32]
//  )
//
//  val function: GArray2DFunction[Vec4[Float32], Vec4[Float32]] = GArray2DFunction(dim, dim, {
//    case ((xi: Int32, yi: Int32), _) =>
//      def wangHash(seed: UInt32): UInt32 = {
//        val s1 = (seed ^ 61) ^ (seed >> 16)
//        val s2 = s1 * 9
//        val s3 = s2 ^ (s2 >> 4)
//        val s4 = s3 * 0x27d4eb2d
//        s4 ^ (s4 >> 15)
//      }
//
//      def randomFloat(seed: UInt32): Random[Float32] = {
//        val nextSeed = wangHash(seed)
//        val f = nextSeed.asFloat / 4294967296.0f
//        Random(f, nextSeed)
//      }
//
//      def randomVector(seed: UInt32): Random[Vec3[Float32]] = {
//        val Random(z, seed1) = randomFloat(seed)
//        val z2 = z * 2.0f - 1.0f
//        val Random(a, seed2) = randomFloat(seed1)
//        val a2 = a * 2.0f * math.Pi.toFloat
//        val r = sqrt(1.0f - z2 * z2)
//        val x = r * cos(a2)
//        val y = r * sin(a2)
//        Random((x, y, z2), seed2)
//      }
//
//      def normalize(v: Vec3[Float32]): Vec3[Float32] = {
//        val len = sqrt(v dot v)
//        v * (1.0f / len)
//      }
//
//      def testSphereTrace(
//        rayPos: Vec3[Float32],
//        rayDir: Vec3[Float32],
//        currentHit: RayHitInfo,
//        sphere: Vec4[Float32],
//        sphereColor: Vec3[Float32]
//      ): RayHitInfo =
//        val toRay = rayPos - sphere.xyz
//        val b = toRay dot rayDir
//        val c = (toRay dot toRay) - (sphere.w * sphere.w)
//        val notHit = currentHit
//        when(c > 0f && b > 0f) {
//          notHit
//        } otherwise {
//          val discr = b * b - c
//          when(discr > 0f) {
//            val initDist = -b - sqrt(discr)
//            val (fromInside, dist) = when(initDist < 0f) {
//              (true, -b + sqrt(discr))
//            } otherwise {
//             initDist
//            }
//            when (dist > minRayHitTime && dist < currentHit.dist) {
//              val normal = normalize((rayPos + rayDir * dist - sphere.xyz) * (when(fromInside)(-1f).otherwise(1f)))
//              RayHitInfo(dist, normal, sphereColor, currentHit.emissive)
//            } otherwise {
//              notHit
//            }
//          } otherwise {
//            notHit
//          }
//        }
//
//      def testScene(
//        rayPos: Vec3[Float32],
//        rayDir: Vec3[Float32],
//        currentHit: RayHitInfo,
//      ): RayHitInfo =
//        val sceneTranslation = vec3(0f, 0f, 10f)
//        val sceneTranslation4 = vec4(0f,0f,10f,0f)
//        val sphere1 = ((-9f, -9.5f, 20f, 3.0f) + sceneTranslation4)
//        testSphereTrace(rayPos, rayDir, currentHit, sphere1, (0.8f, 0.2f, 0.2f))
//
//
//      def getColorForRay(startRayPos: Vec3[Float32], startRayDir: Vec3[Float32]): Vec3[Float32] =
//        val initRay = RayHitInfo(superFar, (0f, 0f, 0f), (0f, 0f, 0f), (0f, 0f, 0f))
//        testScene(startRayPos, startRayDir, initRay).albedo
//
//      val x = (xi.asFloat / dim.toFloat) * 2f - 1f
//      val y = (yi.asFloat / dim.toFloat) * 2f - 1f
//      val xy = (x, y)
//      val rngState = xi * 1973 + yi * 9277 * 26699 | 1
//      val rayPosition = (0f, 0f, 0f)
//      val cameraDist = 1.0f / tan(fovDeg * 0.6f * math.Pi.toFloat / 180.0f)
//      val rayTarget = (x, y, cameraDist)
//      // quad so no need for aspect ratio
//
//      val rayDir = normalize(rayTarget - rayPosition)
//      val r = getColorForRay(rayPosition, rayDir)
//      (r.x, r.y, r.z, 1.0f)
//  })
//
//  val r = Await.result(Vec4FloatMem(dim * dim).map(function), 10.hours)
//  ImageUtility.renderToImage(r, dim, Paths.get("generated.png"))