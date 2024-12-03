package io.computenode.cyfra.spirv.compilers

import ExpressionCompiler.compileBlock
import io.computenode.cyfra.spirv.Opcodes.*
import io.computenode.cyfra.dsl.*
import io.computenode.cyfra.dsl.Control.WhenExpr
import io.computenode.cyfra.spirv.Digest.DigestedExpression
import io.computenode.cyfra.spirv.{Context, ScopeBuilder}
import izumi.reflect.Tag
import io.computenode.cyfra.spirv.SpirvConstants.*
import io.computenode.cyfra.spirv.SpirvTypes.*

private[cyfra] object WhenCompiler:
  
  def compileWhen(expr: DigestedExpression, when: WhenExpr[_], ctx: Context): (List[Words], Context) = {
    def compileCases(
      ctx: Context,
      resultVar: Int,
      conditions: List[DigestedExpression],
      thenCodes: List[DigestedExpression],
      elseCode: DigestedExpression
    ): (List[Words], Context) = (conditions, thenCodes) match {
      case (Nil, Nil) =>
        val (elseInstructions, elseCtx) = compileBlock(
          ScopeBuilder.buildScope(elseCode),
          ctx
        )
        val elseWithStore = elseInstructions :+ Instruction(Op.OpStore, List(
          ResultRef(resultVar),
          ResultRef(elseCtx.exprRefs(elseCode.exprId))
        ))
        (elseWithStore, elseCtx)
      case (caseWhen :: cTail, tCode :: tTail) =>
        val (whenInstructions, whenCtx) = compileBlock(
          ScopeBuilder.buildScope(caseWhen),
          ctx
        )
        val (thenInstructions, thenCtx) = compileBlock(
          ScopeBuilder.buildScope(tCode),
          whenCtx
        )
        val thenWithStore = thenInstructions :+ Instruction(Op.OpStore, List(
          ResultRef(resultVar),
          ResultRef(thenCtx.exprRefs(tCode.exprId))
        ))
        val postCtx = whenCtx.joinNested(thenCtx)
        val endIfLabel = postCtx.nextResultId
        val thenLabel = postCtx.nextResultId + 1
        val elseLabel = postCtx.nextResultId + 2
        val contextForNextIter = postCtx.copy(nextResultId = postCtx.nextResultId + 3)
        val (elseInstructions, elseCtx) = compileCases(
          contextForNextIter,
          resultVar,
          cTail,
          tTail,
          elseCode
        )
        (
          whenInstructions ::: List(
            Instruction(Op.OpSelectionMerge, List(
              ResultRef(endIfLabel),
              SelectionControlMask.MaskNone
            )),
            Instruction(Op.OpBranchConditional, List(
              ResultRef(postCtx.exprRefs(caseWhen.exprId)),
              ResultRef(thenLabel),
              ResultRef(elseLabel)
            )),
            Instruction(Op.OpLabel, List(ResultRef(thenLabel))) // then
          ) ::: thenWithStore ::: List(
            Instruction(Op.OpBranch, List(ResultRef(endIfLabel))),
            Instruction(Op.OpLabel, List(ResultRef(elseLabel))) // else
          ) ::: elseInstructions ::: List(
            Instruction(Op.OpBranch, List(ResultRef(endIfLabel))),
            Instruction(Op.OpLabel, List(ResultRef(endIfLabel))) // end
          ),
          postCtx.joinNested(elseCtx)
        )
    }
  
    val resultVar = ctx.nextResultId
    val resultLoaded = ctx.nextResultId + 1
    val resultTypeTag = ctx.valueTypeMap(when.tag.tag)
    val contextForCases = ctx.copy(nextResultId = ctx.nextResultId + 2)
  
    val blockDeps = expr.blockDeps
    val thenCode = blockDeps.head
    val elseCode = blockDeps.last
    val (conds, thenCodes) = blockDeps.tail.init.splitAt(when.otherConds.length)
    val (caseInstructions, caseCtx) = compileCases(
      contextForCases,
      resultVar,
      expr.dependencies.head :: conds,
      thenCode :: thenCodes,
      elseCode
    )
    val instructions = List(
      Instruction(Op.OpVariable, List(
        ResultRef(ctx.funPointerTypeMap(resultTypeTag)),
        ResultRef(resultVar),
        StorageClass.Function
      ))) ::: caseInstructions ::: List(
      Instruction(Op.OpLoad, List(
        ResultRef(resultTypeTag),
        ResultRef(resultLoaded),
        ResultRef(resultVar)
      ))
    )
    (instructions, caseCtx.copy(exprRefs = caseCtx.exprRefs + (expr.exprId -> resultLoaded)))
  }