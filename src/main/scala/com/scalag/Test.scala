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
  val dim = 2048
  val minRayHitTime = 0.01f
  val rayPosNormalNudge = 0.01f
  val superFar = 1000.0f
  val fovDeg = 60
  val fovRad = fovDeg * math.Pi.toFloat / 180.0f
  val maxBounces = 8
  val pixelIterationsPerFrame = 10000
  case class Random[T <: Value](value: T, nextSeed: UInt32)

  extension (v: Vec3[Float32])
    def mulV(v2: Vec3[Float32]): Vec3[Float32] = (v.x * v2.x, v.y * v2.y, v.z * v2.z)
    def addV(v2: Vec3[Float32]): Vec3[Float32] = (v.x + v2.x, v.y + v2.y, v.z + v2.z)

  def abs(v: Float32): Float32 = when(v < 0f)(-v).otherwise(v)

  case class RayHitInfo(
    dist: Float32,
    normal: Vec3[Float32],
    albedo: Vec3[Float32],
    emissive: Vec3[Float32]
  ) extends GStruct[RayHitInfo]

  case class Sphere(
    center: Vec3[Float32],
    radius: Float32,
    color: Vec3[Float32],
    emissive: Vec3[Float32]
  ) extends GStruct[Sphere]

  val function: GArray2DFunction[Vec4[Float32], Vec4[Float32]] = GArray2DFunction(dim, dim, {
    case ((xi: Int32, yi: Int32), _) =>
      def wangHash(seed: UInt32): UInt32 = {
        val s1 = (seed ^ 61) ^ (seed >> 16)
        val s2 = s1 * 9
        val s3 = s2 ^ (s2 >> 4)
        val s4 = s3 * 0x27d4eb2d
        s4 ^ (s4 >> 15)
      }

      def randomFloat(seed: UInt32): Random[Float32] = {
        val nextSeed = wangHash(seed)
        val f = nextSeed.asFloat / 4294967296.0f
        Random(f, nextSeed)
      }

      def randomVector(seed: UInt32): Random[Vec3[Float32]] = {
        val Random(z, seed1) = randomFloat(seed)
        val z2 = z * 2.0f - 1.0f
        val Random(a, seed2) = randomFloat(seed1)
        val a2 = a * 2.0f * math.Pi.toFloat
        val r = sqrt(1.0f - z2 * z2)
        val x = r * cos(a2)
        val y = r * sin(a2)
        Random((x, y, z2), seed2)
      }

      def normalize(v: Vec3[Float32]): Vec3[Float32] = {
        val len = sqrt(v dot v)
        v * (1.0f / len)
      }

      def scalarTriple(u: Vec3[Float32], v: Vec3[Float32], w: Vec3[Float32]): Float32 = (u cross v) dot w

      case class Quad(a: Vec3[Float32], b: Vec3[Float32], c: Vec3[Float32], d: Vec3[Float32]) extends GStruct[Quad]
      def testQuadTrace(
        rayPos: Vec3[Float32],
        rayDir: Vec3[Float32],
        currentHit: RayHitInfo,
        quad: Quad
      ): RayHitInfo =
        val normal = normalize((quad.c - quad.a) cross (quad.c - quad.b))
        val fixedQuad = when((normal dot rayDir) > 0f) {
          Quad(quad.d, quad.c, quad.b, quad.a)
        } otherwise {
          quad
        }
        val fixedNormal = when((normal dot rayDir) > 0f)(-normal).otherwise(normal)
        val p = rayPos
        val q = rayPos + rayDir
        val pq = q - p
        val pa = fixedQuad.a - p
        val pb = fixedQuad.b - p
        val pc = fixedQuad.c - p
        val m = pc cross pq
        val v = pa dot m
        
        def checkHit(intersectPoint: Vec3[Float32]): RayHitInfo =
          val dist = when(abs(rayDir.x) > 0.1f) {
            (intersectPoint.x - rayPos.x) / rayDir.x
          }.elseWhen(abs(rayDir.y) > 0.1f) {
            (intersectPoint.y - rayPos.y) / rayDir.y
          }.otherwise {
            (intersectPoint.z - rayPos.z) / rayDir.z
          }
          when(dist > minRayHitTime && dist < currentHit.dist) {
            RayHitInfo(dist, fixedNormal, (0.8f, 0.8f, 0.8f), (0f, 0f, 0f))
          } otherwise {
            currentHit
          }

        when(v >= 0f) {
          val u = -(pb dot m)
          val w = scalarTriple(pq, pb, pa)
          when(u >= 0f && w >= 0f) {
            val denom = 1f / (u + v + w)
            val uu = u * denom
            val vv = v * denom
            val ww = w * denom
            val intersectPos = fixedQuad.a * uu + fixedQuad.b * vv + fixedQuad.c * ww
            checkHit(intersectPos)
          } otherwise {
            currentHit
          }
        } otherwise {
          val pd = fixedQuad.d - p
          val u = pd dot m
          val w = scalarTriple(pq, pa, pd)
          when(u >= 0f && w >= 0f) {
            val negV = -v
            val denom = 1f / (u + negV + w)
            val uu = u * denom
            val vv = negV * denom
            val ww = w * denom
            val intersectPos = fixedQuad.a * uu + fixedQuad.d * vv + fixedQuad.c * ww
            checkHit(intersectPos)
          } otherwise {
            currentHit
          }
        }

      def testSphereTrace(
        rayPos: Vec3[Float32],
        rayDir: Vec3[Float32],
        currentHit: RayHitInfo,
        sphere: Sphere
      ): RayHitInfo =
        val toRay = rayPos - sphere.center
        val b = toRay dot rayDir
        val c = (toRay dot toRay) - (sphere.radius * sphere.radius)
        val notHit = currentHit
        when(c > 0f && b > 0f) {
          notHit
        } otherwise {
          val discr = b * b - c
          when(discr > 0f) {
            val initDist = -b - sqrt(discr)
            val fromInside = initDist < 0f
            val dist = when(fromInside)(-b + sqrt(discr)).otherwise(initDist)
            when (dist > minRayHitTime && dist < currentHit.dist) {
              val normal = normalize((rayPos + rayDir * dist - sphere.center) * (when(fromInside)(-1f).otherwise(1f)))
              RayHitInfo(dist, normal, sphere.color, sphere.emissive)
            } otherwise {
              notHit
            }
          } otherwise {
            notHit
          }
        }


      val sceneTranslation = vec4(0f,0f,10f,0f)

      def randomSphere: Sphere = {
        def nextFloatAny = scala.util.Random.nextFloat() * 2f - 1f
        def nextFloatPos = scala.util.Random.nextFloat()
        val center = (nextFloatAny * 10, nextFloatAny * 10, nextFloatPos * 10 + 15f)
        val radius = nextFloatPos * 2 + 1f
        val color = (nextFloatPos * 0.5f + 0.5f, nextFloatPos * 0.5f + 0.5f, nextFloatPos * 0.5f + 0.5f)

        val emissive = if (nextFloatPos > 0.5f) {
          (nextFloatPos * 5f, nextFloatPos * 5f, nextFloatPos * 5f) 
        } else {
          (0f, 0f, 0f)
        }
        Sphere(center, radius, color, emissive)
      }
      
      def randomSpheres(n: Int) = List.fill(n)(randomSphere)
      
      val spheres = randomSpheres(10).map(sp => sp.copy(center = sp.center + sceneTranslation.xyz))
        
      val walls = List(
        Quad(
          (-15.0f, -15.0f, 25.0f),
          (15.0f, -15.0f, 25.0f),
          (15.0f, 15.0f, 25.0f),
          (-15.0f, 15.0f, 25.0f)
        ),
        Quad(
          (15f, -15f, 25.0f),
          (15f, -15f, 15.0f),
          (15f,  15f, 15.0f),
          (15f,  15f, 25.0f)
        ),
        Quad(
          (-15f, -15f, 25.0f),
          (-15f, -15f, 15.0f),
          (-15f,  15f, 15.0f),
          (-15f,  15f, 25.0f)
        ),
        Quad(
          (-15f, 15f, 25.0f),
          ( 15f, 15f, 25.0f),
          ( 15f, 15f, 15.0f),
          (-15f, 15f, 15.0f)
        ),
        Quad(
          (-15f, -15f, 25.0f),
          ( 15f, -15f, 25.0f),
          ( 15f, -15f, 15.0f),
          (-15f, -15f, 15.0f)
        ),
      ).map(quad => quad.copy(a = quad.a + sceneTranslation.xyz, b = quad.b + sceneTranslation.xyz, c = quad.c + sceneTranslation.xyz, d = quad.d + sceneTranslation.xyz))

      def testScene(
        rayPos: Vec3[Float32],
        rayDir: Vec3[Float32],
        currentHit: RayHitInfo,
      ): RayHitInfo =
        val spheresHit = GSeq.of(spheres).fold(currentHit, {
          case (hit, sph) =>
            testSphereTrace(rayPos, rayDir, hit, sph)
        })
        val wallsHit = GSeq.of(walls).fold(spheresHit, { (hit, w) =>
          testQuadTrace(rayPos, rayDir, hit, w)
        })
        wallsHit

      case class RayTraceState(
        rayPos: Vec3[Float32],
        rayDir: Vec3[Float32],
        color: Vec3[Float32],
        throughput: Vec3[Float32],
        rngState: UInt32
      ) extends GStruct[RayTraceState]
      val MaxBounces = 8
      def getColorForRay(startRayPos: Vec3[Float32], startRayDir: Vec3[Float32], initRngState: UInt32): RayTraceState =
        val initState = RayTraceState(startRayPos, startRayDir, (0f, 0f, 0f), (1f, 1f, 1f), initRngState)
        GSeq.gen[RayTraceState](
          first = initState,
          next = { case state @ RayTraceState(rayPos, rayDir, color, throughput, rngState) =>
            val noHit = RayHitInfo(superFar, (0f, 0f, 0f), (0f, 0f, 0f), (0f, 0f, 0f))
            val testResult = testScene(rayPos, rayDir, noHit)
            when(testResult.dist < superFar) {
              val nextRayPos = (rayPos + rayDir * testResult.dist) + (testResult.normal * rayPosNormalNudge)
              val Random(randomVec, nextRngState) = randomVector(rngState)
              val nextRayDir = normalize(testResult.normal + randomVec)
              val nextColor = (throughput mulV testResult.emissive) addV color
              val nextThroughput = throughput mulV testResult.albedo
              RayTraceState(nextRayPos, nextRayDir, nextColor, nextThroughput, nextRngState)
            } otherwise {
              state
            }
          }
        ).limit(MaxBounces).lastOr(initState)

      val x = (xi.asFloat / dim.toFloat) * 2f - 1f
      val y = (yi.asFloat / dim.toFloat) * 2f - 1f
      val xy = (x, y)
      val rngState = xi * 1973 + yi * 9277 * 26699 | 1
      val rayPosition = (0f, 0f, 0f)
      val cameraDist = 1.0f / tan(fovDeg * 0.6f * math.Pi.toFloat / 180.0f)
      val rayTarget = (x, y, cameraDist)

      val rayDir = normalize(rayTarget - rayPosition)
      case class RenderIteration(color: Vec3[Float32], rngState: UInt32) extends GStruct[RenderIteration]
      val rayTraceResult =
        GSeq.gen(first = RenderIteration((0f,0f,0f), rngState.unsigned), next = {
          case RenderIteration(_, rngState) =>
            val rtResult = getColorForRay(rayPosition, rayDir, rngState)
            RenderIteration(rtResult.color, rtResult.rngState)
          }).limit(pixelIterationsPerFrame).fold((0f,0f,0f), _ addV _.color)
      val color = rayTraceResult * (1.0f / pixelIterationsPerFrame.toFloat)
      (color.r, color.g, color.b, 1.0f)
  })
  
  val r = Await.result(Vec4FloatMem(dim * dim).map(function), 10.hours)
  ImageUtility.renderToImage(r, dim, Paths.get("generated.png"))