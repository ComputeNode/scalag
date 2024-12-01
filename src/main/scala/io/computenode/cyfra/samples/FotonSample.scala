package io.computenode.cyfra.samples

import io.computenode.cyfra.foton.*
import io.computenode.cyfra.Algebra.*
import io.computenode.cyfra.Algebra.given
import io.computenode.cyfra.Value.*
import io.computenode.cyfra.foton.animation.{AnimatedScene, AnimationRenderer}
import io.computenode.cyfra.foton.shapes.*
import io.computenode.cyfra.foton.shapes.{Plane, Sphere}
import io.computenode.cyfra.foton.{Camera, Material, Renderer, Scene}
import io.computenode.cyfra.foton.utility.Color.hex
import io.computenode.cyfra.foton.utility.Units.Milliseconds
import io.computenode.cyfra.foton.animation.AnimationFunctions.smooth

import java.nio.file.{Path, Paths}

@main
def main() =
  val sphereMaterial = Material(
    color = (1f, 0.3f, 0.3f),
    emissive = vec3(0f),
    percentSpecular = 0.5f,
    specularColor = (1f, 0.3f, 0.3f) * 0.1f,
    roughness = 0.2f
  )

  val sphere2Material = Material(
    color = (1f, 0.3f, 0.6f),
    emissive = vec3(0f),
    percentSpecular = 0.1f,
    specularColor = (1f, 0.3f, 0.6f) * 0.1f,
    roughness = 0.1f,
    refractionChance = 0.9f,
    indexOfRefraction = 1.5f,
    refractionRoughness = 0.1f,
  )
  val sphere3Material = Material(
    color = (1f, 0.6f, 0.3f),
    emissive = vec3(0f),
    percentSpecular = 0.5f,
    specularColor = (1f, 0.6f, 0.3f) * 0.1f,
    roughness = 0.2f
  )
  val sphere4Material = Material(
    color = (1f, 0.2f, 0.2f),
    emissive = vec3(0f),
    percentSpecular = 0.5f,
    specularColor = (1f, 0.2f, 0.2f) * 0.1f,
    roughness = 0.2f
  )

  val boxMaterial = Material(
    color = (0.3f, 0.3f, 1f),
    emissive = vec3(0f),
    percentSpecular = 0.5f,
    specularColor = (0.3f, 0.3f, 1f) * 0.1f,
    roughness = 0.1f
  )

  val lightMaterial = Material(
    color = (1f, 0.3f, 0.3f),
    emissive = vec3(40f)
  )

  val floorMaterial = Material(
    color = vec3(0.5f),
    emissive = vec3(0f),
    roughness = 0.9f
  )

  case class AnimationControl(
    ballY: Float32,
    cameraZ: Float32
  )

  object AnimationControl:
    def animate(time: Float32): AnimationControl =
      AnimationControl(
        ballY = smooth(from = -5f, to = 1.5f, duration = Milliseconds(2000f))(time),
        cameraZ = smooth(from = -5f, to = -1f, duration = Milliseconds(2000f))(time)
      )

  val scene = AnimatedScene[AnimationControl](
    shapes = control => List(
      // Spheres
      Sphere((-1f, 0.5f, 14f), 3f, sphereMaterial),
      Sphere((3f, control.ballY, 10f), 2f, sphere2Material),
      Sphere((-3f, 2.5f, 10f), 1f, sphere3Material),
      Sphere((9f, -1.5f, 18f), 5f, sphere4Material),
      // Light
      Sphere((-140f, -140f, 10f), 50f, lightMaterial),
      // Floor
      Plane((0f, 3.5f, 0f), (0f, 1f, 0f), floorMaterial),
    ),
    camera = control => Camera(position = (1f, 0f, control.cameraZ)),
    animate = AnimationControl.animate,
    duration = Milliseconds(3000f)
  )

  val parameters = AnimationRenderer.Parameters(
    width = 800,
    height = 800,
    superFar = 300f,
    pixelIterations = 10000,
    iterations = 2,
    bgColor = hex("#ADD8E6"),
    framesPerSecond = 30
  )
  val renderer = AnimationRenderer(parameters)
  renderer.renderFramesToDir(scene, Paths.get("output"))

/**
 * TODO:
 *  - treeId -> valueId cache for scope when compiling? It probably explodes now.
 *  - Animation abstraction for any function, get rid of intermediate Struct, we can derive it just from time context. Reuse logic for rendering pics.
 *  - Optimize memory: Not keep all LazyList, also reuse GPU memory.
 *  - Nice Julia set and something more!
 */

// Renderable with ffmpeg -framerate 30 -pattern_type sequence -start_number 001 -i frame%03d.png -s:v 1920x1080 -c:v libx264 -crf 17 -pix_fmt yuv420p output.mp4
 
