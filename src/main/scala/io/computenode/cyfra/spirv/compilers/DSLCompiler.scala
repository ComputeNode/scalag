package io.computenode.cyfra.spirv.compilers

import io.computenode.cyfra.dsl.Control.*
import io.computenode.cyfra.dsl.Expression.*
import io.computenode.cyfra.*
import io.computenode.cyfra.dsl.Value.*
import io.computenode.cyfra.spirv.Opcodes.*
import io.computenode.cyfra.dsl.{ComposeStruct, Expression, FunctionName, Functions, GArrayElem, GSeq, GStruct, GStructSchema, GetField, PhantomExpression, UniformStructRefTag, Value, WorkerIndexTag}
import izumi.reflect.Tag
import izumi.reflect.macrortti.{LTag, LTagK, LightTypeTag}
import org.lwjgl.BufferUtils
import SpirvProgramCompiler.{ArrayBufferBlock, compileMain, createAndInitUniformBlock, createInvocationId, defineConstants, defineVarNames, defineVoids, getNameDecorations, headers, initAndDecorateUniforms}
import io.computenode.cyfra.spirv.Digest.DigestedExpression
import io.computenode.cyfra.spirv.SpirvConstants.*
import io.computenode.cyfra.spirv.SpirvTypes.*
import io.computenode.cyfra.spirv.compilers.ExpressionCompiler.compileBlock
import io.computenode.cyfra.spirv.compilers.GStructCompiler.defineStructTypes
import io.computenode.cyfra.spirv.{Context, Digest, ScopeBuilder}

import java.nio.ByteBuffer
import scala.annotation.tailrec
import scala.collection.mutable
import scala.math.random
import scala.runtime.stdLibPatches.Predef.summon
import scala.util.Random

private[cyfra] object DSLCompiler:

  private def getAllBlocksExprs(root: DigestedExpression): List[DigestedExpression] = {
    var blockI = 0
    val allBlocksCache = mutable.Map[Int, List[DigestedExpression]]()
    val visited = mutable.Set[Int]()
    def getAllBlocksExprsAcc(root: DigestedExpression): List[DigestedExpression] = {
      if (visited.contains(root.expr.treeid)) {
        return Nil
      }
      if (allBlocksCache.contains(root.expr.treeid)) {
        return allBlocksCache(root.expr.treeid)
      }
      val result = List(root).flatMap {
        case d@DigestedExpression(_, _, deps, blockDeps, _) =>
          d :: deps.flatMap(b => getAllBlocksExprsAcc(b)) ::: blockDeps.flatMap(b => getAllBlocksExprsAcc(b))
      }
      visited += root.expr.treeid
      blockI += 1
      if (blockI % 100 == 0) {
        allBlocksCache.update(root.expr.treeid, result)
      }
      result
    }
    val result = getAllBlocksExprsAcc(root)
    allBlocksCache(root.expr.treeid) = result
    blockI += 1
    result
  }
  
  def compile(returnVal: Value, inTypes: List[Tag[_]], outTypes: List[Tag[_]], uniformSchema: GStructSchema[_]): ByteBuffer =
    val tree = returnVal.tree
    val digestTree = Digest.digest(tree)
    val sorted = ScopeBuilder.buildScope(digestTree)
    val allBlockExprs = getAllBlocksExprs(digestTree)
    val insnWithHeader = SpirvProgramCompiler.headers()
    val typesInCode = allBlockExprs.map(_.expr.tag)
    val allTypes = (typesInCode ::: inTypes ::: outTypes).distinct
    def scalarTypes = allTypes.filter(_.tag <:< summon[Tag[Scalar]].tag)
    val (typeDefs, typedContext) = defineScalarTypes(scalarTypes, Context.initialContext)
    val structsInCode = (allBlockExprs.map(_.expr).collect {
      case cs: ComposeStruct[_] => cs.resultSchema
      case gf: GetField[_, _] => gf.resultSchema
    } :+ uniformSchema).distinct
    val (structDefs, structCtx) = defineStructTypes(structsInCode, typedContext)
    val (decorations, uniformDefs, uniformContext) = initAndDecorateUniforms(inTypes, outTypes, structCtx)
    val (uniformStructDecorations, uniformStructInsns, uniformStructContext) = createAndInitUniformBlock(uniformSchema, uniformContext)
    val (inputDefs, inputContext) = createInvocationId(uniformStructContext)
    val (constDefs, constCtx) = defineConstants(allBlockExprs, inputContext)
    val (varDefs, varCtx) = defineVarNames(constCtx)
    val resultType = returnVal.tree.tag
    val (main, finalCtx) = compileMain(sorted, resultType, varCtx)
    val nameDecorations = getNameDecorations(finalCtx)

    val code: List[Words] =
      insnWithHeader :::
        decorations :::
        nameDecorations :::
        uniformStructDecorations :::
        typeDefs :::
        structDefs :::
        uniformDefs :::
        uniformStructInsns :::
        inputDefs :::
        constDefs :::
        varDefs :::
        main

    val fullCode = code.map {
      case WordVariable(name) if name == BOUND_VARIABLE => IntWord(finalCtx.nextResultId)
      case x => x
    }
    val bytes = fullCode.flatMap(_.toWords).toArray

    BufferUtils.createByteBuffer(bytes.length).put(bytes).rewind()


