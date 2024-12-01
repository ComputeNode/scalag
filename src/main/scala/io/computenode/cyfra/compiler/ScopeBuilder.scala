package io.computenode.cyfra.compiler

import Digest.DigestedExpression

object ScopeBuilder {
  def buildScope(tree: DigestedExpression): List[DigestedExpression] = {
    def bfsAcc(curr: List[DigestedExpression], visited: Set[String], acc: List[DigestedExpression]): List[DigestedExpression] = {
      val children = curr.flatMap(_.dependencies)
      if(children.isEmpty) acc
      else {
        bfsAcc(
          children,
          children.map(_.exprId).toSet ++ visited,
          acc ++ children 
        )
      }
    }
    val childrenWithDuplicates = bfsAcc(List(tree), Set(), List())
    val withoutDuplicates = childrenWithDuplicates.reverse.foldLeft((Set[String](), List[DigestedExpression]())){
      case ((visited, acc), next) =>
        if(visited.contains(next.exprId)) (visited, acc) else (visited + next.exprId, acc :+ next)
    }._2.reverse // we can replace it with BFS with inverted edges if it's too slow, but it was the simplest solution
    (tree :: withoutDuplicates).reverse
  }
}
