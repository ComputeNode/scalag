package com.unihogsoft.scalag.compiler
import java.security.MessageDigest
import java.util.Base64

import com.unihogsoft.scalag.dsl.DSL._

object Digest {

  case class DigestedExpression(digest: String, expr: Expression[_ <: ValType], dependencies: List[DigestedExpression])

  def hashToBytes(a: Any): Array[Byte] = {
    val i = a.hashCode()
    Array[Byte] (
      (i >>> 24).asInstanceOf[Byte],
      (i >>> 16).asInstanceOf[Byte],
      (i >>> 8).asInstanceOf[Byte],
      (i >>> 0).asInstanceOf[Byte]
    )
  }

  def digest(tree: Expression[_ <: ValType]): (DigestedExpression, Array[Byte]) = {
    val d = MessageDigest.getInstance("MD5")
    val products = tree.productIterator.toList // non-lazy
    val children = (for(elem <- products) yield {
      elem match {
        case x: E[_] =>
          val (digestedChild, bytes) = digest(x.asInstanceOf[E[ _ <: ValType]])
          d.update(bytes)
          Some(digestedChild)
        case other =>
          val hashBytes = hashToBytes(other)
          d.update(hashBytes)
          None
      }
    }).collect { case Some(expr) => expr }
    val typeBytes = tree.valClassTag.runtimeClass.getSimpleName.getBytes()
    d.update(typeBytes)
    val digestBytes = d.digest()
    val b64 = Base64.getEncoder.encodeToString(digestBytes)
    (DigestedExpression(b64, tree, children), digestBytes)
  }

  def formatTreeWithDigest(e: DigestedExpression): String = {
    def format(value: Any, depth: Int): String = {
      val indent = "  " * depth
      value match {
        case DigestedExpression(hash, expr, deps) =>
          def optNewline(cond: Boolean) = if(cond) "\n" else ""
          val hasExprChildren = expr.productIterator.exists {
            case _: Expression[_] => true
            case _ => false
          }
          val childrenString = if(hasExprChildren) {
            val valDeps = expr.productIterator.toList.filterNot(_.isInstanceOf[Expression[_]])
            (deps ++ valDeps).map(
              p => format(p, depth + 1)
            ).mkString("")
          } else {
            s"(${expr.productIterator.map(_.toString).mkString(", ")})"
          }
          val exprType = expr.getClass.getSimpleName
          val exprValType = expr.valClassTag.runtimeClass.getSimpleName
          val prodString = s"$indent- ${exprType} [${exprValType}] ## $hash ${optNewline(hasExprChildren)}"
          prodString + childrenString + optNewline(!hasExprChildren)
        case any =>
          s"$indent* ${any.toString}\n"
      }
    }
    format(e, 0)
  }
}
