package io.computenode.cyfra

import Algebra.FromExpr
import Value.GBoolean
import io.computenode.cyfra.compiler.Digest.DigestedExpression
import izumi.reflect.Tag

import java.util.UUID

object Control:
  
  case class Scope[T <: Value : Tag](expr: Expression[_]) // to not have it sorted and inlined
  
  case class When[T <: Value: Tag : FromExpr](
    when: GBoolean, 
    thenCode: T,
    otherConds: List[Scope[GBoolean]],
    otherCases: List[Scope[T]]
  ):
    def elseWhen(cond: GBoolean)(t: T): When[T] =
      When(when, thenCode, otherConds :+ Scope(cond.tree), otherCases :+ Scope(t.tree))
    def otherwise(t: T): T =
      summon[FromExpr[T]].fromExpr(WhenExpr(when, Scope(thenCode.tree), otherConds, otherCases, Scope(t.tree)))
 
  case class WhenExpr[T <: Value: Tag](
    when: GBoolean, 
    thenCode: Scope[T], 
    otherConds: List[Scope[GBoolean]], 
    otherCaseCodes: List[Scope[T]],
    otherwise: Scope[T]
  ) extends Expression[T]
  
  def when[T <: Value: Tag: FromExpr](cond: GBoolean)(fn: T): When[T] =
    When(cond, fn, Nil, Nil)
    
