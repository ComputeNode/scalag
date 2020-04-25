package com.unihogsoft.scalag.compiler

import java.nio.ByteBuffer

import shapeless.HList
import com.unihogsoft.scalag.dsl.DSL._
import org.lwjgl.BufferUtils

import scala.util.hashing.MurmurHash3
class DSLCompiler {
  def compile[T<: ValType](returnVal: T): ByteBuffer = {
    val tree = returnVal.tree
    val (digestTree, hash) = Digest.digest(tree)
    val sorted = TopologicalSort.sortTree(digestTree)
    println(Digest.formatTreeWithDigest(digestTree))
    println()
    println(sorted.mkString("\n"))
    BufferUtils.createByteBuffer(1)
  }
}
