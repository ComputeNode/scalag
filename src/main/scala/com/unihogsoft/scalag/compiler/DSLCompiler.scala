package com.unihogsoft.scalag.compiler

import java.nio.ByteBuffer

import shapeless.HList
import com.unihogsoft.scalag.dsl.DSL._
class DSLCompiler {
  def compile[T<: ValType](returnVal: T): ByteBuffer = {
    println("Compiling :)")
    println(returnVal)
  }
}
