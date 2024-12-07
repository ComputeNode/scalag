package io.computenode.cyfra.dsl.macros

//import io.computenode.cyfra.dsl.macros.Source.Enclosing
//
//import scala.quoted.{Expr, Quotes}
//
//case class Source(
//  name: String,
//  enclosing: Enclosing
//)
//
//object Source:
//
//  sealed trait Enclosing
//
//  case object NonPure extends Enclosing
//  case class Pure(name: String)
//
//
//  inline implicit def generate: Source = ${ sourceImpl }
//
//  /**
//   * If value is in pure function, then put information about it in Source
//   * Pure is:
//   *  - def that only has Values as params and Value as output
//   *  - Does not use other Values than the ones from args
//   *  - What about Scala values? That's to be thought about...
//   * Assert purity with annotation and error if it's not pure. Cache! Check once per function!
//   */
//
//  def sourceImpl(using Quotes): Expr[Source] = {
//    import quotes.reflect.*
//
//    val name = Expr(Symbol.spliceOwner.name)
//    val enclosing = Symbol.spliceOwner.owner match {
//      case DefDef(_, _, _, _) => '{ Source.Pure(${ Expr(Symbol.spliceOwner.owner.name) }) }
//      case _ => '{ Source.NonPure }
//    }
//
//    '{ Source($name, $enclosing) }
//  }
