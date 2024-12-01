package io.computenode.cyfra.foton.utility

object Units:
  
  opaque type Milliseconds <: Float = Float
  object Milliseconds:
    def apply(value: Float): Milliseconds = value
    def toFloat(value: Milliseconds): Float = value
      