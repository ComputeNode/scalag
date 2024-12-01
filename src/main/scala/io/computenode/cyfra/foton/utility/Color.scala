package io.computenode.cyfra.foton.utility

import io.computenode.cyfra.Algebra.*
import io.computenode.cyfra.Control.*
import io.computenode.cyfra.Algebra.given 
import io.computenode.cyfra.Functions.{mix, pow}
import io.computenode.cyfra.Value.{Float32, Vec3}
import Math3D.lessThan

object Color:
  
  def SRGBToLinear(rgb: Vec3[Float32]): Vec3[Float32] = {
    val clampedRgb = vclamp(rgb, 0.0f, 1.0f)
    mix(
      pow((clampedRgb + vec3(0.055f)) * (1.0f / 1.055f), vec3(2.4f)),
      clampedRgb * (1.0f / 12.92f),
      lessThan(clampedRgb, 0.04045f)
    )
  }

  def linearToSRGB(rgb: Vec3[Float32]): Vec3[Float32] = {
    val clampedRgb = vclamp(rgb, 0.0f, 1.0f)
    mix(
      pow(clampedRgb, vec3(1.0f / 2.4f)) * 1.055f - vec3(0.055f),
      clampedRgb * 12.92f,
      lessThan(clampedRgb, 0.0031308f)
    )
  }
  
  object InterpolationThemes:
    val Blue = ((8f, 22f, 104f) * (1 / 255f), (62f, 82f, 199f) * (1 / 255f), (221f, 233f, 255f) * (1 / 255f))
    
  def interpolate3(colors: (Vec3[Float32], Vec3[Float32], Vec3[Float32]), f: Float32): Vec3[Float32] = 
    val (c1, c2, c3) = colors
    val ratio1 = (1f - f) * (1f - f)
    val ratio2 = 2f * f * (1f - f)
    val ratio3 = f * f
    c1 * ratio1 + c2 * ratio2 + c3 * ratio3

  transparent inline def hex(inline color: String): Any = ${hexImpl('{color})}

  import scala.quoted.*
  def hexImpl(color: Expr[String])(using Quotes): Expr[Any] =
    import quotes.reflect.*
    val str = color.valueOrAbort
    val rgbPattern = """#?([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})""".r
    val rgbaPattern = """#?([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})""".r
    def byteHexToFloat(hex: String): Float = Integer.parseInt(hex, 16) / 255f
    def byteHexToFloatExpr(hex: String): Expr[Float] = Expr(byteHexToFloat(hex))
    str match
      case rgbPattern(r, g, b) => '{(${byteHexToFloatExpr(r)}, ${byteHexToFloatExpr(g)}, ${byteHexToFloatExpr(b)})}
      case rgbaPattern(r, g, b, a) => '{(${byteHexToFloatExpr(r)}, ${byteHexToFloatExpr(g)}, ${byteHexToFloatExpr(b)}, ${byteHexToFloatExpr(a)})}
      case _ => quotes.reflect.report.errorAndAbort(s"Invalid color format: $str")