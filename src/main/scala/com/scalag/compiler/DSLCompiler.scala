package com.scalag.compiler

import com.scalag.compiler.Digest.DigestedExpression
import com.scalag.compiler.Opcodes.*
import com.scalag.Value
import com.scalag.Value.*
import com.scalag.Expression.*
import com.scalag.*
import com.scalag.Control.*
import org.lwjgl.BufferUtils
import izumi.reflect.Tag
import izumi.reflect.macrortti.{LTag, LTagK, LightTypeTag}

import java.nio.ByteBuffer
import scala.annotation.tailrec
import scala.collection.mutable
import scala.math.random
import scala.runtime.stdLibPatches.Predef.summon
import scala.util.Random

object DSLCompiler {
  val scalagVendorId: Byte = 44 // https://github.com/KhronosGroup/SPIRV-Headers/blob/main/include/spirv/spir-v.xml#L52

  val localSizeX = 128
  val localSizeY = 1
  val localSizeZ = 1

  val BOUND_VARIABLE = "bound"
  val GLSL_EXT_NAME = "GLSL.std.450"
  val GLSL_EXT_REF = 1
  val TYPE_VOID_REF = 2
  val VOID_FUNC_TYPE_REF = 3
  val MAIN_FUNC_REF = 4
  val GL_GLOBAL_INVOCATION_ID_REF = 5
  val GL_WORKGROUP_SIZE_REF = 6
  val HEADER_REFS_TOP = 7
  
  val Int32Tag = summon[Tag[Int32]]
  val UInt32Tag = summon[Tag[UInt32]]
  val Float32Tag = summon[Tag[Float32]]
  val GBooleanTag = summon[Tag[GBoolean]]
  val Vec2TagWithoutArgs = summon[Tag[Vec2[_]]].tag.withoutArgs
  val Vec3TagWithoutArgs = summon[Tag[Vec3[_]]].tag.withoutArgs
  val Vec4TagWithoutArgs = summon[Tag[Vec4[_]]].tag.withoutArgs
  val Vec2Tag = summon[Tag[Vec2[_]]]
  val Vec3Tag = summon[Tag[Vec3[_]]]
  val Vec4Tag = summon[Tag[Vec4[_]]]
  
  

  def scalarTypeDefInsn(tag: Tag[_], typeDefIndex: Int) = tag match {
    case t if t == Int32Tag => Instruction(Op.OpTypeInt, List(ResultRef(typeDefIndex), IntWord(32), IntWord(1)))
    case t if t == UInt32Tag => Instruction(Op.OpTypeInt, List(ResultRef(typeDefIndex), IntWord(32), IntWord(0)))
    case t if t == Float32Tag => Instruction(Op.OpTypeFloat, List(ResultRef(typeDefIndex), IntWord(32)))
    case t if t == GBooleanTag => Instruction(Op.OpTypeBool, List(ResultRef(typeDefIndex)))
  }

  val typeStride = Map[Tag[_], Int](
    Int32Tag -> 4,
    UInt32Tag -> 4,
    Float32Tag -> 4,
    summon[Tag[Vec2[Float32]]] -> 8,
    summon[Tag[Vec2[Int32]]] -> 8,
    summon[Tag[Vec2[UInt32]]] -> 8,
    summon[Tag[Vec3[Float32]]] -> 12,
    summon[Tag[Vec3[Int32]]] -> 12,
    summon[Tag[Vec4[Float32]]] -> 16,
    summon[Tag[Vec4[Int32]]] -> 16,
    summon[Tag[Vec4[UInt32]]] -> 16
  )

  def headers(): List[Words] = {
    Word(Array(0x03, 0x02, 0x23, 0x07)) :: // SPIR-V
      Word(Array(0x00, 0x00, 0x01, 0x00)) :: // Version: 0.1.0
      Word(Array(scalagVendorId, 0x00, 0x01, 0x00)) :: // Generator: Scalag; 1
      WordVariable(BOUND_VARIABLE) :: // Bound: To be calculated
      Word(Array(0x00, 0x00, 0x00, 0x00)) :: // Schema: 0
      Instruction(Op.OpCapability, List(Capability.Shader)) :: // OpCapability Shader
      Instruction(Op.OpExtInstImport, List(ResultRef(GLSL_EXT_REF), Text(GLSL_EXT_NAME))) :: // OpExtInstImport "GLSL.std.450"
      Instruction(Op.OpMemoryModel, List(AddressingModel.Logical, MemoryModel.GLSL450)) :: // OpMemoryModel Logical GLSL450
      Instruction(Op.OpEntryPoint, List(
        ExecutionModel.GLCompute, ResultRef(MAIN_FUNC_REF), Text("main"), ResultRef(GL_GLOBAL_INVOCATION_ID_REF)
      )) :: // OpEntryPoint GLCompute %MAIN_FUNC_REF "main" %GL_GLOBAL_INVOCATION_ID_REF
      Instruction(Op.OpExecutionMode, List(
        ResultRef(MAIN_FUNC_REF), ExecutionMode.LocalSize, IntWord(128), IntWord(1), IntWord(1)
      )) :: // OpExecutionMode %4 LocalSize 128 1 1
      Instruction(Op.OpSource, List(SourceLanguage.GLSL, IntWord(450))) :: // OpSource GLSL 450
      Instruction(Op.OpDecorate, List(
        ResultRef(GL_GLOBAL_INVOCATION_ID_REF), Decoration.BuiltIn, BuiltIn.GlobalInvocationId
      )) :: // OpDecorate %GL_GLOBAL_INVOCATION_ID_REF BuiltIn GlobalInvocationId
      Instruction(Op.OpDecorate, List(
        ResultRef(GL_WORKGROUP_SIZE_REF), Decoration.BuiltIn, BuiltIn.WorkgroupSize
      )) ::
      Nil
  }

  case class ArrayBufferBlock(
     structTypeRef: Int, // %BufferX
     blockVarRef: Int, // %__X
     blockPointerRef: Int, // _ptr_Uniform_OutputBufferX
     memberArrayTypeRef: Int, // %_runtimearr_float_X
     binding: Int
   )

  case class Context(
    valueTypeMap: Map[LightTypeTag, Int] = Map(),
    scalarTypeMap: Map[Tag[_], Int] = Map(),
    funPointerTypeMap: Map[Int, Int] = Map(),
    uniformPointerMap: Map[Int, Int] = Map(),
    inputPointerMap: Map[Int, Int] = Map(),
    vectorTypeMap: Map[LightTypeTag, Int] = Map(),
    funVecPointerTypeMap: Map[Int, Int] = Map(),
    uniformVecPointerMap: Map[Int, Int] = Map(),
    inputVecPointerMap: Map[Int, Int] = Map(),
    voidTypeRef: Int = -1,
    voidFuncTypeRef: Int = -1,
    workerIndexRef: Int = -1,

    constRefs: Map[(Tag[_], Any), Int] = Map(), // todo not sure about float eq on this one (but is the same so?)
    exprRefs: Map[String, Int] = Map(),
    inBufferBlocks: List[ArrayBufferBlock] = List(),
    outBufferBlocks: List[ArrayBufferBlock] = List(),
    nextResultId: Int = HEADER_REFS_TOP,
    nextBinding: Int = 0,
  ):
    def nested: Context = this.copy()

    def joinNested(ctx: Context): Context =
      this.copy(nextResultId = ctx.nextResultId)

  def initialContext: Context = Context()
  type Vec2C[T <: Value] = Vec2[T]
  type Vec3C[T <: Value] = Vec3[T]
  type Vec4C[T <: Value] = Vec4[T]
  def defineScalarTypes(types: List[Tag[_]]): (List[Words], Context) = {
    val basicTypes = List(Int32Tag, Float32Tag, UInt32Tag, GBooleanTag)
    (basicTypes ::: types).distinct.foldLeft((List[Words](), initialContext)) {
      case ((words, ctx), valType) =>
        val typeDefIndex = ctx.nextResultId
        val code = List(
          scalarTypeDefInsn(valType, typeDefIndex),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 1), StorageClass.Function, IntWord(typeDefIndex))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 2), StorageClass.Uniform, IntWord(typeDefIndex))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 3), StorageClass.Input, IntWord(typeDefIndex))),
          Instruction(Op.OpTypeVector, List(ResultRef(typeDefIndex + 4), ResultRef(typeDefIndex), IntWord(2))),
          Instruction(Op.OpTypeVector, List(ResultRef(typeDefIndex + 5), ResultRef(typeDefIndex), IntWord(3))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 6), StorageClass.Function, IntWord(typeDefIndex + 4))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 7), StorageClass.Uniform, IntWord(typeDefIndex + 4))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 8), StorageClass.Input, IntWord(typeDefIndex + 4))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 9), StorageClass.Function, IntWord(typeDefIndex + 5))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 10), StorageClass.Uniform, IntWord(typeDefIndex + 5))),
          Instruction(Op.OpTypeVector, List(ResultRef(typeDefIndex + 11), ResultRef(typeDefIndex), IntWord(4))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 12), StorageClass.Function, IntWord(typeDefIndex + 11))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 13), StorageClass.Uniform, IntWord(typeDefIndex + 11))),
          Instruction(Op.OpTypePointer, List(ResultRef(typeDefIndex + 14), StorageClass.Input, IntWord(typeDefIndex + 11))),
        )
        (code ::: words,
          ctx.copy(
            valueTypeMap = ctx.valueTypeMap ++ Map(
              valType.tag -> typeDefIndex,
              (summon[LTag[Vec2C]].tag.combine(valType.tag)) -> (typeDefIndex + 4),
              (summon[LTag[Vec3C]].tag.combine(valType.tag)) -> (typeDefIndex + 5),
              (summon[LTag[Vec4C]].tag.combine(valType.tag)) -> (typeDefIndex + 11)
            ),
            scalarTypeMap = ctx.scalarTypeMap + (valType -> typeDefIndex),
            funPointerTypeMap = ctx.funPointerTypeMap ++ Map(
              typeDefIndex -> (typeDefIndex + 1),
              (typeDefIndex + 4) -> (typeDefIndex + 6),
              (typeDefIndex + 5) -> (typeDefIndex + 9),
              (typeDefIndex + 11) -> (typeDefIndex + 12)
            ),
            uniformPointerMap = ctx.uniformPointerMap + (
              typeDefIndex -> (typeDefIndex + 2),
              (typeDefIndex + 4) -> (typeDefIndex + 7),
              (typeDefIndex + 5) -> (typeDefIndex + 10),
              (typeDefIndex + 11) -> (typeDefIndex + 13)
            ),
            inputPointerMap = ctx.inputPointerMap + (typeDefIndex -> (typeDefIndex + 3)),
            vectorTypeMap = ctx.vectorTypeMap ++ Map(
              (summon[LTag[Vec2C]].tag.combine(valType.tag)) -> (typeDefIndex + 4),
              (summon[LTag[Vec3C]].tag.combine(valType.tag)) -> (typeDefIndex + 5),
              (summon[LTag[Vec4C]].tag.combine(valType.tag)) -> (typeDefIndex + 11)
            ),
            funVecPointerTypeMap = ctx.funVecPointerTypeMap ++ Map(
              typeDefIndex -> (typeDefIndex + 6),
              (typeDefIndex + 4) -> (typeDefIndex + 6),
              (typeDefIndex + 5) -> (typeDefIndex + 9),
              (typeDefIndex + 11) -> (typeDefIndex + 12)
            ),
            uniformVecPointerMap = ctx.uniformVecPointerMap + (typeDefIndex -> (typeDefIndex + 7)),
            inputVecPointerMap = ctx.inputVecPointerMap + (typeDefIndex -> (typeDefIndex + 8)),
            nextResultId = ctx.nextResultId + 15
          )
        )
    }
  }

  def defineStructTypes(list: List[GStructSchema[_]], context: Context): (List[Words], Context) =
    list.distinctBy(_.structTag).foldLeft((List[Words](), context)) {
      case ((words, ctx), schema) =>
        (words ::: List(
          Instruction(Op.OpTypeStruct, List(
            ResultRef(ctx.nextResultId),
          ) ::: schema.fields.map(_._3).map(t => ctx.valueTypeMap(t.tag)).map(ResultRef.apply)),
          Instruction(Op.OpTypePointer, List(
            ResultRef(ctx.nextResultId + 1),
            StorageClass.Function,
            ResultRef(ctx.nextResultId)
          )),
        ), ctx.copy(
          nextResultId = ctx.nextResultId + 2,
          valueTypeMap = ctx.valueTypeMap + (schema.structTag.tag -> ctx.nextResultId),
          funPointerTypeMap = ctx.funPointerTypeMap + (ctx.nextResultId -> (ctx.nextResultId + 1))
        ))
    }



  def getVectorType(ctx: Context, v: Tag[_], s: Tag[_]) = {
    val vElems = v match {
      case v if v.tag.withoutArgs == Vec2TagWithoutArgs => 2
      case v if v.tag.withoutArgs == Vec3TagWithoutArgs => 3
    }
  }

  def createAndInitBlocks(blocks: List[Tag[_]], in: Boolean, context: Context): (List[Words], List[Words], Context) = {
    val (decoration, definition, newContext) = blocks.foldLeft((List[Words](), List[Words](), context))({ case ((decAcc, insnAcc, ctx), tpe) =>
      val block = ArrayBufferBlock(
        ctx.nextResultId,
        ctx.nextResultId + 1,
        ctx.nextResultId + 2,
        ctx.nextResultId + 3,
        ctx.nextBinding
      )

      val decorationInstructions = List[Words](
        Instruction(Op.OpDecorate, List(
          ResultRef(block.memberArrayTypeRef),
          Decoration.ArrayStride,
          IntWord(typeStride(tpe))
        )), // OpDecorate %_runtimearr_X ArrayStride [typeStride(type)]
        Instruction(Op.OpMemberDecorate, List(
          ResultRef(block.structTypeRef),
          IntWord(0),
          Decoration.Offset,
          IntWord(0)
        )), // OpMemberDecorate %BufferX 0 Offset 0
        Instruction(Op.OpDecorate, List(
          ResultRef(block.structTypeRef),
          Decoration.BufferBlock
        )), // OpDecorate %BufferX BufferBlock
        Instruction(Op.OpDecorate, List(
          ResultRef(block.blockVarRef),
          Decoration.DescriptorSet,
          IntWord(0)
        )), // OpDecorate %_X DescriptorSet 0
        Instruction(Op.OpDecorate, List(
          ResultRef(block.blockVarRef),
          Decoration.Binding,
          IntWord(block.binding)
        )), // OpDecorate %_X Binding [binding]
      )

      val definitionInstructions = List[Words](
        Instruction(Op.OpTypeRuntimeArray, List(
          ResultRef(block.memberArrayTypeRef),
          IntWord(context.valueTypeMap(tpe.tag))
        )), // %_runtimearr_X = OpTypeRuntimeArray %[typeOf(tpe)]
        Instruction(Op.OpTypeStruct, List(
          ResultRef(block.structTypeRef),
          IntWord(block.memberArrayTypeRef)
        )), // %BufferX = OpTypeStruct %_runtimearr_X
        Instruction(Op.OpTypePointer, List(
          ResultRef(block.blockPointerRef),
          StorageClass.Uniform,
          ResultRef(block.structTypeRef)
        )), // %_ptr_Uniform_BufferX= OpTypePointer Uniform %BufferX
        Instruction(Op.OpVariable, List(
          ResultRef(block.blockPointerRef),
          ResultRef(block.blockVarRef),
          StorageClass.Uniform,
        )), // %_X = OpVariable %_ptr_Uniform_X Uniform
      )

      val contextWithBlock = if (in) ctx.copy(
        inBufferBlocks = block :: ctx.inBufferBlocks
      ) else ctx.copy(
        outBufferBlocks = block :: ctx.outBufferBlocks
      )
      (decAcc ::: decorationInstructions, insnAcc ::: definitionInstructions, contextWithBlock.copy(
        nextResultId = contextWithBlock.nextResultId + 5,
        nextBinding = contextWithBlock.nextBinding + 1
      ))
    })
    (decoration, definition, newContext)
  }

  def defineVoids(context: Context): (List[Words], Context) = {
    val voidDef = List[Words](
      Instruction(Op.OpTypeVoid, List(ResultRef(TYPE_VOID_REF))),
      Instruction(Op.OpTypeFunction, List(ResultRef(VOID_FUNC_TYPE_REF), ResultRef(TYPE_VOID_REF)))
    )
    val ctxWithVoid = context.copy(
      voidTypeRef = TYPE_VOID_REF,
      voidFuncTypeRef = VOID_FUNC_TYPE_REF,
    )
    (voidDef, ctxWithVoid)
  }

  def initAndDecorateUniforms(ins: List[Tag[_]], outs: List[Tag[_]], context: Context): (List[Words], List[Words], Context) = {
    val (inDecor, inDef, inCtx) = createAndInitBlocks(ins, in = true, context)
    val (outDecor, outDef, outCtx) = createAndInitBlocks(outs, in = false, inCtx)
    val (voidsDef, voidCtx) = defineVoids(outCtx)
    (inDecor ::: outDecor, voidsDef ::: inDef ::: outDef, voidCtx)
  }

  def createInvocationId(context: Context): (List[Words], Context) = {
    val definitionInstructions = List(
      Instruction(Op.OpConstant, List(ResultRef(context.scalarTypeMap(UInt32Tag)), ResultRef(context.nextResultId + 0), IntWord(localSizeX))),
      Instruction(Op.OpConstant, List(ResultRef(context.scalarTypeMap(UInt32Tag)), ResultRef(context.nextResultId + 1), IntWord(localSizeY))),
      Instruction(Op.OpConstant, List(ResultRef(context.scalarTypeMap(UInt32Tag)), ResultRef(context.nextResultId + 2), IntWord(localSizeZ))),
      Instruction(Op.OpConstantComposite, List(
        IntWord(context.vectorTypeMap(summon[Tag[Vec3[UInt32]]].tag)),
        ResultRef(GL_WORKGROUP_SIZE_REF),
        ResultRef(context.nextResultId + 0),
        ResultRef(context.nextResultId + 1),
        ResultRef(context.nextResultId + 2)
      )),
    )
    (definitionInstructions, context.copy(nextResultId = context.nextResultId + 3))
  }

  def toWord(tpe: Tag[_], value: Any): Words = tpe match {
    case t if t == Int32Tag =>
      IntWord(value.asInstanceOf[Int])
    case t if t == UInt32Tag =>
      IntWord(value.asInstanceOf[Int]) // todo maybe handle pseudo-unsigned from Scala
    case t if t == Float32Tag =>
      val fl = value match {
        case fl: Float => fl
        case dl: Double => dl.toFloat
        case il: Int => il.toFloat
      }
      Word(intToBytes(java.lang.Float.floatToIntBits(fl)).reverse.toArray)
  }

  def defineConstants(exprs: List[DigestedExpression], ctx: Context): (List[Words], Context) = {
    val consts = (exprs.collect {
      case DigestedExpression(_, c@Const(x), _, _) =>
        (c.tag, x)
    } ::: List((Int32Tag, 0), (UInt32Tag, 0))).distinct.filterNot(_._1 == GBooleanTag)
    val (insns, newC) = consts.foldLeft((List[Words](), ctx)) {
      case ((instructions, context), const) =>
        val insn = Instruction(Op.OpConstant, List(
          ResultRef(context.scalarTypeMap(const._1)),
          ResultRef(context.nextResultId),
          toWord(const._1, const._2)
        ))
        val ctx = context.copy(
          constRefs = context.constRefs + (const -> context.nextResultId),
          nextResultId = context.nextResultId + 1
        )
        (instructions :+ insn, ctx)
    }
    val withBool = insns ::: List(
      Instruction(Op.OpConstantTrue, List(
        ResultRef(ctx.scalarTypeMap(GBooleanTag)),
        ResultRef(newC.nextResultId)
      )),
      Instruction(Op.OpConstantFalse, List(
        ResultRef(ctx.scalarTypeMap(GBooleanTag)),
        ResultRef(newC.nextResultId + 1)
      ))
    )
    (withBool, newC.copy(
      nextResultId = newC.nextResultId + 2,
      constRefs = newC.constRefs ++ Map(
        (GBooleanTag, true) -> (newC.nextResultId),
        (GBooleanTag, false) -> (newC.nextResultId + 1)
      )
    ))
  }

  def defineVars(ctx: Context): (List[Words], Context) = {
    (
      List(Instruction(
        Op.OpVariable,
        List(
          ResultRef(ctx.inputVecPointerMap(ctx.scalarTypeMap(Int32Tag))),
          ResultRef(GL_GLOBAL_INVOCATION_ID_REF),
          StorageClass.Input
        ))),
      ctx.copy()
    )
  }

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
        ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
        ResultRef(ctx.exprRefs(expr.dependencies(1).digest))
      ))
    )
    val updatedContext = ctx.copy(
      exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
      nextResultId = ctx.nextResultId + 1
    )
    (instructions, updatedContext)

  private def compileConvertExpression(expr: DigestedExpression, cexpr: ConvertExpression[_, _], ctx: Context): (List[Instruction], Context) =
    val tpe = cexpr.tag
    val typeRef = ctx.scalarTypeMap(tpe)
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
        ResultRef(ctx.exprRefs(expr.dependencies(0).digest))
      )))
    val updatedContext = ctx.copy(
        exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
        nextResultId = ctx.nextResultId + 1
      )
    (instructions, updatedContext)

  private val fnOpMap: Map[FunctionName, Code] = Map(
    Functions.Sin -> GlslOp.Sin,
    Functions.Cos -> GlslOp.Cos,
    Functions.Tan -> GlslOp.Tan,
    Functions.Len2 -> GlslOp.Length,
    Functions.Len3 -> GlslOp.Length,
    Functions.Pow -> GlslOp.Pow,
    Functions.Smoothstep -> GlslOp.SmoothStep,
    Functions.Sqrt -> GlslOp.Sqrt,
    Functions.Cross -> GlslOp.Cross,
    Functions.Clamp -> GlslOp.FClamp,
    Functions.Mix -> GlslOp.FMix,
    Functions.Abs -> GlslOp.FAbs,
    Functions.Atan -> GlslOp.Atan,
    Functions.Acos -> GlslOp.Acos,
    Functions.Asin -> GlslOp.Asin,
    Functions.Atan2 -> GlslOp.Atan2
  )

  def compileFunctionCall(expr: DigestedExpression, call: Expression.FunctionCall[_], ctx: Context): (List[Instruction], Context) =
    val fnOp = fnOpMap(call.fn)
    val tp = call.tag
    val typeRef = if(tp.tag <:< summon[Tag[Vec[_]]].tag.withoutArgs) {
      ctx.vectorTypeMap(tp.tag)
    } else {
      ctx.scalarTypeMap(tp)
    }
    val instructions = List(
      Instruction(Op.OpExtInst, List(
        ResultRef(typeRef),
        ResultRef(ctx.nextResultId),
        ResultRef(GLSL_EXT_REF),
        fnOp
      ) ::: expr.dependencies.map(d => ResultRef(ctx.exprRefs(d.digest)))
    ))
    val updatedContext = ctx.copy(
      exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
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
    val typeRef = ctx.scalarTypeMap(tpe)
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
      ) ::: expr.dependencies.map(d => ResultRef(ctx.exprRefs(d.digest))))
    )
    val updatedContext = ctx.copy(
      exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
      nextResultId = ctx.nextResultId + 1
    )
    (instructions, updatedContext)

  def compileBlock(sortedTree: List[DigestedExpression], ctx: Context): (List[Words], Context) = {
    @tailrec
    def compileExpressions(exprs: List[DigestedExpression], ctx: Context, acc: List[Words]): (List[Words], Context) = {
      if (exprs.isEmpty) (acc, ctx)
      else {
        val expr = exprs.head
        if(ctx.exprRefs.contains(expr.digest)) {
          compileExpressions(exprs.tail, ctx, acc)
        } else {
          val (instructions, updatedCtx) = expr.expr match {
            case c@Const(x) =>
              val constRef = ctx.constRefs((c.tag, x))
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> constRef)
              )
              (List(), updatedContext)

            case d@Dynamic("worker_index") =>
              (Nil, ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.workerIndexRef),
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
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case bo: BitwiseOpExpression[_] =>
              compileBitwiseExpression(expr, bo, ctx)

            case and: And =>
              val instructions = List(
                Instruction(Op.OpLogicalAnd, List(
                  ResultRef(ctx.scalarTypeMap(GBooleanTag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case or: Or =>
              val instructions = List(
                Instruction(Op.OpLogicalOr, List(
                  ResultRef(ctx.scalarTypeMap(GBooleanTag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case not: Not =>
              val instructions = List(
                Instruction(Op.OpLogicalNot, List(
                  ResultRef(ctx.scalarTypeMap(GBooleanTag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case sp: ScalarProd[_, _] =>
              val instructions = List(
                Instruction(Op.OpVectorTimesScalar, List(
                  ResultRef(ctx.vectorTypeMap(sp.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).digest)))
                ))
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case dp: DotProd[_, _] =>
              val instructions = List(
                Instruction(Op.OpDot, List(
                  ResultRef(ctx.scalarTypeMap(dp.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case co: ComparisonOpExpression[_] =>
              val (intOp, floatOp) = comparisonOp(co)
              val op = if (co.operandTag.tag <:< summon[Tag[FloatType]].tag) floatOp else intOp
              val instructions = List(
                Instruction(op, List(
                  ResultRef(ctx.scalarTypeMap(GBooleanTag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)


            case e: ExtractScalar[_, _] =>
              val instructions = List(
                Instruction(Op.OpVectorExtractDynamic, List(
                  ResultRef(ctx.scalarTypeMap(e.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case composeVec2: ComposeVec2[_] =>
              val instructions = List(
                Instruction(Op.OpCompositeConstruct, List(
                  ResultRef(ctx.vectorTypeMap(composeVec2.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case composeVec3: ComposeVec3[_] =>
              val instructions = List(
                Instruction(Op.OpCompositeConstruct, List(
                  ResultRef(ctx.vectorTypeMap(composeVec3.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(2).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case composeVec4: ComposeVec4[_] =>
              val instructions = List(
                Instruction(Op.OpCompositeConstruct, List(
                  ResultRef(ctx.vectorTypeMap(composeVec4.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies(0).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(1).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(2).digest)),
                  ResultRef(ctx.exprRefs(expr.dependencies(3).digest))
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (instructions, updatedContext)

            case fc: FunctionCall[_] =>
              compileFunctionCall(expr, fc, ctx)

            case ga@GArrayElem(index, i) =>
              val instructions = List(
                Instruction(Op.OpAccessChain, List(
                  ResultRef(ctx.uniformPointerMap(ctx.valueTypeMap(ga.tag.tag))),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.inBufferBlocks(index).blockVarRef),
                  ResultRef(ctx.constRefs((Int32Tag, 0))),
                  ResultRef(ctx.exprRefs(expr.dependencies.head.digest))
                )),
                Instruction(Op.OpLoad, List(
                  IntWord(ctx.valueTypeMap(ga.tag.tag)),
                  ResultRef(ctx.nextResultId + 1),
                  ResultRef(ctx.nextResultId)
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> (ctx.nextResultId + 1)),
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
                  case (f, i) => ResultRef(ctx.exprRefs(expr.dependencies(i).digest))
                })
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (insns, updatedContext)

            case gf: GetField[_, _] =>
              val insns: List[Instruction] = List(
                Instruction(Op.OpCompositeExtract, List(
                  ResultRef(ctx.valueTypeMap(gf.tag.tag)),
                  ResultRef(ctx.nextResultId),
                  ResultRef(ctx.exprRefs(expr.dependencies.head.digest)),
                  IntWord(gf.fieldIndex)
                ))
              )
              val updatedContext = ctx.copy(
                exprRefs = ctx.exprRefs + (expr.digest -> ctx.nextResultId),
                nextResultId = ctx.nextResultId + 1
              )
              (insns, updatedContext)

            case ph: PhantomExpression[_] => (List(), ctx)
          }
          compileExpressions(exprs.tail, updatedCtx, acc ::: instructions)
        }
      }
    }
    compileExpressions(sortedTree, ctx, Nil)
  }

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
          BlockBuilder.buildBlock(elseCode),
          ctx
        )
        val elseWithStore = elseInstructions :+ Instruction(Op.OpStore, List(
          ResultRef(resultVar),
          ResultRef(elseCtx.exprRefs(elseCode.digest))
        ))
        (elseWithStore, elseCtx)
      case (caseWhen :: cTail, tCode :: tTail) =>
        val (whenInstructions, whenCtx) = compileBlock(
          BlockBuilder.buildBlock(caseWhen),
          ctx
        )
        val (thenInstructions, thenCtx) = compileBlock(
          BlockBuilder.buildBlock(tCode),
          whenCtx.nested
        )
        val thenWithStore = thenInstructions :+ Instruction(Op.OpStore, List(
          ResultRef(resultVar),
          ResultRef(thenCtx.exprRefs(tCode.digest))
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
              ResultRef(postCtx.exprRefs(caseWhen.digest)),
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
          postCtx.copy(nextResultId = elseCtx.nextResultId)
        )
    }

    val resultVar = ctx.nextResultId
    val resultLoaded = ctx.nextResultId + 1
    val resultTypeTag =  ctx.valueTypeMap(when.tag.tag)
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
    (instructions, caseCtx.copy(exprRefs = caseCtx.exprRefs + (expr.digest -> resultLoaded)))
  }

  def bubbleUpVars(exprs: List[Words]) = {
    val (vars, notVars) = exprs.partition {
      case Instruction(Op.OpVariable, _) => true
      case _ => false
    }
    vars ::: notVars
  }

  def compileMain(sortedTree: List[DigestedExpression], resultType: Tag[_], ctx: Context): (List[Words], Context) = {
    val init = List(
      Instruction(Op.OpFunction, List(
        ResultRef(ctx.voidTypeRef),
        ResultRef(MAIN_FUNC_REF),
        SamplerAddressingMode.None,
        ResultRef(VOID_FUNC_TYPE_REF)
      )),
      Instruction(Op.OpLabel, List(
        ResultRef(ctx.nextResultId)
      )),
    )

    val initWorkerIndex = List(
      Instruction(Op.OpAccessChain, List(
        ResultRef(ctx.inputPointerMap(ctx.scalarTypeMap(Int32Tag))),
        ResultRef(ctx.nextResultId + 1),
        ResultRef(GL_GLOBAL_INVOCATION_ID_REF),
        ResultRef(ctx.constRefs(Int32Tag, 0))
      )),
      Instruction(Op.OpLoad, List(
        ResultRef(ctx.scalarTypeMap(Int32Tag)),
        ResultRef(ctx.nextResultId + 2),
        ResultRef(ctx.nextResultId + 1)
      ))
    )

    val (body, codeCtx) = compileBlock(sortedTree, ctx.copy(
      nextResultId = ctx.nextResultId + 3,
      workerIndexRef = ctx.nextResultId + 2
    ))

    val bubbledBody = bubbleUpVars(initWorkerIndex ::: body)

    val end = List(
      Instruction(Op.OpAccessChain, List(
        ResultRef(codeCtx.uniformPointerMap(codeCtx.valueTypeMap(resultType.tag))),
        ResultRef(codeCtx.nextResultId),
        ResultRef(codeCtx.outBufferBlocks(0).blockVarRef),
        ResultRef(codeCtx.constRefs((Int32Tag, 0))),
        ResultRef(codeCtx.workerIndexRef)
      )),

      Instruction(Op.OpStore, List(
        ResultRef(codeCtx.nextResultId),
        ResultRef(codeCtx.exprRefs(sortedTree.last.digest))
      )),
      Instruction(Op.OpReturn, List()),
      Instruction(Op.OpFunctionEnd, List())
    )
    (init ::: bubbledBody ::: end, codeCtx.copy(nextResultId = codeCtx.nextResultId + 1))
  }

  // its a bit weird, but with these two caches it went from 10 minutes to below 1s
  private val allBlocksCache = mutable.Map[Int, List[DigestedExpression]]()
  private var blockI = 0
  def getAllBlocksExprs(root: DigestedExpression): List[DigestedExpression] = {
    val visited = mutable.Set[Int]()
    def getAllBlocksExprsAcc(root: DigestedExpression): List[DigestedExpression] = {
      if (visited.contains(root.expr.treeid)) {
        return Nil
      }
      if (allBlocksCache.contains(root.expr.treeid)) {
        return allBlocksCache(root.expr.treeid)
      }
      val result = List(root).flatMap {
        case d@DigestedExpression(_, _, deps, blockDeps) =>
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
  
  def compile[T <: Value](returnVal: T, inTypes: List[Tag[_]], outTypes: List[Tag[_]]): ByteBuffer = {
    val tree = returnVal.tree
    println("Compiling...")
    println("Digesting...")
    val (digestTree, hash) = Digest.digest(tree)
    println("Digest done!")
    val sorted = BlockBuilder.buildBlock(digestTree)
 
    val allBlockExprs = getAllBlocksExprs(digestTree)
    println("Generated all blocks tree")

    val insnWithHeader = headers()
    val typesInCode = allBlockExprs.map(_.expr.tag)
    val allTypes = (typesInCode ::: inTypes ::: outTypes).distinct
    def scalarTypes = allTypes.filter(_.tag <:< summon[Tag[Scalar]].tag)
    val (typeDefs, typedContext) = defineScalarTypes(scalarTypes)
    val structsInCode = allBlockExprs.map(_.expr).collect {
      case cs: ComposeStruct[_] => cs.resultSchema
      case gf: GetField[_, _] => gf.resultSchema
    }.distinct
    val (structDefs, structCtx) = defineStructTypes(structsInCode, typedContext)
    val (decorations, uniformDefs, uniformContext) = initAndDecorateUniforms(inTypes, outTypes, structCtx)
    val (inputDefs, inputContext) = createInvocationId(uniformContext)
    val (constDefs, constCtx) = defineConstants(allBlockExprs, inputContext)
    val (varDefs, varCtx) = defineVars(constCtx)
//    println("preC")
    val resultType = returnVal.tree.tag
    val (main, finalCtx) = compileMain(sorted, resultType, varCtx)

    val code: List[Words] =
      insnWithHeader :::
        decorations :::
        typeDefs :::
        structDefs :::
        uniformDefs :::
        inputDefs :::
        constDefs :::
        varDefs :::
        main

    val fullCode = code.map {
      case WordVariable(name) if name == BOUND_VARIABLE => IntWord(finalCtx.nextResultId)
      case x => x
    }
//    dumpCode(fullCode)

    val bytes = fullCode.flatMap(_.toWords).toArray

    BufferUtils.createByteBuffer(bytes.length).put(bytes).rewind()
  }

  def hexDumpCode(code: List[Words]): Unit = {
    println(code.flatMap(_.toWords)
      .grouped(4).map(_.map(i => Integer.toHexString(i & 0xFF))
      .mkString(" ")).mkString("\n"))
  }

  def dumpCode(code: List[Words], fn: String => Unit): Unit = {
    fn(code.map(_.toString).mkString("\n"))
  }

  def dumpCode(code: List[Words]): Unit = {
    dumpCode(code, x => println(x))
  }
}
