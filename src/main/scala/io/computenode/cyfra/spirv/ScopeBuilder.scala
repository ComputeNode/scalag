package io.computenode.cyfra.spirv

import io.computenode.cyfra.dsl.Expression.E
import io.computenode.cyfra.dsl.Value

import scala.collection.mutable


private[cyfra] object ScopeBuilder:

  def buildScope(tree: E[_]): List[E[_]] =

    val inDegrees = mutable.Map[Int, Int]()
    val q = mutable.Queue[E[_]]()
    q.enqueue(tree)
    while q.nonEmpty do
      val curr = q.dequeue()
      curr match
        case expr: E[_] =>
          val children = expr.exprDependencies
          children.foreach: child =>
            val childId = child.treeid
            inDegrees(childId) = inDegrees.getOrElse(childId, 0) + 1
            q.enqueue(child)
        case v: Value =>
          q.enqueue(v.tree)

    val l = mutable.ListBuffer[E[_]]()
    val roots = mutable.Set[E[_]](tree)

    while roots.nonEmpty do
      val curr: E[_] = roots.head
      roots.remove(curr)
      l += curr
      curr.exprDependencies.foreach: child =>
        val childId = child.treeid
        inDegrees(childId) -= 1
        if inDegrees(childId) == 0 then
          roots += child

    assert(inDegrees.valuesIterator.forall(_ == 0), "Cycle detected in the graph")
    l.toList.reverse

