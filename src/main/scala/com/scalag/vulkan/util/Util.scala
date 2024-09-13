package com.scalag.vulkan.util

import org.lwjgl.BufferUtils
import org.lwjgl.assimp.{AIFace, AIMesh, AIVector3D, Assimp}
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_SUCCESS

import java.nio.{ByteBuffer, FloatBuffer, IntBuffer, ShortBuffer}
import scala.util.Using

object Util {
  def pushStack[T](f: MemoryStack => T): T = Using(MemoryStack.stackPush())(f).get

  def check(err: Int, message: String = ""): Unit = if (err != VK_SUCCESS) throw new VulkanAssertionError(message, err)

  def importScene(path: String): Seq[Mesh] = {
    val bytes = getClass.getClassLoader.getResourceAsStream(path).readAllBytes()
    val bb = BufferUtils.createByteBuffer(bytes.length)
    bb.put(bytes).flip()

    val scene = Assimp.aiImportFileFromMemory(bb, 0, "obj")
    val meshCount = scene.mNumMeshes()
    val meshesBuffer = scene.mMeshes()
    val res = (0 until meshCount)
      .map(x => AIMesh.create(meshesBuffer.get(x)))
      .map { mesh =>
        val vert = BufferUtils.createByteBuffer(mesh.mNumVertices() * 3 * 4)
        val vertf = vert.asFloatBuffer()
        mesh.mVertices().forEach(v => vertf.put(v.x()).put(v.y()).put(v.z()))

        val face = BufferUtils.createByteBuffer(mesh.mNumFaces() * 3 * 4)
        val facei = face.asIntBuffer()
        mesh.mFaces().forEach(f => assert(f.mNumIndices() == 3, "Only triangles are supported"))
        mesh.mFaces().forEach(f => facei.put(f.mIndices()))

        Mesh(vert.flip(), mesh.mNumVertices(), face.flip(), mesh.mNumFaces())
      }
    Assimp.aiReleaseImport(scene)
    res
  }

  case class Mesh(vertices: ByteBuffer, v: Int, faces: ByteBuffer, f: Int)
}
