package io.computenode.cyfra

import Algebra.FromExpr
import Value.GBoolean
import io.computenode.cyfra.compiler.Digest.DigestedExpression
import izumi.reflect.Tag

import java.util.UUID

object Control:
  
  case class Block[T <: Value : Tag](expr: Expression[_]) // to not have it sorted and inlined
  
  case class When[T <: Value: Tag : FromExpr](
    when: GBoolean, 
    thenCode: T,
    otherConds: List[Block[GBoolean]],
    otherCases: List[Block[T]]
  ):
    def elseWhen(cond: GBoolean)(t: T): When[T] =
      When(when, thenCode, otherConds :+ Block(cond.tree), otherCases :+ Block(t.tree))
    def otherwise(t: T): T =
      summon[FromExpr[T]].fromExpr(WhenExpr(when, Block(thenCode.tree), otherConds, otherCases, Block(t.tree)))
 
  case class WhenExpr[T <: Value: Tag](
    when: GBoolean, 
    thenCode: Block[T], 
    otherConds: List[Block[GBoolean]], 
    otherCaseCodes: List[Block[T]],
    otherwise: Block[T]
  ) extends Expression[T]
  
  def when[T <: Value: Tag: FromExpr](cond: GBoolean)(fn: T): When[T] =
    When(cond, fn, Nil, Nil)
    
