package io.computenode.cyfra.samples

import io.computenode.cyfra.foton.*
import io.computenode.cyfra.Algebra.*
import io.computenode.cyfra.Algebra.given
import io.computenode.cyfra.Value.*
import io.computenode.cyfra.foton.shapes.*
import io.computenode.cyfra.foton.shapes.{Plane, Sphere}
import io.computenode.cyfra.foton.{Camera, Material, Renderer, Scene}
import io.computenode.cyfra.foton.utility.Color.hex

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
  
  val scene = Scene(
    List(
      // Spheres
      Sphere((-1f, 0.5f, 14f), 3f, sphereMaterial),
      Sphere((3f, 1.5f, 10f), 2f, sphere2Material),
      Sphere((-3f, 2.5f, 10f), 1f, sphere3Material),
      Sphere((9f, -1.5f, 18f), 5f, sphere4Material),
      // Light
      Sphere((-140f, -140f, 10f), 50f, lightMaterial),
      // Floor
      Plane((0f, 3.5f, 0f), (0f, 1f, 0f), floorMaterial),
    ),
    Camera(position = (1f, 0f, -1f))
  )

  val parameters = Renderer.Parameters(
    width = 800,
    height = 800,
    superFar = 300f,
    pixelIterations = 10000,
    bgColor = hex("#ADD8E6")
  )
  val renderer = Renderer(parameters)
  renderer.renderSceneToFile(scene, "output.png")

 
