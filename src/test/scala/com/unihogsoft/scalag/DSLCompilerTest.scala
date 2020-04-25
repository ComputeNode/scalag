package com.unihogsoft.scalag

import com.unihogsoft.scalag.compiler.Digest.DigestedExpression
import com.unihogsoft.scalag.compiler.{Digest, TopologicalSort}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.unihogsoft.scalag.dsl.DSL._

class DSLCompilerTest extends AnyFlatSpec with Matchers {
  "DSL Compiler" should "sort expressions topologically" in {
    val prev = Int32(Const(1)) + 1
    val next = Int32(Const(2)) + 1
    val other = Int32(Const(4)) - Int32(Const(1))
    val value = ((prev / 2) + ((prev + 1) * 2) + next / prev) * other

    val (digestTree, hash) = Digest.digest(value.tree)
    val sorted = TopologicalSort.sortTree(digestTree)
    val withDeps = sorted.map(withDependencies)
    println(Digest.formatTreeWithDigest(digestTree))
    println(withDeps.mkString("\n"))
    var initialized = Set[String]()
    for(expr <- withDeps) {
      assert(expr.deps.forall(initialized.contains))
      initialized = initialized + expr.expr
    }
  }

  case class ExprWidthDeps(expr: String, deps: Set[String])
  def withDependencies(expr: DigestedExpression): ExprWidthDeps = {
    ExprWidthDeps(
      expr.digest,
      allDeps(expr).toSet
    )
  }

  def allDeps(expr: DigestedExpression): List[String] = {
    if(expr.dependencies.isEmpty) List()
    else expr.dependencies.map(_.digest) ::: expr.dependencies.flatMap(allDeps)
  }
}
