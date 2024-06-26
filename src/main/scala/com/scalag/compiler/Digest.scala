package com.scalag.compiler


import com.scalag.Control.Block
import com.scalag.{Expression, GSeq, Value}

import java.security.MessageDigest
import java.util.Base64
import scala.collection.mutable

object Digest {

  trait CustomDigest:
    def digest: Array[Byte]
    
  // todo maybe make all exprs just digested out of the box?
  trait CustomDependencies extends CustomDigest:
    def dependencies: List[DigestedExpression]
    def blockDeps: List[DigestedExpression]


  case class DigestedExpression(digest: String, expr: Expression[_ <: Value], dependencies: List[DigestedExpression], blockDeps: List[DigestedExpression])

  def hashToBytes(a: Any): Array[Byte] = {
    val i = a.hashCode()
    Array[Byte] (
      (i >>> 24).asInstanceOf[Byte],
      (i >>> 16).asInstanceOf[Byte],
      (i >>> 8).asInstanceOf[Byte],
      (i >>> 0).asInstanceOf[Byte]
    )
  }
  
  def combineDigest(d: List[Array[Byte]]): Array[Byte] = {
    val md = MessageDigest.getInstance("MD5")
    d.foreach(md.update)
    md.digest()
  }
  
  val treeCache: mutable.Map[Int, (DigestedExpression, Array[Byte])] = mutable.Map.empty[Int, (DigestedExpression, Array[Byte])]
  def digest(tree: Expression[_]): (DigestedExpression, Array[Byte]) = {
    if(treeCache.contains(tree.treeid)) return treeCache(tree.treeid)
    val products = tree.productIterator.toList // non-lazy
    def digestChildren(children: List[Any]): (List[DigestedExpression], List[DigestedExpression]) =
      (for (elem <- children) yield {
       elem match {
          case b: Block[_] =>
            val (digestedChild, bytes) = digest(b.expr)
            (None, Some(digestedChild))
          case x: Expression[_] =>
            val (digestedChild, bytes) = digest(x)
            (Some(digestedChild), None)
          case x: Value =>
            val (digestedChild, bytes) = digest(x.tree)
            (Some(digestedChild), None)
          case list: List[Any] =>
            (digestChildren(list.filter(_.isInstanceOf[Value]).map(_.asInstanceOf[Value].tree))._1,
              digestChildren(list.filter(_.isInstanceOf[Block[_]]).map(_.asInstanceOf[Block[_]].expr))._1)
          case _ => (None, None)
        }
      }).foldLeft((List.empty[DigestedExpression], List.empty[DigestedExpression])) {
        case ((acc, blockAcc), (newExprs, newBlocks)) => (acc ::: newExprs.iterator.toList, blockAcc ::: newBlocks.iterator.toList)
      } // todo better structure for lists

    val (children, blockChildren) = tree match {
      case c: CustomDependencies =>
        (c.dependencies, c.blockDeps)
      case _ => digestChildren(products)
    }
    val digestBytes = tree match {
      case c: CustomDigest => c.digest
      case _ => BigInt(tree.treeid).toByteArray
    }
    val ds = tree match {
      case c: CustomDigest => Base64.getEncoder.encodeToString(c.digest)
      case _ => Base64.getEncoder.encodeToString(digestBytes)
    }
    val result = (DigestedExpression(ds, tree, children, blockChildren), digestBytes)
    treeCache.put(tree.treeid, result)
    result
  }

  def formatTreeWithDigest(e: DigestedExpression): String = {
    def format(value: Any, depth: Int): String = {
      val indent = "  " * depth
      value match {
        case DigestedExpression(hash, expr, deps, blockDeps) =>
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
