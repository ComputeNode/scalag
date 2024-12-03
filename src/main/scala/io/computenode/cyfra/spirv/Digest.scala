package io.computenode.cyfra.spirv

import io.computenode.cyfra.dsl.Control.Scope
import io.computenode.cyfra.dsl.{Expression, GSeq, Value}

import java.security.MessageDigest
import java.util.Base64
import scala.collection.mutable

private[cyfra] object Digest {

  private[cyfra] trait CustomExprId:
    def exprId: String
    
  // todo make all exprs just digested out of the box
  private[cyfra] trait CustomDependencies:
    def dependencies: List[DigestedExpression]
    def blockDeps: List[DigestedExpression]


  private[cyfra] case class DigestedExpression(exprId: String, expr: Expression[_ <: Value], dependencies: List[DigestedExpression], blockDeps: List[DigestedExpression], name: String)
  
  val treeCache: mutable.Map[Int, DigestedExpression] = mutable.Map.empty[Int, DigestedExpression]
  def digest(tree: Expression[_], name: String = "value"): DigestedExpression = {
    if(treeCache.contains(tree.treeid)) return treeCache(tree.treeid)
    val products = tree.productIterator.toList // non-lazy
    def digestChildren(children: List[Any]): (List[DigestedExpression], List[DigestedExpression]) =
      (for (elem <- children) yield {
       elem match {
          case b: Scope[_] =>
            val digestedChild = digest(b.expr)
            (None, Some(digestedChild))
          case x: Expression[_] =>
            val digestedChild = digest(x)
            (Some(digestedChild), None)
          case x: Value =>
            val digestedChild = digest(x.tree, x.name.value)
            (Some(digestedChild), None)
          case list: List[Any] =>
            (digestChildren(list.filter(_.isInstanceOf[Value]).map(_.asInstanceOf[Value]))._1,
              digestChildren(list.filter(_.isInstanceOf[Scope[_]]).map(_.asInstanceOf[Scope[_]].expr))._1)
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
    DigestedExpression(ds, tree, children, blockChildren, name)
  }
  
}
