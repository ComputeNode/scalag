package com.unihogsoft.scalag.compiler

import java.nio.ByteBuffer

import shapeless.HList
import com.unihogsoft.scalag.dsl.DSL._
import org.lwjgl.BufferUtils
import Opcodes._
import scala.reflect.runtime.universe._

import scala.util.hashing.MurmurHash3
class DSLCompiler {

  val BOUND_VARIABLE = "bound"
  val GLSL_EXT_NAME = "GLSL.std.450"
  val GLSL_EXT_REF = 1
  val TYPE_VOID_REF = 2
  val VOID_FUNC_TYPE_REF = 3
  val MAIN_FUNC_REF = 4
  val GL_GLOBAL_INVOCATION_ID_REF = 5


  def headers(): List[Words] = {
    Word(Array(0x03, 0x02, 0x23, 0x07)) :: // SPIR-V
    Word(Array(0x00, 0x00, 0x01, 0x00)) :: // Version: 1.0
    Word(Array(0x00, 0x00, 0x00, 0x00)) :: // Generator: null
    WordVariable(BOUND_VARIABLE) :: // Bound: To be calculated
    Word(Array(0x00, 0x00, 0x00, 0x00)) :: // Schema: 0
    Instruction(Op.OpCapability, List(Capability.Shader)) :: // OpCapability Shader
    Instruction(Op.OpExtInstImport, List(ResultRef(GLSL_EXT_REF), Text(GLSL_EXT_NAME))) :: // OpExtInstImport "GLSL.std.450"
    Instruction(Op.OpMemoryModel, List(AddressingModel.Logical, MemoryModel.GLSL450)) :: // OpMemoryModel Logical GLSL450
    Instruction(Op.OpEntryPoint, List(
      ExecutionModel.GLCompute, ResultRef(MAIN_FUNC_REF), Text("main"), ResultRef(GL_GLOBAL_INVOCATION_ID_REF))
    ) :: // OpEntryPoint GLCompute %MAIN_FUNC_REF "main" %GL_GLOBAL_INVOCATION_ID_REF
    Instruction(Op.OpExecutionMode, List(
      ResultRef(MAIN_FUNC_REF), ExecutionMode.LocalSize, IntWord(128), IntWord(1), IntWord(1))
    ) :: // OpExecutionMode %4 LocalSize 128 1 1
    Instruction(Op.OpSource, List(SourceLanguage.GLSL, IntWord(450))) :: // OpSource GLSL 450
    Instruction(Op.OpDecorate, List(
      ResultRef(GL_GLOBAL_INVOCATION_ID_REF), Decoration.BuiltIn, BuiltIn.GlobalInvocationId)
    ) :: // OpDecorate %GL_GLOBAL_INVOCATION_ID_REF BuiltIn GlobalInvocationId
    Nil
  }


  case class Context(
    scalarTypeMap: Map[TypeTag[_], Int],
    pointerTypeMap: Map[Int, Int],
    nextResultId: Int
  )

  def initialContext: Context = Context(Map(), Map(), 0)

  val scalarTypeDefInsn = Map[TypeTag[_], Instruction](
    typeTag[Int32] -> Instruction(Op.OpTypeInt, List(WordVariable("result"), IntWord(32), IntWord(0))),
    typeTag[Float32] -> Instruction(Op.OpTypeFloat, List(WordVariable("result"), IntWord(32), IntWord(0)))
  )


  def defineTypes(types: List[TypeTag[_]]): (List[Words], Context) = {
    types.distinct.foldLeft((List[Words](), initialContext)) {
      case ((words, ctx), valType) =>
        val typeDefIndex = ctx.nextResultId
        val code = List(
          scalarTypeDefInsn(valType).replaceVar("result", typeDefIndex),
          Instruction(Op.OpTypePointer, List(IntWord(typeDefIndex + 1), StorageClass.Function, IntWord(typeDefIndex)))
        )
        (code ::: words,
          ctx.copy(
            scalarTypeMap = ctx.scalarTypeMap + (valType -> typeDefIndex),
            pointerTypeMap = ctx.pointerTypeMap + ((typeDefIndex + 1) -> typeDefIndex),
            nextResultId = ctx.nextResultId + 2
          )
        )
    }
  }

  def compile[T<: ValType](returnVal: T, inTypes: List[TypeTag[_]], outTypes: List[TypeTag[_]]): ByteBuffer = {
    val tree = returnVal.tree
    val (digestTree, hash) = Digest.digest(tree)
    val sorted = TopologicalSort.sortTree(digestTree)
    println(Digest.formatTreeWithDigest(digestTree))
    println()
    println(sorted.mkString("\n"))

    val insnWithHeader = headers()
    val (typeDefs, context) = defineTypes(inTypes ::: outTypes)
    hexDumpCode(typeDefs)
    BufferUtils.createByteBuffer(1)
  }

  def hexDumpCode(code: List[Words]): Unit = {
    println(code.flatMap(_.toWords)
      .grouped(4).map(_.map(i => Integer.toHexString(i & 0xFF))
      .mkString(" ")).mkString("\n"))
  }

}
