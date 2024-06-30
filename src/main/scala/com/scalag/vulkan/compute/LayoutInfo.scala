package com.scalag.vulkan.compute

/** @author
  *   MarconZet Created 25.04.2020
  */

sealed trait LayoutElementSize
case class InputBufferSize(elemSize: Int) extends LayoutElementSize
case class UniformSize(size: Int) extends LayoutElementSize

case class LayoutInfo(set: Int, binding: Int, size: LayoutElementSize)