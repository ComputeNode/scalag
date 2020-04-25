package com.unihogsoft.scalag.compiler

import com.unihogsoft.scalag.compiler.Digest.DigestedExpression

object TopologicalSort {
  def sortTree(tree: DigestedExpression): List[DigestedExpression] = {
    def bfsAcc(curr: List[DigestedExpression], visited: Set[String], acc: List[DigestedExpression]): List[DigestedExpression] = {
      val children = curr.flatMap(_.dependencies)
      if(children.isEmpty) acc
      else {
        bfsAcc(
          children,
          children.map(_.digest).toSet ++ visited,
          acc ++ children
        )
      }
    }
    val childrenWithDuplicates = bfsAcc(List(tree), Set(), List())
    val withoutDuplicates = childrenWithDuplicates.reverse.foldLeft((Set[String](), List[DigestedExpression]())){
      case ((visited, acc), next) =>
        if(visited.contains(next.digest)) (visited, acc) else (visited + next.digest, acc :+ next)
    }._2.reverse // we can replace it with BFS with inverted edges if it's too slow, but it was the simplest solution
    (tree :: withoutDuplicates).reverse
  }
}
