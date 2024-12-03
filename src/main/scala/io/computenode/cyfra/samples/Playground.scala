package io.computenode.cyfra.samples

import io.computenode.cyfra.dsl.Value.Float32
import io.computenode.cyfra.dsl.Value.given
import io.computenode.cyfra.dsl.Algebra.*
import io.computenode.cyfra.dsl.Algebra.given

object Playground:

  @main
  def playground =
    val exampleFloat: Float32 = 3.14f
    println("HI")
    println(s"Example float: ${exampleFloat.name}")
