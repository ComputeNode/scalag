package io.computenode.cyfra.foton.animation

import io.computenode.cyfra.GStruct
import io.computenode.cyfra.Value.Float32
import io.computenode.cyfra.foton.{Camera, Scene}
import io.computenode.cyfra.foton.shapes.Shape
import io.computenode.cyfra.foton.utility.Units.Milliseconds

case class AnimatedScene[State](
  shapes: State => List[Shape],
  camera: State => Camera,
  duration: Milliseconds,
  animate: Float32 => State
):
  def at(time: Float32): Scene = 
    Scene(shapes(animate(time)), camera(animate(time)))
