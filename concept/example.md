## Concept

#### Basic API
```scala 3
@main
def main = {
  val RECURSION_LIMIT = 10000
  val dim = 1024
  val juliaSet: GArray2DFunction[Vec2[Float32], Float32, Vec3[Float32]] = GArray2DFunction(dim, dim)
  
      def juliaSet(uv: Vec2[Float32]): Int32 = {
        GStream.gen(init = uv, next = v => {
          ((v.x * v.x) - (v.y * v.y), 2.0 * z.x * z.y) + constant
        }).limit(RECURSION_LIMIT).map(length).takeUntil(_ < 2.0).length
      }
  
      def rotate(uv: Vec2[Float32], angle: Float32): Vec2[Float32] = {
        val newXAxis = (cos(angle), sin(angle))
        val newYAxis = (-newXAxis.y, newXAxis.x)
        (uv dot newXAxis, uv dot newYAxis) * 0.9
      }
  
      val uv = 2.0 * (coords - 0.5 * dim) / dim
      val angle = PI / 3.0
      val rotatedUv = rotate(uv, angle)

      val recursionCount = juliaSet(rotatedUv)

      val f = recursionCount.toFloat / RECURSION_LIMIT
      val ff = pow(f, 1.0 - f)
      val step = smoothstep(0.0, 1.0, ff)
      (
        step * (uv.x * 0.5 + 3),
        step * (uv.y * 0.5 + 3),
        step * (-uv.x * 0.5 + 3),
      ) * 5000.0
    
```
