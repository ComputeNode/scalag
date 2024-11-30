package io.computenode.cyfra.foton

import io.computenode.cyfra.Value.{Float32, Vec3}
import io.computenode.cyfra.Value.*
import io.computenode.cyfra.*
import io.computenode.cyfra.Algebra.*
import io.computenode.cyfra.Algebra.given
import io.computenode.cyfra.given
import io.computenode.cyfra.{GStruct, Value}

case class Material(
  color: Vec3[Float32],
  emissive: Vec3[Float32],
  percentSpecular: Float32 = 0f,
  roughness: Float32 = 0f,
  specularColor: Vec3[Float32] = vec3(0f),
  indexOfRefraction: Float32 = 1f,
  refractionChance: Float32 = 0f,
  refractionRoughness: Float32 = 0f,
  refractionColor: Vec3[Float32] = vec3(0f),
) extends GStruct[Material]

object Material:
  val Zero = Material(vec3(0f), vec3(0f))