package com.scalag.compiler

import com.scalag.GSeq.*
import com.scalag.compiler.DSLCompiler.Context
import com.scalag.compiler.Digest.DigestedExpression
import com.scalag.compiler.Opcodes.*
import com.scalag.Value.*
import izumi.reflect.Tag

object GSeqCompiler:
  
  def compileFold(expr: DigestedExpression, fold: FoldSeq[_, _], ctx: Context): (List[Words], Context) =
    val loopBack = ctx.nextResultId
    val mergeBlock = ctx.nextResultId + 1
    val continueTarget = ctx.nextResultId + 2
    val postLoopMergeLabel = ctx.nextResultId + 3
    val shouldTakeVar = ctx.nextResultId + 4
    val iVar = ctx.nextResultId + 5
    val accVar = ctx.nextResultId + 6
    val resultVar = ctx.nextResultId + 7
    val shouldTakeInCheck = ctx.nextResultId + 8
    val iInCheck = ctx.nextResultId + 9
    val isLessThanLimitInCheck = ctx.nextResultId + 10
    val loopCondInCheck = ctx.nextResultId + 11
    val loopCondLabel = ctx.nextResultId + 12
    val accLoaded = ctx.nextResultId + 13
    val iLoaded = ctx.nextResultId + 14
    val iIncremented = ctx.nextResultId + 15
    val finalResult = ctx.nextResultId + 16

    val boolType = ctx.valueTypeMap(DSLCompiler.GBooleanTag.tag)
    val boolPointerType = ctx.funPointerTypeMap(boolType)

    val ops = fold.seq.elemOps
    val genInitExpr = fold.streamInitExpr
    val genInitType = ctx.valueTypeMap(genInitExpr.expr.tag.tag)
    val genInitPointerType = ctx.funPointerTypeMap(genInitType)
    val genNextExpr = fold.streamNextExpr

    val int32Type = ctx.valueTypeMap(DSLCompiler.Int32Tag.tag)
    val int32PointerType = ctx.funPointerTypeMap(int32Type)

    val foldZeroExpr = fold.zeroExpr
    val foldZeroType = ctx.valueTypeMap(foldZeroExpr.expr.tag.tag)
    val foldZeroPointerType = ctx.funPointerTypeMap(foldZeroType)
    val foldFnExpr = fold.fnExpr

    def generateSeqOps(seqExprs: List[(ElemOp[_], DigestedExpression)], context: Context, elemRef: Int): (List[Words], Context) =
      val withElemRefCtx = context.copy(
        exprRefs = context.exprRefs + (fold.seq.currentElemDigest -> elemRef)
      )
      seqExprs match {
        case Nil => // No more transformations, so reduce ops now
          val resultRef = context.nextResultId
          val forReduceCtx = withElemRefCtx.copy(
            exprRefs = withElemRefCtx.exprRefs + (fold.seq.aggregateElemDigest -> resultRef)
          ).copy(nextResultId = context.nextResultId + 1)
          val (reduceOps, reduceCtx) = DSLCompiler.compileBlock(BlockBuilder.buildBlock(foldFnExpr), forReduceCtx)
          val instructions = List(
            Instruction(Op.OpLoad, List(
              ResultRef(foldZeroType),
              ResultRef(resultRef),
              ResultRef(resultVar)
            ))
          ) ::: reduceOps ::: List(
            Instruction(Op.OpStore, List(
              ResultRef(resultVar),
              ResultRef(reduceCtx.exprRefs(foldFnExpr.exprId))
            ))
          )
          (instructions, ctx.joinNested(reduceCtx))
        case (op, dExpr) :: tail =>

          op match {
            case MapOp(_) =>
              val mapBlock = BlockBuilder.buildBlock(dExpr)
              val (mapOps, mapContext) = DSLCompiler.compileBlock(mapBlock, withElemRefCtx)
              val newElemRef = mapContext.exprRefs(dExpr.exprId)
              val (tailOps, tailContext) = generateSeqOps(tail, context.joinNested(mapContext), newElemRef)
              (mapOps ++ tailOps, tailContext)
            case FilterOp(_) =>
              val filterBlock = BlockBuilder.buildBlock(dExpr)
              val (filterOps, filterContext) = DSLCompiler.compileBlock(filterBlock, withElemRefCtx)
              val condResultRef = filterContext.exprRefs(dExpr.exprId)
              val mergeBlock = filterContext.nextResultId
              val trueLabel = filterContext.nextResultId + 1
              val (tailOps, tailContext) = generateSeqOps(
                tail,
                context.joinNested(filterContext).copy(nextResultId = filterContext.nextResultId + 2),
                elemRef
              )
              val instructions = List(
                Instruction(Op.OpSelectionMerge, List(
                  ResultRef(mergeBlock),
                  SelectionControlMask.MaskNone
                )),
                Instruction(Op.OpBranchConditional, List(
                  ResultRef(condResultRef),
                  ResultRef(trueLabel),
                  ResultRef(mergeBlock)
                )),
                Instruction(Op.OpLabel, List(ResultRef(trueLabel))),
              ) ::: tailOps ::: List(
                Instruction(Op.OpBranch, List(ResultRef(mergeBlock))),
                Instruction(Op.OpLabel, List(ResultRef(mergeBlock)))
              )
              (instructions, tailContext)
            case TakeUntilOp(_) =>
              val takeUntilBlock = BlockBuilder.buildBlock(dExpr)
              val (takeUntilOps, takeUntilContext) = DSLCompiler.compileBlock(takeUntilBlock, withElemRefCtx)
              val condResultRef = takeUntilContext.exprRefs(dExpr.exprId)
              val mergeBlock = takeUntilContext.nextResultId
              val trueLabel = takeUntilContext.nextResultId + 1
              val (tailOps, tailContext) = generateSeqOps(
                tail,
                context.joinNested(takeUntilContext).copy(nextResultId = takeUntilContext.nextResultId + 2),
                elemRef
              )
              val instructions = takeUntilOps ::: List(
                Instruction(Op.OpStore, List(
                  ResultRef(shouldTakeVar),
                  ResultRef(condResultRef),
                )),
                Instruction(Op.OpSelectionMerge, List(
                  ResultRef(mergeBlock),
                  SelectionControlMask.MaskNone
                )),
                Instruction(Op.OpBranchConditional, List(
                  ResultRef(condResultRef),
                  ResultRef(trueLabel),
                  ResultRef(mergeBlock)
                )),
                Instruction(Op.OpLabel, List(ResultRef(trueLabel))),
              ) ::: tailOps ::: List(
                Instruction(Op.OpBranch, List(ResultRef(mergeBlock))),
                Instruction(Op.OpLabel, List(ResultRef(mergeBlock)))
              )
              (instructions, tailContext)
          }
      }

    val seqExprs = fold.seq.elemOps.zip(fold.seqExprs)

    val ctxAfterSetup = ctx.copy(
      nextResultId = ctx.nextResultId + 17
    )

    val (seqOps, seqOpsCtx) = generateSeqOps(seqExprs, ctxAfterSetup, accLoaded)

    val withElemRefInitCtx = seqOpsCtx.copy(
      exprRefs = ctx.exprRefs + (fold.seq.currentElemDigest -> accLoaded),
    )
    val (generatorOps, generatorCtx) = DSLCompiler.compileBlock(BlockBuilder.buildBlock(genNextExpr), withElemRefInitCtx)
    val instructions = List(
      Instruction(Op.OpVariable, List( // bool shouldTake
        ResultRef(boolPointerType),
        ResultRef(shouldTakeVar),
        StorageClass.Function
      )),
      Instruction(Op.OpVariable, List( // int i
        ResultRef(int32PointerType),
        ResultRef(iVar),
        StorageClass.Function
      )),
      Instruction(Op.OpVariable, List( // T acc
        ResultRef(genInitPointerType),
        ResultRef(accVar),
        StorageClass.Function
      )),
      Instruction(Op.OpVariable, List( // R result
        ResultRef(foldZeroPointerType),
        ResultRef(resultVar),
        StorageClass.Function
      )),
      Instruction(Op.OpStore, List( // shouldTake = true
        ResultRef(shouldTakeVar),
        ResultRef(ctx.constRefs((DSLCompiler.GBooleanTag, true)))
      )),
      Instruction(Op.OpStore, List( // i = 0
        ResultRef(iVar),
        ResultRef(ctx.constRefs((DSLCompiler.Int32Tag, 0)))
      )),
      Instruction(Op.OpStore, List( // acc = genInitExpr
        ResultRef(accVar),
        ResultRef(ctx.exprRefs(genInitExpr.exprId))
      )),
      Instruction(Op.OpStore, List( // result = foldZeroExpr
        ResultRef(resultVar),
        ResultRef(ctx.exprRefs(foldZeroExpr.exprId))
      )),
      Instruction(Op.OpBranch, List(ResultRef(loopBack))),
      Instruction(Op.OpLabel, List(ResultRef(loopBack))),
      Instruction(Op.OpLoopMerge, List(ResultRef(mergeBlock), ResultRef(continueTarget), LoopControlMask.MaskNone)),
      Instruction(Op.OpBranch, List(ResultRef(postLoopMergeLabel))),
      Instruction(Op.OpLabel, List(ResultRef(postLoopMergeLabel))),
      Instruction(Op.OpLoad, List(
        ResultRef(boolType),
        ResultRef(shouldTakeInCheck),
        ResultRef(shouldTakeVar)
      )),
      Instruction(Op.OpLoad, List(
        ResultRef(int32Type),
        ResultRef(iInCheck),
        ResultRef(iVar)
      )),
      Instruction(Op.OpSLessThan, List(
        ResultRef(boolType),
        ResultRef(isLessThanLimitInCheck),
        ResultRef(iInCheck),
        ResultRef(ctx.exprRefs(fold.limitExpr.exprId))
      )),
      Instruction(Op.OpLogicalAnd, List(
        ResultRef(boolType),
        ResultRef(loopCondInCheck),
        ResultRef(shouldTakeInCheck),
        ResultRef(isLessThanLimitInCheck)
      )),
      Instruction(Op.OpBranchConditional, List(
        ResultRef(loopCondInCheck),
        ResultRef(loopCondLabel),
        ResultRef(mergeBlock)
      )),
      Instruction(Op.OpLabel, List(ResultRef(loopCondLabel))),
      Instruction(Op.OpLoad, List(
        ResultRef(genInitType),
        ResultRef(accLoaded),
        ResultRef(accVar)
      )),
    ) ::: seqOps ::: generatorOps :::
      List(
        Instruction(Op.OpStore, List(
          ResultRef(accVar),
          ResultRef(generatorCtx.exprRefs(genNextExpr.exprId))
        )),
        Instruction(Op.OpLoad, List(
          ResultRef(int32Type),
          ResultRef(iLoaded),
          ResultRef(iVar)
        )),
        Instruction(Op.OpIAdd, List(
          ResultRef(int32Type),
          ResultRef(iIncremented),
          ResultRef(iLoaded),
          ResultRef(ctx.constRefs((DSLCompiler.Int32Tag, 1)))
        )),
        Instruction(Op.OpStore, List(
          ResultRef(iVar),
          ResultRef(iIncremented)
        )),
      )
    ::: List(
      Instruction(Op.OpBranch, List(ResultRef(continueTarget))), // OpBranch continuteTarget
      Instruction(Op.OpLabel, List(ResultRef(continueTarget))), // OpLabel continuteTarget
      Instruction(Op.OpBranch, List(ResultRef(loopBack))), // OpBranch loopBack
      Instruction(Op.OpLabel, List(ResultRef(mergeBlock))), // OpLabel mergeBlock
      Instruction(Op.OpLoad, List(
        ResultRef(foldZeroType),
        ResultRef(finalResult),
        ResultRef(resultVar)
      ))
    )


    (instructions, generatorCtx.copy(
      exprRefs = generatorCtx.exprRefs + (expr.exprId -> finalResult)
    ))
