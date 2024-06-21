package com.scalag

import Algebra.*
import Value.*


trait GStream[T <: Value]:
  def map[R <: Value](fn: T => R): GStream[R] = GStream.Map(this, fn)

object GStream:
  def gen[T <: Value](init: T, next: T => T): GStream[T] =
    new Gen[T](init, next)

  case class Gen[T <: Value](init: T, next: T => T) extends GStream[T]

  case class Map[T <: Value, R <: Value](stream: GStream[T], fn: T => R) extends GStream[R]

  case class Limit[T <: Value](stream: GStream[T], n: Int) extends GStream[T]

  case class TakeUntil[T <: Value](stream: GStream[T], fn: T => Bool) extends GStream[T]

  case class Length[T <: Value](stream: GStream[T]) extends Expression[Int32]





