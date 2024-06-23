package com.scalag

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

object ImageUtility {
  def renderToImage(arr: Array[(Float, Float, Float, Float)], n: Int, location: Path): Unit = {
    val image = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB)
    for (y <- 0 until n) {
      for (x <- 0 until n) {
        val (r,g,b, _) = arr(y * n + x)
        def clip(f: Float) = Math.min(1.0f, Math.max(0.0f, f))
        val (iR, iG, iB) = ((clip(r) * 255).toInt, (clip(g) * 255).toInt, (clip(b) * 255).toInt)
        image.setRGB(x, y, (iR << 16) | (iG << 8) | iB)
      }
    }

    val outputFile = location.toFile
    ImageIO.write(image, "png", outputFile)
    println(s"Image saved to ${outputFile.getAbsolutePath}")
  }

}
