package io.computenode.cyfra.compiler

import io.computenode.cyfra.compiler.DSLCompiler.ArrayBufferBlock
import io.computenode.cyfra.compiler.SpirvConstants.HEADER_REFS_TOP
import izumi.reflect.Tag
import izumi.reflect.macrortti.LightTypeTag


case class Context(
  valueTypeMap: Map[LightTypeTag, Int] = Map(),
  funPointerTypeMap: Map[Int, Int] = Map(),
  uniformPointerMap: Map[Int, Int] = Map(),
  inputPointerMap: Map[Int, Int] = Map(),
  voidTypeRef: Int = -1,
  voidFuncTypeRef: Int = -1,
  workerIndexRef: Int = -1,
  uniformVarRef: Int = -1,
  constRefs: Map[(Tag[_], Any), Int] = Map(), // todo not sure about float eq on this one (but is the same so?)
  exprRefs: Map[String, Int] = Map(),
  inBufferBlocks: List[ArrayBufferBlock] = List(),
  outBufferBlocks: List[ArrayBufferBlock] = List(),
  nextResultId: Int = HEADER_REFS_TOP,
  nextBinding: Int = 0,
  exprNames: Map[Int, String] = Map()
):
  def nested: Context = this.copy()

  def joinNested(ctx: Context): Context =
    this.copy(nextResultId = ctx.nextResultId, exprNames = ctx.exprNames ++ this.exprNames)

object Context:

  def initialContext: Context = Context()