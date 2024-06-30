package com.scalag.vulkan.compute

/** @author
  *   MarconZet Created 25.04.2020
  */
case class LayoutInfo(sets: Seq[LayoutSet])
case class LayoutSet(id: Int, bindings: Seq[Binding])
case class Binding(id: Int, size: Int)
