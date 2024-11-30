package io.computenode.cyfra.foton.utility

object Utility:

  def timed[T](fn: => T): T = {
    val start = System.currentTimeMillis()
    val res = fn
    val end = System.currentTimeMillis()
    println(s"Time taken: ${end - start}ms")
    res
  }
