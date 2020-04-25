package com.unihogsoft.scalag.compiler

import java.nio.ByteBuffer

import shapeless.HList
import com.unihogsoft.scalag.dsl.DSL._
import org.lwjgl.BufferUtils

import scala.util.hashing.MurmurHash3
class DSLCompiler {
  def compile[T<: ValType](returnVal: T): ByteBuffer = {
    println("Compiling :)")
    println(formatTree(returnVal.tree))
    println(Digest.formatTreeWithDigest(Digest.digest(returnVal.tree)._1))
    BufferUtils.createByteBuffer(1)
  }
}
