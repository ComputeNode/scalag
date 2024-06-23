package com.scalag

import Algebra.*
import Value.*
import izumi.reflect.Tag
import Algebra.*
import Algebra.given
import com.scalag.Expression.ConstInt32
import com.scalag.GSeq.*
import com.scalag.compiler.Digest
import com.scalag.compiler.Digest.{CustomDependencies, CustomDigest, DigestedExpression}

import java.util.Base64
import scala.util.Random

case class GSeq[T <: Value : Tag : FromExpr](
  source: GSeqStream[_],
  elemOps: List[GSeq.ElemOp[_]],
  limit: Option[Int]
):
  private def currentElem: T = summon[FromExpr[T]].fromExpr(CurrentElem[T]())
  private def aggregateElem[R <: Value : Tag: FromExpr]: R = summon[FromExpr[R]].fromExpr(AggregateElem[R]())

  def map[R <: Value : Tag : FromExpr](fn: T => R): GSeq[R] =
    this.copy[R](elemOps = elemOps :+ GSeq.MapOp[T,R](
      fn(currentElem).tree
    ))

  def filter(fn: T => GBoolean): GSeq[T] =
    this.copy(elemOps = elemOps :+ GSeq.FilterOp(
      fn(currentElem).tree
    ))

  def takeUntil(fn: T => GBoolean): GSeq[T] =
    this.copy(elemOps = elemOps :+ GSeq.TakeUntilOp(
      fn(currentElem).tree
    ))

  def limit(n: Int): GSeq[T] =
    this.copy(limit = Some(n))

  def fold[R <: Value : Tag : FromExpr](zero: R, fn: (R, T) => R): R =
    summon[FromExpr[R]].fromExpr(GSeq.FoldSeq(zero, fn(aggregateElem, currentElem).tree, this))

  def count: Int32 =
    fold(0, (acc: Int32, _: T) => acc + 1)
    


object GSeq:

  def gen[T <: Value : Tag : FromExpr](first: T, next: T => T) = GSeq(
    GSeqStream(first, next(summon[FromExpr[T]].fromExpr(CurrentElem[T]())).tree),
    Nil,
    None
  )

  val CurrentElemDigestBytes = Random(2137).nextBytes(64)
  val CurrentElemDigest = Base64.getEncoder.encodeToString(CurrentElemDigestBytes)
  case class CurrentElem[T <: Value : Tag]() extends PhantomExpression[T] with CustomDigest:
    def digest: Array[Byte] = CurrentElemDigestBytes

  val AggregateElemDigestBytes = Random(2138).nextBytes(64)
  val AggregateElemDigest = Base64.getEncoder.encodeToString(AggregateElemDigestBytes)
  case class AggregateElem[T <: Value : Tag]() extends PhantomExpression[T] with CustomDigest:
    def digest: Array[Byte] = AggregateElemDigestBytes

  sealed trait ElemOp[T <: Value : Tag]:
    def tag: Tag[T] = summon[Tag[T]]

  case class MapOp[T <: Value: Tag, R <: Value: Tag](fn: Expression[_]) extends ElemOp[R]
  case class FilterOp[T <: Value: Tag](fn: Expression[GBoolean]) extends ElemOp[T]
  case class TakeUntilOp[T <: Value: Tag](fn: Expression[GBoolean]) extends ElemOp[T]

  sealed trait GSeqSource[T <: Value: Tag]
  case class GSeqStream[T <: Value: Tag](init: T, next: Expression[_]) extends GSeqSource[T]
  
  case class FoldSeq[R <: Value : Tag, T <: Value : Tag](zero: R, fn: Expression[_], seq: GSeq[T]) extends Expression[R] with CustomDependencies:
    val (zeroExpr, zeroDigest) = Digest.digest(zero.tree)
    val (fnExpr, fnDigest) = Digest.digest(fn)
    val (streamInitExpr, streamInitDigest) = Digest.digest(seq.source.init.tree)
    val (streamNextExpr, streamNextDigest) = Digest.digest(seq.source.next)
    val (seqExprs, seqDigest) = seq.elemOps.map {
      case MapOp(fn) => Digest.digest(fn)
      case FilterOp(fn) => Digest.digest(fn)
      case TakeUntilOp(fn) => Digest.digest(fn)
    }.foldLeft((List.empty[DigestedExpression], Array.empty[Byte])) {
      case ((exprs, digest), (expr, d)) => (exprs :+ expr, Digest.combineDigest(List(digest, d)))
    }
    val (limitExpr, limitDigest) = Digest.digest(
      ConstInt32(seq.limit.getOrElse(throw new IllegalArgumentException("Reduce on infinite stream is not supported")))
    )
    val dependencies: List[DigestedExpression] = List(zeroExpr, streamInitExpr, limitExpr)
    val blockDeps: List[DigestedExpression] = fnExpr :: streamNextExpr :: seqExprs
    val digest: Array[Byte] = Digest.combineDigest(List(
      zeroDigest, fnDigest, streamInitDigest, streamNextDigest, seqDigest, limitDigest
    ))





