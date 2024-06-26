package com.scalag.vulkan.utility;

/** @author
  *   MarconZet Created 13.04.2020
  */
abstract class VulkanObject {
  protected var alive: Boolean = false

  def destroy(): Unit = {
    if (!alive)
      throw new IllegalStateException();
    close();
    alive = false;
  }

  protected def create(): Unit = {
    if (alive)
      throw new IllegalStateException();
    init();
    alive = true;
  }

  protected def init(): Unit

  protected def close(): Unit

}
