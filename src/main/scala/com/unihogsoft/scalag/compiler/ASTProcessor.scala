package com.unihogsoft.scalag.compiler

import java.security.MessageDigest

import com.unihogsoft.scalag.dsl.DSL._
import com.unihogsoft.scalag.compiler.Opcodes._

class ASTProcessor {

  val header = List(
      MagicNumber,
      Version,
      Generator
    )

 case class Program(idBound: Int, insnList: List[Instruction])



 def process(ast: Expression[_]): Program = {
   val program = Program(0, Nil)
   program
 }
}