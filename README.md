# scalag

Prototype project that would allow to run Scala DSL to be compiled to SPIR-V and run as Vulkan Compute Shaders on GPU. Such approach allows much more flexibility than compiling the DSL to CUDA/OpenCL code and makes it highly multi-platform - works on all popular operating systems and most of the platforms that implement Vulkan 1.0 in some way. SPIR-V is also easily transpiled to HLSL for Direct3D.

## Examples

### Blur
Code that blurs an input image. Blur is applied with constant weight of all surrounding pixels (better visual effect could be achieved with gaussian blur weights).
```scala
val function: GArray2DFunction[DSL.Float32, DSL.Float32] = GArray2DFunction(dim, dim, {
  case ((x: Int32, y: Int32), arr) =>
    def sample(offsetX: Int, offsetY: Int): Float32 = arr.at(x + offsetX, y + offsetY,)
    def blur(radius: Int): Float32 = {
      val samples = for {
        offsetX <- -radius to radius
        offsetY <- -radius to radius
      } yield sample(offsetX, offsetY)
      val weight = 1.0f / ((2 * radius + 1) * (2 * radius + 1))
      samples.reduce(_ + _) * weight
    }
    blur(10)
})
```
#### Input
<img src="https://github.com/scalag/scalag/assets/4761866/a8d3b8f2-3fca-4fbb-b946-2e656206f8e7" width="256">

#### Output
<img src="https://github.com/scalag/scalag/assets/4761866/d86fb1ca-7ee3-4d54-a5a2-fcfc7cd91c02" width="256">

### Square 2d function
Code that calculates values of `x^2 + y^2` on 2d plane.
```scala
val function: GArray2DFunction[DSL.Float32, DSL.Float32] = GArray2DFunction(dim, dim, {
  case ((x: Int32, y: Int32), _) =>
    val xSquared = (x - (dim / 2)) * (x - (dim / 2))
    val ySquared = (y - (dim / 2)) * (y - (dim / 2))
    xSquared.asFloat + ySquared.asFloat
})
```

#### Output
<img src="https://github.com/scalag/scalag/assets/4761866/15ce6ccf-bc6a-4dd1-8131-444bc4d78689" width="256">
