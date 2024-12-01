package io.computenode.cyfra.foton.utility

object Utility:

  def timed[T](tag: String = "Time taken")(fn: => T): T = {
    val start = System.currentTimeMillis()
    val res = fn
    val end = System.currentTimeMillis()
    println(s"$tag: ${end - start}ms")
    res
  }
