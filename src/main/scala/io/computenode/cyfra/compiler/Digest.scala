package io.computenode.cyfra.compiler

import io.computenode.cyfra.Control.Block
import io.computenode.cyfra.{Expression, GSeq, Value}

import java.security.MessageDigest
import java.util.Base64
import scala.collection.mutable

object Digest {

  trait CustomExprId:
    def exprId: String
    
  // todo make all exprs just digested out of the box
  trait CustomDependencies:
    def dependencies: List[DigestedExpression]
    def blockDeps: List[DigestedExpression]


  case class DigestedExpression(exprId: String, expr: Expression[_ <: Value], dependencies: List[DigestedExpression], blockDeps: List[DigestedExpression])
  
  val treeCache: mutable.Map[Int, DigestedExpression] = mutable.Map.empty[Int, DigestedExpression]
  def digest(tree: Expression[_]): DigestedExpression = {
    if(treeCache.contains(tree.treeid)) return treeCache(tree.treeid)
    val products = tree.productIterator.toList // non-lazy
    def digestChildren(children: List[Any]): (List[DigestedExpression], List[DigestedExpression]) =
      (for (elem <- children) yield {
       elem match {
          case b: Block[_] =>
            val digestedChild = digest(b.expr)
            (None, Some(digestedChild))
          case x: Expression[_] =>
            val digestedChild = digest(x)
            (Some(digestedChild), None)
          case x: Value =>
            val digestedChild = digest(x.tree)
            (Some(digestedChild), None)
          case list: List[Any] =>
            (digestChildren(list.filter(_.isInstanceOf[Value]).map(_.asInstanceOf[Value].tree))._1,
              digestChildren(list.filter(_.isInstanceOf[Block[_]]).map(_.asInstanceOf[Block[_]].expr))._1)
          case _ => (None, None)
        }
      }).foldLeft((List.empty[DigestedExpression], List.empty[DigestedExpression])) {
        case ((acc, blockAcc), (newExprs, newBlocks)) => (acc ::: newExprs.iterator.toList, blockAcc ::: newBlocks.iterator.toList)
      }

    val (children, blockChildren) = tree match {
      case c: CustomDependencies =>
        (c.dependencies, c.blockDeps)
      case _ => digestChildren(products)
    }
    val ds = tree match {
      case c: CustomExprId => c.exprId
      case _ => tree.treeid.toString
    }
    DigestedExpression(ds, tree, children, blockChildren)
  }
  
}
