package io.computenode.cyfra.spirv

import io.computenode.cyfra.spirv.SpirvConstants.HEADER_REFS_TOP
import io.computenode.cyfra.spirv.compilers.SpirvProgramCompiler.ArrayBufferBlock
import izumi.reflect.Tag
import izumi.reflect.macrortti.LightTypeTag


private[cyfra] case class Context(
  valueTypeMap: Map[LightTypeTag, Int] = Map(),
  funPointerTypeMap: Map[Int, Int] = Map(),
  uniformPointerMap: Map[Int, Int] = Map(),
  inputPointerMap: Map[Int, Int] = Map(),
  voidTypeRef: Int = -1,
  voidFuncTypeRef: Int = -1,
  workerIndexRef: Int = -1,
  uniformVarRef: Int = -1,
  constRefs: Map[(Tag[_], Any), Int] = Map(),
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

private[cyfra] object Context:

  def initialContext: Context = Context()