# scalag

Prototype project that would allow to run Scala DSL to be compiled to SPIR-V and run as Vulkan Compute Shaders on GPU. Such approach allows much more flexibility than compiling the DSL to CUDA/OpenCL code and makes it highly multi-platform - works on all popular operating systems and most of the platforms that implement Vulkan 1.0 in some way. SPIR-V is also easily transpiled to HLSL for Direct3D. Working POC:
```scala
object Test extends App {

  implicit val gcontext: GContext = new MVPContext()
  implicit val econtext: ExecutionContext = Implicits.global

  val calcAverages: GMap[DSL.Float32, DSL.Float32] = GMap {
    (i: Int32, f: GArray[Float32]) =>
      val prev = f.at(i - 1)
      val next = f.at(i + 1)
      (prev / 2) + (next / 2)
  } //gets compiled to SPIR-V

  val data = FloatMem(Array(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f))

  data.map(addOne).map(r => {
    println("Output!")
    println(r.getData(0).asFloatBuffer().array().mkString(", "))
  })

}
```
