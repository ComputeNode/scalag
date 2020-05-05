package com.unihogsoft.scalag.dsl

import java.beans.Expression

import scala.reflect.runtime.universe.TypeTag

trait DSLStructure {
  trait ValType {
    type Self >: this.type <: ValType
    def tree: E[Self]
    def init: E[Self] => Self
    private def adapt[R <: ValType](e: E[R])= e.asInstanceOf[E[Self]]
    protected def biCombine[R <: ValType](f: (E[Self], E[Self]) => E[Self], e: R)(implicit ev: R =:= Self) = {
      init(f(tree, adapt(e.tree)))
    }
  }

  type ValInit[T <: ValType] = E[T] => T

  abstract class Expression[T <: ValType : TypeTag] extends Product {
    val valTypeTag: TypeTag[T] = implicitly[TypeTag[T]]
  }
  type E[T <: ValType] = Expression[T]

  def formatTree(e: Expression[_]): String = {
    def format(value: Any, depth: Int): String = {
      val indent = "  " * depth
      value match {
        case expr: Expression[_] =>
          def optNewline(cond: Boolean) = if(cond) "\n" else ""
          val hasExprChildren = expr.productIterator.exists {
            case _: Expression[_] => true
            case _ => false
          }
          val childrenString = if(hasExprChildren) {
            expr.productIterator.map(
              p => format(p, depth + 1)
            ).mkString("")
          } else {
            s"(${expr.productIterator.map(_.toString).mkString(", ")})"
          }
          val exprType = expr.getClass.getSimpleName
          val mirror = expr.valTypeTag.mirror
          val exprValType = mirror.runtimeClass(expr.valTypeTag.tpe).getSimpleName
          val prodString = s"$indent- ${exprType} [${exprValType}] ${optNewline(hasExprChildren)}"
          prodString + childrenString + optNewline(!hasExprChildren)
        case any =>
          s"$indent* ${any.toString}\n"
      }
    }
    format(e, 0)
  }
}