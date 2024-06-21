package com.scalag.compiler


import com.scalag.{Expression, Value}

import java.security.MessageDigest
import java.util.Base64

object Digest {

  case class DigestedExpression(digest: String, expr: Expression[_ <: Value], dependencies: List[DigestedExpression])

  def hashToBytes(a: Any): Array[Byte] = {
    val i = a.hashCode()
    Array[Byte] (
      (i >>> 24).asInstanceOf[Byte],
      (i >>> 16).asInstanceOf[Byte],
      (i >>> 8).asInstanceOf[Byte],
      (i >>> 0).asInstanceOf[Byte]
    )
  }

  def digest(tree: Expression[_]): (DigestedExpression, Array[Byte]) = {
    val d = MessageDigest.getInstance("MD5")
    val products = tree.productIterator.toList // non-lazy
    def digestChildren(children: List[Any]): List[DigestedExpression] =
      (for (elem <- children) yield {
        elem match {
          case x: Expression[_] =>
            val (digestedChild, bytes) = digest(x)
            d.update(bytes)
            Some(digestedChild)
          case x: Value =>
            val (digestedChild, bytes) = digest(x.tree)
            d.update(bytes)
            Some(digestedChild)
          case list: List[Any] =>
            digestChildren(list.filter(_.isInstanceOf[Value]).map(_.asInstanceOf[Value].tree))
          case other =>
            val hashBytes = hashToBytes(other)
            d.update(hashBytes)
            None
        }
      }).flatMap(_.iterator.toList)
    val children = digestChildren(products)
    val treeT = tree.getClass.getSimpleName.hashCode // TODO conflicts possible
    val typeBytes = tree.tag.tag.shortName.getBytes()
    d.update(typeBytes)
    d.update(BigInt(treeT).toByteArray)
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
          val mirror = expr.tag
          val exprValType = mirror.tag.shortName
          val prodString = s"$indent- ${exprType} [${exprValType}] [##$hash] ${optNewline(hasExprChildren)}"
          prodString + childrenString + optNewline(!hasExprChildren)
        case any =>
          s"$indent* ${any.toString}\n"
      }
    }
    format(e, 0)
  }
}
