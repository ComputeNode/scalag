package com.unihogsoft.scalag.api
import com.unihogsoft.scalag.dsl.DSL._

import scala.reflect.ClassTag

case class GArray[R <: ValType](index: Int){
  def at(i: Int32): R = ???
}

object GArray {

}