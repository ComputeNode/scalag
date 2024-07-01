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
  val pixelIterationsPerFrame = 1000
  val bgColor = (0.2f, 0.2f, 0.2f)
  val exposure = 0.5f
  case class Random[T <: Value](value: T, nextSeed: UInt32)

  def lessThan(f: Vec3[Float32], f2: Float32): Vec3[Float32] =
    (when(f.x < f2)(1.0f).otherwise(0.0f),
      when(f.y < f2)(1.0f).otherwise(0.0f),
      when(f.z < f2)(1.0f).otherwise(0.0f))

  def linearToSRGB(rgb: Vec3[Float32]): Vec3[Float32] = {
    val clampedRgb = vclamp(rgb, 0.0f, 1.0f)
    mix(
      pow(clampedRgb, vec3(1.0f / 2.4f)) * 1.055f - vec3(0.055f),
      clampedRgb * 12.92f,
      lessThan(clampedRgb, 0.0031308f)
    )
  }

  def SRGBToLinear(rgb: Vec3[Float32]): Vec3[Float32] = {
    val clampedRgb = vclamp(rgb, 0.0f, 1.0f)
    mix(
      pow((clampedRgb + vec3(0.055f)) * (1.0f / 1.055f), vec3(2.4f)),
      clampedRgb * (1.0f / 12.92f),
      lessThan(clampedRgb, 0.04045f)
    )
  }

  def ACESFilm(x: Vec3[Float32]): Vec3[Float32] =
    val a = 2.51f
    val b = 0.03f
    val c = 2.43f
    val d = 0.59f
    val e = 0.14f
    vclamp((x mulV (x * a + vec3(b))) divV (x mulV (x * c + vec3(d)) + vec3(e)), 0.0f, 1.0f)

  case class RayHitInfo(
    dist: Float32,
    normal: Vec3[Float32],
    albedo: Vec3[Float32],
    emissive: Vec3[Float32],
    percentSpecular: Float32 = 0f,
    roughness: Float32 = 0f,
    specularColor: Vec3[Float32] = vec3(0f),
    indexOfRefraction: Float32 = 1.0f,
    refractionChance: Float32 = 0f,
    refractionRoughness: Float32 = 0f,
    refractionColor: Vec3[Float32] = vec3(0f),
    fromInside: GBoolean = false
  ) extends GStruct[RayHitInfo]

  case class Sphere(
    center: Vec3[Float32],
    radius: Float32,
    color: Vec3[Float32],
    emissive: Vec3[Float32],
    percentSpecular: Float32 = 0f,
    roughness: Float32 = 0f,
    specularColor: Vec3[Float32] = vec3(0f),
    indexOfRefraction: Float32 = 1f,
    refractionChance: Float32 = 0f,
    refractionRoughness: Float32 = 0f,
    refractionColor: Vec3[Float32] = vec3(0f),
  ) extends GStruct[Sphere]

  case class Quad(
    a: Vec3[Float32],
    b: Vec3[Float32], 
    c: Vec3[Float32], 
    d: Vec3[Float32],
    color: Vec3[Float32],
    emissive: Vec3[Float32]
  ) extends GStruct[Quad]

  case class RayTraceState(
    rayPos: Vec3[Float32],
    rayDir: Vec3[Float32],
    color: Vec3[Float32],
    throughput: Vec3[Float32],
    rngState: UInt32,
    finished: GBoolean = false
  ) extends GStruct[RayTraceState]

  val sceneTranslation = vec4(0f, 0f, 10f, 0f)
  val rd = scala.util.Random(1)
  
  def randomSphere: Sphere = {
    def nextFloatAny = rd.nextFloat() * 2f - 1f

    def nextFloatPos = rd.nextFloat()

    val center = (nextFloatAny * 10, nextFloatAny * 10, nextFloatPos * 10 + 15f)
    val radius = nextFloatPos * 2 + 1f
    val color = (nextFloatPos, nextFloatPos, nextFloatPos)
    val emissive = (0f, 0f, 0f)
    Sphere(center, radius, color, emissive, 0.5f, 0.1f, (nextFloatPos, nextFloatPos, nextFloatPos), 1.1f, 0.7f, 0.1f, (nextFloatPos, nextFloatPos, nextFloatPos))
  }

  def randomSpheres(n: Int) = List.fill(n)(randomSphere)

  val spheres = randomSpheres(20).map(sp => sp.copy(center = sp.center + sceneTranslation.xyz))


  val walls = List(

    Quad( // back
      (-15.5f, -15.5f, 25.0f),
      (15.5f, -15.5f, 25.0f),
      (15.5f, 15.5f, 25.0f),
      (-15.5f, 15.5f, 25.0f),
      (0.8f, 0.8f, 0.8f),
      (0f, 0f, 0f)
    ),
    Quad( // right
      (15f, -15.5f, 25.5f),
      (15f, -15.5f, -15.5f),
      (15f, 15.5f, -15.5f),
      (15f, 15.5f, 25.5f),
      (0.0f, 0.8f, 0.0f),
      (0f, 0f, 0f)
    ),
    Quad( // left
      (-15f, -15.5f, 25.5f),
      (-15f, -15.5f, -15.5f),
      (-15f, 15.5f, -15.5f),
      (-15f, 15.5f, 25.5f),
      (0.8f, 0.0f, 0.0f),
      (0f, 0f, 0f)
    ),
    Quad( // bottom
      (-15.5f, 15f, 25.5f),
      (15.5f, 15f, 25.5f),
      (15.5f, 15f, -15.5f),
      (-15.5f, 15f, -15.5f),
      (0.8f, 0.8f, 0.8f),
      (0f, 0f, 0f)
    ),
    Quad( // top
      (-15.5f, -15f, 25.5f),
      (15.5f, -15f, 25.5f),
      (15.5f, -15f, -15.5f),
      (-15.5f, -15f, -15.5f),
      (0.8f, 0.8f, 0.8f),
      (0f, 0f, 0f)
    ),
    Quad( // light
      (-2.5f, -14.8f, 17.5f),
      (2.5f, -14.8f, 17.5f),
      (2.5f, -14.8f, 12.5f),
      (-2.5f, -14.8f, 12.5f),
      (1f, 1f, 1f),
      (10f, 10f, 10f)
    ),
    { // flash
      val x = -10f
      val mX = -5f
      val y = -10f
      val mY = 0f
      val z = -5f
      Quad(
        (x, y, z),
        (mX, y, z),
        (mX, mY, z),
        (x, mY, z),
        (1f, 1f, 1f),
        (20f, 20f, 20f)
      )
    }
  ).map(quad => quad.copy(a = quad.a + sceneTranslation.xyz, b = quad.b + sceneTranslation.xyz, c = quad.c + sceneTranslation.xyz, d = quad.d + sceneTranslation.xyz))

  // Poor man's uniform param
  def function(frame: Int): GArray2DFunction[Vec4[Float32], Vec4[Float32]] = GArray2DFunction(dim, dim, {
    case ((xi: Int32, yi: Int32), lastFrame) =>
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

      def scalarTriple(u: Vec3[Float32], v: Vec3[Float32], w: Vec3[Float32]): Float32 = (u cross v) dot w

      val scalaBase = vec3(0f, 0f, 25.0f)
      val scalaRadius = 3f
      val spiralHeight = 6f
      val scalaHeight = 7f
      val scalaColor = vec3(0.9f, 0.3f, 0.3f)
      val rotations = 2.5f
      val angleHole = 3f

      def testScala(
        rayPos: Vec3[Float32],
        rayDir: Vec3[Float32],
        currentHit: RayHitInfo,
      ): RayHitInfo =
        val a = rayDir.x * rayDir.x + rayDir.z * rayDir.z
        val b = 2f * (rayDir.x * (rayPos.x - scalaBase.x) + rayDir.z * (rayPos.z - scalaBase.z))
        val c = (rayPos.x - scalaBase.x) * (rayPos.x - scalaBase.x) + (rayPos.z - scalaBase.z) * (rayPos.z - scalaBase.z) - scalaRadius * scalaRadius

        val delta = b*b - 4f*a*c
        when(delta < 0.0001f) {
          currentHit
        } otherwise {
          val t1 = (-b - sqrt(delta)) / (2f * a)
          val t2 = (-b + sqrt(delta)) / (2f * a)
          val t = when(t1 > t2)(t2).otherwise(t1)
          val hitPos = rayPos + rayDir * t
          val h = hitPos.y - scalaBase.y
          
          when(h >= scalaBase.y && (h <= scalaBase.y + scalaHeight)) {
            // front face
            val l = spiralHeight / (2f * math.Pi.toFloat * rotations)
            val spiralAngle = acos(l)
            val ct = 1.0f / tan(spiralAngle)
            val x = h / l
            val segmentI = (x / (2f * math.Pi.toFloat)).asInt
            val spiralX = cos(x mod (2f * math.Pi.toFloat))
            val spiralZ = -sin(x mod (2f * math.Pi.toFloat))
            val distToSpiral = length((spiralX * scalaRadius, h, spiralZ * scalaRadius) - (hitPos - scalaBase))
            val angleToSpiral = asin((distToSpiral / 2f) / scalaRadius) * 2f
            val distOnSphereToSpiral = angleToSpiral * scalaRadius
            val distToNearestOnSpiral = distOnSphereToSpiral * ct
            val centerToHit = normalize(hitPos.x - scalaBase.x, hitPos.y - scalaBase.y)
            when(distToNearestOnSpiral < angleHole) {
              val normal = normalize((hitPos.x - scalaBase.x, 0f, hitPos.z - scalaBase.z))
              RayHitInfo(t, normal, scalaColor, (0f, 0f, 0f), 0.5f, 0.2f, scalaColor, 1.0f, 0.0f, 0.0f, scalaColor)
            } otherwise {
              // back face
              val tb = when(t1 < t2)(t2).otherwise(t1)
              val hitPosb = rayPos + rayDir * tb
              val hb = hitPosb.y - scalaBase.y
              val xb = hb / l
              val spiralXb = cos(xb mod (2f * math.Pi.toFloat))
              val spiralZb = -sin(xb mod (2f * math.Pi.toFloat))
              val distToSpiralb = length((spiralXb * scalaRadius, hb, spiralZb * scalaRadius) - (hitPosb - scalaBase))
              val angleToSpiralb = asin((distToSpiralb / 2f) / scalaRadius) * 2f
              val distOnSphereToSpiralb = angleToSpiralb * scalaRadius
              val distToNearestOnSpiralb = distOnSphereToSpiralb * ct
              when(hb >= scalaBase.y && (hb <= scalaBase.y + scalaHeight) && distToNearestOnSpiralb < angleHole) {
                val normal = normalize((hitPosb.x - scalaBase.x, 0f, hitPosb.z - scalaBase.z))
                RayHitInfo(tb, normal, scalaColor, (0f, 0f, 0f), 0.5f, 0.2f, scalaColor, 1.0f, 0.0f, 0.0f, scalaColor)
              } otherwise {
                currentHit
              }
            }
          } otherwise {
            currentHit
          }
        }



      def testQuadTrace(
        rayPos: Vec3[Float32],
        rayDir: Vec3[Float32],
        currentHit: RayHitInfo,
        quad: Quad
      ): RayHitInfo =
        val normal = normalize((quad.c - quad.a) cross (quad.c - quad.b))
        val fixedQuad = when((normal dot rayDir) > 0f) {
          Quad(quad.d, quad.c, quad.b, quad.a, quad.color, quad.emissive)
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
            RayHitInfo(dist, fixedNormal, quad.color, quad.emissive)
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
              RayHitInfo(
                dist, normal, sphere.color, sphere.emissive, sphere.percentSpecular, sphere.roughness, sphere.specularColor,
                sphere.indexOfRefraction, sphere.refractionChance, sphere.refractionRoughness, sphere.refractionColor, fromInside
               )
            } otherwise {
              notHit
            }
          } otherwise {
            notHit
          }
        }

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
        val scalaHit = testScala(rayPos, rayDir, wallsHit)
        scalaHit

      def fresnelReflectAmount(
        n1: Float32, 
        n2: Float32,
        normal: Vec3[Float32], 
        incident: Vec3[Float32],
        f0: Float32,
        f90: Float32
      ): Float32 =
        val r0 = ((n1 - n2) / (n1 + n2)) * ((n1 - n2) / (n1 + n2))
        val cosX = -(normal dot incident)
        when(n1 > n2) {
          val n = n1 / n2
          val sinT2 = n * n * (1f - cosX * cosX)
          when(sinT2 > 1f) {
            f90
          } otherwise {
            val cosX2 = sqrt(1.0f - sinT2)
            val x = 1.0f - cosX2
            val ret = r0 + ((1.0f - r0) * x * x * x * x * x)
            mix(f0, f90, ret)
          }
        } otherwise {
          val x = 1.0f - cosX
          val ret = r0 + ((1.0f - r0) * x * x * x * x * x)
          mix(f0, f90, ret)
        }
        
      val MaxBounces = 8
      def getColorForRay(startRayPos: Vec3[Float32], startRayDir: Vec3[Float32], initRngState: UInt32): RayTraceState =
        val initState = RayTraceState(startRayPos, startRayDir, (0f, 0f, 0f), (1f, 1f, 1f), initRngState)
        GSeq.gen[RayTraceState](
          first = initState,
          next = { case state @ RayTraceState(rayPos, rayDir, color, throughput, rngState, _) =>

            val noHit = RayHitInfo(superFar, (0f, 0f, 0f), (0f, 0f, 0f), (0f, 0f, 0f))
            val testResult = testScene(rayPos, rayDir, noHit)
            when(testResult.dist < superFar) {

              val throughput2 = when(testResult.fromInside) {
                throughput mulV exp[Vec3[Float32]](-testResult.refractionColor * testResult.dist)
              }.otherwise {
                throughput
              }
              
              val specularChance = when(testResult.percentSpecular > 0.0f){
                fresnelReflectAmount(
                  when(testResult.fromInside)(testResult.indexOfRefraction).otherwise(1.0f),
                  when(!testResult.fromInside)(testResult.indexOfRefraction).otherwise(1.0f),
                  rayDir, testResult.normal, testResult.percentSpecular, 1.0f
                )
              }.otherwise {
                0f
              }
              
              val refractionChance = when(specularChance > 0.0f) {
                testResult.refractionChance * ((1.0f - specularChance) / (1.0f - testResult.percentSpecular))
              } otherwise {
                testResult.refractionChance
              }
                
              val Random(rayRoll, nextRngState1) = randomFloat(rngState)
              val doSpecular = when(specularChance > 0.0f && rayRoll < specularChance) {
                1.0f
              }.otherwise(0.0f)
              
              val doRefraction = when(refractionChance > 0.0f && doSpecular === 0.0f && rayRoll < specularChance + refractionChance ) {
                1.0f
              }.otherwise(0.0f)
              
              val rayProbability = when(doSpecular === 1.0f) {
                specularChance
              }.elseWhen(doRefraction === 1.0f) {
                refractionChance
              }.otherwise {
                1.0f - (specularChance + refractionChance)
              }
              
              val rayProbabilityCorrected = max(rayProbability, 0.001f)
              
              val nextRayPos = when(doRefraction === 1.0f) {
                (rayPos + rayDir * testResult.dist) - (testResult.normal * rayPosNormalNudge)
              }.otherwise {
                (rayPos + rayDir * testResult.dist) + (testResult.normal * rayPosNormalNudge)
              }
              
              
              val Random(randomVec1, nextRngState2) = randomVector(nextRngState1)
              val diffuseRayDir = normalize(testResult.normal + randomVec1)
              val specularRayDirPerfect = reflect(rayDir, testResult.normal)
              val specularRayDir = normalize(mix(specularRayDirPerfect, diffuseRayDir, testResult.roughness * testResult.roughness))
              
              val Random(randomVec2, nextRngState3) = randomVector(nextRngState2)
              val refractionRayDirPerfect = refract(
                rayDir, 
                testResult.normal, 
                when(testResult.fromInside)(testResult.indexOfRefraction).otherwise(1.0f / testResult.indexOfRefraction)
              )
              val refractionRayDir = normalize(
                mix(
                  refractionRayDirPerfect, 
                  normalize(-testResult.normal + randomVec2), 
                  testResult.refractionRoughness * testResult.refractionRoughness
              ))
              
              val rayDirSpecular = mix(diffuseRayDir, specularRayDir, doSpecular)
              val rayDirRefracted = mix(rayDirSpecular, refractionRayDir, doRefraction)
              
              val nextColor = (throughput2 mulV testResult.emissive) addV color
              
              val nextThroughput = when(doRefraction === 0.0f) {
                throughput2 mulV mix[Vec3[Float32]](testResult.albedo, testResult.specularColor, doSpecular);
              }.otherwise(throughput2)
              
              val throughputRayProb = nextThroughput * (1.0f / rayProbabilityCorrected)

              RayTraceState(nextRayPos, rayDirRefracted, nextColor, throughputRayProb, nextRngState3)
            } otherwise {
              RayTraceState(rayPos, rayDir, color, throughput, rngState, true)
            }

          }
        ).limit(MaxBounces).takeWhile(!_.finished).lastOr(initState)

      val rngState = xi * 1973 + yi * 9277 + frame * 26699 | 1
      case class RenderIteration(color: Vec3[Float32], rngState: UInt32) extends GStruct[RenderIteration]
      val rayTraceResult =
        GSeq.gen(first = RenderIteration((0f,0f,0f), rngState.unsigned), next = {
          case RenderIteration(_, rngState) =>
            val Random(wiggleX, rngState1) = randomFloat(rngState)
            val Random(wiggleY, rngState2) = randomFloat(rngState1)
            val x = ((xi.asFloat + wiggleX) / dim.toFloat) * 2f - 1f
            val y = ((yi.asFloat + wiggleY) / dim.toFloat) * 2f - 1f
            val xy = (x, y)

            val rayPosition = (0f, 0f, 0f)
            val cameraDist = 1.0f / tan(fovDeg * 0.6f * math.Pi.toFloat / 180.0f)
            val rayTarget = (x, y, cameraDist)

            val rayDir = normalize(rayTarget - rayPosition)
            val rtResult = getColorForRay(rayPosition, rayDir, rngState)
            val withBg = rtResult.color + (bgColor mulV rtResult.throughput)
            RenderIteration(withBg, rtResult.rngState)
          }).limit(pixelIterationsPerFrame).fold((0f,0f,0f), _ addV _.color)
      val color = rayTraceResult * (1.0f / pixelIterationsPerFrame.toFloat)
      // maybe less realistic, but certainly more fun with this commented out
      // val eColor = color * exposure 
      // val aces = ACESFilm(eColor)
      // val srgb = linearToSRGB(aces)
      when(frame == 0) {
        (color, 1.0f)
      } otherwise {
        mix(lastFrame.at(xi, yi), (color, 1.0f), vec4(1.0f / (frame.toFloat + 1f)))
      }
  })
  val initialMem = Array.fill(dim * dim)((0.5f,0.5f,0.5f,0.5f))
  val renders = 10
  List.range(0, renders).foldLeft(initialMem) {
    case(mem, i) =>
      val newMem = Await.result(Vec4FloatMem(mem).map(function(i)), 10.hours)
      ImageUtility.renderToImage(newMem, dim, Paths.get(s"generated.png"))
      println(s"Finished render $i")
      newMem
  }