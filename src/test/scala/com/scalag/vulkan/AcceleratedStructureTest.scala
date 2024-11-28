package com.scalag.vulkan

import com.scalag.vulkan.util.Util
import org.junit.jupiter.api.Test

class AcceleratedStructureTest {

  @Test
  def loadMesh(): Unit = {
    val meshes = Util.importScene("teapot.obj")
    assert(meshes.nonEmpty)
  }

}
