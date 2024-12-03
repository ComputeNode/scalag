package io.computenode.cyfra.spirv.compilers

import io.computenode.cyfra.spirv.Opcodes.*
import io.computenode.cyfra.dsl.Expression.*
import io.computenode.cyfra.dsl.Algebra.*
import io.computenode.cyfra.dsl.Value.*
import ExtFunctionCompiler.compileExtFunctionCall
import WhenCompiler.compileWhen
import io.computenode.cyfra.dsl.Control.WhenExpr
import io.computenode.cyfra.dsl.{ComposeStruct, Functions, GArrayElem, GSeq, GStructSchema, GetField, PhantomExpression, UniformStructRefTag, WorkerIndexTag}
import io.computenode.cyfra.spirv.Context
import io.computenode.cyfra.spirv.Digest.DigestedExpression
import izumi.reflect.Tag
import io.computenode.cyfra.spirv.SpirvConstants.*
import io.computenode.cyfra.spirv.SpirvTypes.*

import scala.annotation.tailrec
import scala.collection.immutable.List as expr

private[cyfra] object ExpressionCompiler:
  
  private def binaryOpOpcode(expr: BinaryOpExpression[_]) = expr match
    case _: Sum[_] => (Op.OpIAdd, Op.OpFAdd)
    case _: Diff[_] => (Op.OpISub, Op.OpFSub)
    case _: Mul[_] => (Op.OpIMul, Op.OpFMul)
    case _: Div[_] => (Op.OpSDiv, Op.OpFDiv)
    case _: Mod[_] => (Op.OpSMod, Op.OpFMod)

  private def compileBinaryOpExpression(expr: DigestedExpression, bexpr: BinaryOpExpression[_], ctx: Context): (List[Instruction], Context) =
    val tpe = bexpr.tag
    val typeRef = ctx.valueTypeMap(tpe.tag)
    val subOpcode = tpe match {
      case i if i.tag <:< summon[Tag[IntType]].tag || i.tag <:< summon[Tag[UIntType]].tag
        || (i.tag <:< summon[Tag[Vec[_]]].tag && i.tag.typeArgs.head <:< summon[Tag[IntType]].tag) =>
        binaryOpOpcode(bexpr)._1
      case f if f.tag <:< summon[Tag[FloatType]].tag
        || (f.tag <:< summon[Tag[Vec[_]]].tag && f.tag.typeArgs.head <:< summon[Tag[FloatType]].tag) =>
        binaryOpOpcode(bexpr)._2
    }
    val instructions = List(
      Instruction(subOpcode, List(
        ResultRef(typeRef),
        ResultRef(ctx.nextResultId),
        ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
        ResultRef(ctx.exprRefs(expr.dependencies(1).exprId))
      ))
    )
    val updatedContext = ctx.copy(
      exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
      nextResultId = ctx.nextResultId + 1
    )
    (instructions, updatedContext)

  private def compileConvertExpression(expr: DigestedExpression, cexpr: ConvertExpression[_, _], ctx: Context): (List[Instruction], Context) =
    val tpe = cexpr.tag
    val typeRef = ctx.valueTypeMap(tpe.tag)
    val tfOpcode = (cexpr.fromTag, expr.expr) match {
      case (from, _: ToFloat32[_]) if from.tag =:= Int32Tag.tag => Op.OpConvertSToF
      case (from, _: ToFloat32[_]) if from.tag =:= UInt32Tag.tag => Op.OpConvertUToF
      case (from, _: ToInt32[_]) if from.tag =:= Float32Tag.tag => Op.OpConvertFToS
      case (from, _: ToUInt32[_]) if from.tag =:= Float32Tag.tag => Op.OpConvertFToU
      case (from, _: ToInt32[_]) if from.tag =:= UInt32Tag.tag => Op.OpBitcast
      case (from, _: ToUInt32[_]) if from.tag =:= Int32Tag.tag => Op.OpBitcast
    }
    val instructions = List(
      Instruction(tfOpcode, List(
        ResultRef(typeRef),
        ResultRef(ctx.nextResultId),
        ResultRef(ctx.exprRefs(expr.dependencies(0).exprId))
      )))
    val updatedContext = ctx.copy(
      exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
      nextResultId = ctx.nextResultId + 1
    )
    (instructions, updatedContext)
  

  def comparisonOp(comparisonOpExpression: ComparisonOpExpression[_]) =
    comparisonOpExpression match
      case _: GreaterThan[_] => (Op.OpSGreaterThan, Op.OpFOrdGreaterThan)
      case _: LessThan[_] => (Op.OpSLessThan, Op.OpFOrdLessThan)
      case _: GreaterThanEqual[_] => (Op.OpSGreaterThanEqual, Op.OpFOrdGreaterThanEqual)
      case _: LessThanEqual[_] => (Op.OpSLessThanEqual, Op.OpFOrdLessThanEqual)
      case _: Equal[_] => (Op.OpIEqual, Op.OpFOrdEqual)

  private def compileBitwiseExpression(expr: DigestedExpression, bexpr: BitwiseOpExpression[_], ctx: Context): (List[Instruction], Context) =
    val tpe = bexpr.tag
    val typeRef = ctx.valueTypeMap(tpe.tag)
    val subOpcode = bexpr match {
      case _: BitwiseAnd[_] => Op.OpBitwiseAnd
      case _: BitwiseOr[_] => Op.OpBitwiseOr
      case _: BitwiseXor[_] => Op.OpBitwiseXor
      case _: BitwiseNot[_] => Op.OpNot
      case _: ShiftLeft[_] => Op.OpShiftLeftLogical
      case _: ShiftRight[_] => Op.OpShiftRightLogical
    }
    val instructions = List(
      Instruction(subOpcode, List(
        ResultRef(typeRef),
        ResultRef(ctx.nextResultId),
      ) ::: expr.dependencies.map(d => ResultRef(ctx.exprRefs(d.exprId))))
    )
    val updatedContext = ctx.copy(
      exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
      nextResultId = ctx.nextResultId + 1
    )
    (instructions, updatedContext)

  def compileBlock(sortedTree: List[DigestedExpression], ctx: Context): (List[Words], Context) = {

    @tailrec
    def compileExpressions(exprs: List[DigestedExpression], ctx: Context, acc: List[Words], usedNames: Set[String]): (List[Words], Context) = {
      if (exprs.isEmpty) (acc, ctx)
      else {
        val expr = exprs.head
        if (ctx.exprRefs.contains(expr.exprId)) {
          compileExpressions(exprs.tail, ctx, acc, usedNames)
        } else {
          val name: Option[String] = if usedNames.contains(expr.name) then None else Some(expr.name)
          val updatedUsedNames = usedNames ++ name
          val (instructions, updatedCtx) = expr.expr match {
            case c@Const(x) =>
              val constRef = ctx.constRefs((c.tag, x))
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> constRef)
              )
              (List(), updatedContext)

            case d@Dynamic(WorkerIndexTag) =>
              (Nil, ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.workerIndexRef),
              ))

            case d@Dynamic(UniformStructRefTag) =>
              (Nil, ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.uniformVarRef),
              ))

            case c: ConvertExpression[_, _] =>
              compileConvertExpression(expr, c, ctx)

            case b: BinaryOpExpression[_] =>
              compileBinaryOpExpression(expr, b, ctx)

            case negate: Negate[_] =>
              val op = if (negate.tag.tag <:< summon[Tag[FloatType]].tag || (negate.tag.tag <:< summon[Tag[Vec[_]]].tag && negate.tag.tag.typeArgs.head <:< summon[Tag[FloatType]].tag))
                Op.OpFNegate
              else Op.OpSNegate
              val instructions = List(
                Instruction(op, List(
                  ResultRef(ctx.valueTypeMap(negate.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case bo: BitwiseOpExpression[_] =>
              compileBitwiseExpression(expr, bo, ctx)

            case and: And =>
              val instructions = List(
                Instruction(Op.OpLogicalAnd, List(
                  ResultRef(ctx.valueTypeMap(GBooleanTag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case or: Or =>
              val instructions = List(
                Instruction(Op.OpLogicalOr, List(
                  ResultRef(ctx.valueTypeMap(GBooleanTag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case not: Not =>
              val instructions = List(
                Instruction(Op.OpLogicalNot, List(
                  ResultRef(ctx.valueTypeMap(GBooleanTag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case sp: ScalarProd[_, _] =>
              val instructions = List(
                Instruction(Op.OpVectorTimesScalar, List(
                  ResultRef(ctx.valueTypeMap(sp.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).exprId)))
                ))
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case dp: DotProd[_, _] =>
              val instructions = List(
                Instruction(Op.OpDot, List(
                  ResultRef(ctx.valueTypeMap(dp.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case co: ComparisonOpExpression[_] =>
              val (intOp, floatOp) = comparisonOp(co)
              val op = if (co.operandTag.tag <:< summon[Tag[FloatType]].tag) floatOp else intOp
              val instructions = List(
                Instruction(op, List(
                  ResultRef(ctx.valueTypeMap(GBooleanTag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)


            case e: ExtractScalar[_, _] =>
              val instructions = List(
                Instruction(Op.OpVectorExtractDynamic, List(
                  ResultRef(ctx.valueTypeMap(e.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case composeVec2: ComposeVec2[_] =>
              val instructions = List(
                Instruction(Op.OpCompositeConstruct, List(
                  ResultRef(ctx.valueTypeMap(composeVec2.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case composeVec3: ComposeVec3[_] =>
              val instructions = List(
                Instruction(Op.OpCompositeConstruct, List(
                  ResultRef(ctx.valueTypeMap(composeVec3.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(2).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case composeVec4: ComposeVec4[_] =>
              val instructions = List(
                Instruction(Op.OpCompositeConstruct, List(
                  ResultRef(ctx.valueTypeMap(composeVec4.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(2).exprId)),
                  ResultRef(ctx.exprRefs(expr.dependencies(3).exprId))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case fc: ExtFunctionCall[_] =>
              compileExtFunctionCall(expr, fc, ctx)

            case ga@GArrayElem(index, i) =>
              val instructions = List(
                Instruction(Op.OpAccessChain, List(
                  ResultRef(ctx.uniformPointerMap(ctx.valueTypeMap(ga.tag.tag))),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.inBufferBlocks(index).blockVarRef),
                  ResultRef(ctx.constRefs((Int32Tag, 0))),
                  ResultRef(ctx.exprRefs(expr.dependencies.head.exprId))
                )),
                Instruction(Op.OpLoad, List(
                  IntWord(ctx.valueTypeMap(ga.tag.tag)),
                  ResultRef(ctx.nextResultId + 1),
                  ResultRef(ctx.nextResultId)
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> (ctx.nextResultId + 1)),
                nextResultId = ctx.nextResultId + 2
              )
              (instructions, updatedContext)

            case when: WhenExpr[_] =>
              compileWhen(expr, when, ctx)

            case fd: GSeq.FoldSeq[_, _] =>
              GSeqCompiler.compileFold(expr, fd, ctx)

            case cs: ComposeStruct[_] =>
              val schema = cs.resultSchema.asInstanceOf[GStructSchema[_]]
              val fields = cs.fields
              val insns: List[Instruction] = List(
                Instruction(Op.OpCompositeConstruct, List(
                  ResultRef(ctx.valueTypeMap(cs.tag.tag)),
                  ResultRef(ctx.nextResultId),
                ) ::: fields.zipWithIndex.map {
                  case (f, i) => ResultRef(ctx.exprRefs(expr.dependencies(i).exprId))
                })
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (insns, updatedContext)
            case gf@GetField(dynamic@Dynamic(UniformStructRefTag), fieldIndex) =>
              val insns: List[Instruction] = List(
                Instruction(Op.OpAccessChain, List(
                  ResultRef(ctx.uniformPointerMap(ctx.valueTypeMap(gf.tag.tag))),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.uniformVarRef),
                  ResultRef(ctx.constRefs((Int32Tag, gf.fieldIndex)))
                )),
                Instruction(Op.OpLoad, List(
                  IntWord(ctx.valueTypeMap(gf.tag.tag)),
                  ResultRef(ctx.nextResultId + 1),
                  ResultRef(ctx.nextResultId)
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> (ctx.nextResultId + 1)),
                nextResultId = ctx.nextResultId + 2
              )
              (insns, updatedContext)
            case gf: GetField[_, _] =>
              val insns: List[Instruction] = List(
                Instruction(Op.OpCompositeExtract, List(
                  ResultRef(ctx.valueTypeMap(gf.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies.head.exprId)),
                  IntWord(gf.fieldIndex)
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.exprId -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (insns, updatedContext)

            case ph: PhantomExpression[_] => (List(), ctx)
          }
          val ctxWithName = updatedCtx.copy(
            exprNames = updatedCtx.exprNames ++ name.map(n => (updatedCtx.nextResultId - 1, n)).toMap
          )
          compileExpressions(exprs.tail, ctxWithName, acc ::: instructions, updatedUsedNames)
        }
      }
    }

    compileExpressions(sortedTree, ctx, Nil, Set.empty)
  }
