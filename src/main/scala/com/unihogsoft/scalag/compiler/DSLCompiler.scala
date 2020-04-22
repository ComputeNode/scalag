package com.unihogsoft.scalag.compiler

import java.nio.ByteBuffer

import shapeless.HList
import com.unihogsoft.scalag.dsl.DSL._
import org.lwjgl.BufferUtils
class DSLCompiler {
  def compile[T<: ValType](returnVal: T): ByteBuffer = {
    println("Compiling :)")
    println(formatTree(returnVal.tree))
    
    BufferUtils.createByteBuffer(1)
  }
}
