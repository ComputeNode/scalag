package com.scalag

import Algebra.*
import Value.*
import izumi.reflect.Tag
import Algebra.*
import Algebra.given
import com.scalag.Control.when
import com.scalag.Expression.{ConstInt32, E}
import com.scalag.GSeq.*
import com.scalag.compiler.Digest
import com.scalag.compiler.Digest.{CustomDependencies, CustomDigest, DigestedExpression}

import java.util.Base64
import scala.util.Random

class GSeq[T <: Value : Tag : FromExpr](
  val uninitSource: Expression[_] => GSeqStream[_],
  val elemOps: List[GSeq.ElemOp[_]],
  val limit: Option[Int],
  currentElemExprTreeId: Int = treeidState.getAndIncrement(),
  aggregateElemExprTreeId: Int = treeidState.getAndIncrement()
):

  def copyWithDynamicTrees[R <: Value : Tag : FromExpr](
    elemOps: List[GSeq.ElemOp[_]] = elemOps,
    limit: Option[Int] = limit,
    currentElemExprTreeId: Int = currentElemExprTreeId,
    aggregateElemExprTreeId: Int = aggregateElemExprTreeId
  ) = GSeq[R](
    uninitSource,
    elemOps,
    limit,
    currentElemExprTreeId,
    aggregateElemExprTreeId
  )

  private val currentElemExpr = CurrentElem[T](currentElemExprTreeId)
  val source = uninitSource(currentElemExpr)
  private def currentElem: T = summon[FromExpr[T]].fromExpr(currentElemExpr)
  private def aggregateElem[R <: Value : Tag: FromExpr]: R = summon[FromExpr[R]].fromExpr(AggregateElem[R](aggregateElemExprTreeId))

  // todo get rid of base64
  val currentElemDigest = Base64.getEncoder.encodeToString(BigInt(currentElemExprTreeId).toByteArray)
  val aggregateElemDigest = Base64.getEncoder.encodeToString(BigInt(aggregateElemExprTreeId).toByteArray)
  

  
  def map[R <: Value : Tag : FromExpr](fn: T => R): GSeq[R] =
    this.copyWithDynamicTrees[R](elemOps = elemOps :+ GSeq.MapOp[T,R](
      fn(currentElem).tree
    ))

  def filter(fn: T => GBoolean): GSeq[T] =
    this.copyWithDynamicTrees(elemOps = elemOps :+ GSeq.FilterOp(
      fn(currentElem).tree
    ))

  def takeWhile(fn: T => GBoolean): GSeq[T] =
    this.copyWithDynamicTrees(elemOps = elemOps :+ GSeq.TakeUntilOp(
      fn(currentElem).tree
    ))

  def limit(n: Int): GSeq[T] =
    this.copyWithDynamicTrees(limit = Some(n))

  def fold[R <: Value : Tag : FromExpr](zero: R, fn: (R, T) => R): R =
    summon[FromExpr[R]].fromExpr(GSeq.FoldSeq(zero, fn(aggregateElem, currentElem).tree, this))

  def count: Int32 =
    fold(0, (acc: Int32, _: T) => acc + 1)

  def lastOr(t: T): T =
    fold(t, (_: T, elem: T) => elem)
    


object GSeq:

  def gen[T <: Value : Tag : FromExpr](first: T, next: T => T) = GSeq(
    ce => GSeqStream(first, next(summon[FromExpr[T]].fromExpr(ce.asInstanceOf[E[T]])).tree),
    Nil,
    None
  )
  
  // REALLY naive implementation, should be replaced with dynamic array (O(1)) access
  def of[T <: Value : Tag : FromExpr](xs: List[T]) =
    GSeq.gen[Int32](0, _ + 1).map { i =>
      val first = when(i === 0) {
        xs(0)
      }
      xs.init.zipWithIndex.tail.foldLeft(first) {
        case (acc, (x, j)) =>
          acc.elseWhen(i === j) {
            x
          }
      }.otherwise(xs.last)
    }.limit(xs.length)
  
  case class CurrentElem[T <: Value : Tag](tid: Int) extends PhantomExpression[T] with CustomDigest:
    override def digest: Array[Byte] = BigInt(tid).toByteArray
  
  case class AggregateElem[T <: Value : Tag](tid: Int) extends PhantomExpression[T] with CustomDigest:
    override def digest: Array[Byte] = BigInt(tid).toByteArray
    
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





