package com.unihogsoft.scalag.compiler

object Opcodes {

  case class Instruction(opcode: Int, operands: List[Int])

  val MagicNumber = 0x07230203
  val Version = 0x00010100
  val Revision = 8
  val OpCodeMask = 0xffff
  val WordCountShift = 16

  object SourceLanguage {
    val Unknown = 0
    val ESSL = 1
    val GLSL = 2
    val OpenCL_C = 3
    val OpenCL_CPP = 4
    val HLSL = 5
  }

  object ExecutionModel {
    val Vertex = 0
    val TessellationControl = 1
    val TessellationEvaluation = 2
    val Geometry = 3
    val Fragment = 4
    val GLCompute = 5
    val Kernel = 6
  }

  object AddressingModel {
    val Logical = 0
    val Physical32 = 1
    val Physical64 = 2
  }

  object MemoryModel {
    val Simple = 0
    val GLSL450 = 1
    val OpenCL = 2
  }

  object ExecutionMode {
    val Invocations = 0
    val SpacingEqual = 1
    val SpacingFractionalEven = 2
    val SpacingFractionalOdd = 3
    val VertexOrderCw = 4
    val VertexOrderCcw = 5
    val PixelCenterInteger = 6
    val OriginUpperLeft = 7
    val OriginLowerLeft = 8
    val EarlyFragmentTests = 9
    val PointMode = 10
    val Xfb = 11
    val DepthReplacing = 12
    val DepthGreater = 14
    val DepthLess = 15
    val DepthUnchanged = 16
    val LocalSize = 17
    val LocalSizeHint = 18
    val InputPoints = 19
    val InputLines = 20
    val InputLinesAdjacency = 21
    val Triangles = 22
    val InputTrianglesAdjacency = 23
    val Quads = 24
    val Isolines = 25
    val OutputVertices = 26
    val OutputPoints = 27
    val OutputLineStrip = 28
    val OutputTriangleStrip = 29
    val VecTypeHint = 30
    val ContractionOff = 31
    val Initializer = 33
    val Finalizer = 34
    val SubgroupSize = 35
    val SubgroupsPerWorkgroup = 36
    val PostDepthCoverage = 4446
    val StencilRefReplacingEXT = 5027
  }

  object StorageClass {
    val UniformConstant = 0
    val Input = 1
    val Uniform = 2
    val Output = 3
    val Workgroup = 4
    val CrossWorkgroup = 5
    val Private = 6
    val Function = 7
    val Generic = 8
    val PushConstant = 9
    val AtomicCounter = 10
    val Image = 11
    val StorageBuffer = 12
  }

  object Dim {
    val Dim1D = 0
    val Dim2D = 1
    val Dim3D = 2
    val Cube = 3
    val Rect = 4
    val Buffer = 5
    val SubpassData = 6
  }

  object SamplerAddressingMode {
    val None = 0
    val ClampToEdge = 1
    val Clamp = 2
    val Repeat = 3
    val RepeatMirrored = 4
  }

  object SamplerFilterMode {
    val Nearest = 0
    val Linear = 1
  }

  object ImageFormat {
    val Unknown = 0
    val Rgba32f = 1
    val Rgba16f = 2
    val R32f = 3
    val Rgba8 = 4
    val Rgba8Snorm = 5
    val Rg32f = 6
    val Rg16f = 7
    val R11fG11fB10f = 8
    val R16f = 9
    val Rgba16 = 10
    val Rgb10A2 = 11
    val Rg16 = 12
    val Rg8 = 13
    val R16 = 14
    val R8 = 15
    val Rgba16Snorm = 16
    val Rg16Snorm = 17
    val Rg8Snorm = 18
    val R16Snorm = 19
    val R8Snorm = 20
    val Rgba32i = 21
    val Rgba16i = 22
    val Rgba8i = 23
    val R32i = 24
    val Rg32i = 25
    val Rg16i = 26
    val Rg8i = 27
    val R16i = 28
    val R8i = 29
    val Rgba32ui = 30
    val Rgba16ui = 31
    val Rgba8ui = 32
    val R32ui = 33
    val Rgb10a2ui = 34
    val Rg32ui = 35
    val Rg16ui = 36
    val Rg8ui = 37
    val R16ui = 38
    val R8ui = 39
  }

  object ImageChannelOrder {
    val R = 0
    val A = 1
    val RG = 2
    val RA = 3
    val RGB = 4
    val RGBA = 5
    val BGRA = 6
    val ARGB = 7
    val Intensity = 8
    val Luminance = 9
    val Rx = 10
    val RGx = 11
    val RGBx = 12
    val Depth = 13
    val DepthStencil = 14
    val sRGB = 15
    val sRGBx = 16
    val sRGBA = 17
    val sBGRA = 18
    val ABGR = 19
  }

  object ImageChannelDataType {
    val SnormInt8 = 0
    val SnormInt16 = 1
    val UnormInt8 = 2
    val UnormInt16 = 3
    val UnormShort565 = 4
    val UnormShort555 = 5
    val UnormInt101010 = 6
    val SignedInt8 = 7
    val SignedInt16 = 8
    val SignedInt32 = 9
    val UnsignedInt8 = 10
    val UnsignedInt16 = 11
    val UnsignedInt32 = 12
    val HalfFloat = 13
    val Float = 14
    val UnormInt24 = 15
    val UnormInt101010_2 = 16
  }

  object ImageOperandsShift {
    val Bias = 0
    val Lod = 1
    val Grad = 2
    val ConstOffset = 3
    val Offset = 4
    val ConstOffsets = 5
    val Sample = 6
    val MinLod = 7
  }

  object ImageOperandsMask {
    val MaskNone = 0
    val Bias = 0x00000001
    val Lod = 0x00000002
    val Grad = 0x00000004
    val ConstOffset = 0x00000008
    val Offset = 0x00000010
    val ConstOffsets = 0x00000020
    val Sample = 0x00000040
    val MinLod = 0x00000080
  }

  object FPFastMathModeShift {
    val NotNaN = 0
    val NotInf = 1
    val NSZ = 2
    val AllowRecip = 3
    val Fast = 4
  }

  object FPFastMathModeMask {
    val MaskNone = 0
    val NotNaN = 0x00000001
    val NotInf = 0x00000002
    val NSZ = 0x00000004
    val AllowRecip = 0x00000008
    val Fast = 0x00000010
  }

  object FPRoundingMode {
    val RTE = 0
    val RTZ = 1
    val RTP = 2
    val RTN = 3
  }

  object LinkageType {
    val Export = 0
    val Import = 1
  }

  object AccessQualifier {
    val ReadOnly = 0
    val WriteOnly = 1
    val ReadWrite = 2
  }

  object FunctionParameterAttribute {
    val Zext = 0
    val Sext = 1
    val ByVal = 2
    val Sret = 3
    val NoAlias = 4
    val NoCapture = 5
    val NoWrite = 6
    val NoReadWrite = 7
  }

  object Decoration {
    val RelaxedPrecision = 0
    val SpecId = 1
    val Block = 2
    val BufferBlock = 3
    val RowMajor = 4
    val ColMajor = 5
    val ArrayStride = 6
    val MatrixStride = 7
    val GLSLShared = 8
    val GLSLPacked = 9
    val CPacked = 10
    val BuiltIn = 11
    val NoPerspective = 13
    val Flat = 14
    val Patch = 15
    val Centroid = 16
    val Sample = 17
    val Invariant = 18
    val Restrict = 19
    val Aliased = 20
    val Volatile = 21
    val Constant = 22
    val Coherent = 23
    val NonWritable = 24
    val NonReadable = 25
    val Uniform = 26
    val SaturatedConversion = 28
    val Stream = 29
    val Location = 30
    val Component = 31
    val Index = 32
    val Binding = 33
    val DescriptorSet = 34
    val Offset = 35
    val XfbBuffer = 36
    val XfbStride = 37
    val FuncParamAttr = 38
    val FPRoundingMode = 39
    val FPFastMathMode = 40
    val LinkageAttributes = 41
    val NoContraction = 42
    val InputAttachmentIndex = 43
    val Alignment = 44
    val MaxByteOffset = 45
    val ExplicitInterpAMD = 4999
    val OverrideCoverageNV = 5248
    val PassthroughNV = 5250
    val ViewportRelativeNV = 5252
    val SecondaryViewportRelativeNV = 5256
  }

  object BuiltIn {
    val Position = 0
    val PointSize = 1
    val ClipDistance = 3
    val CullDistance = 4
    val VertexId = 5
    val InstanceId = 6
    val PrimitiveId = 7
    val InvocationId = 8
    val Layer = 9
    val ViewportIndex = 10
    val TessLevelOuter = 11
    val TessLevelInner = 12
    val TessCoord = 13
    val PatchVertices = 14
    val FragCoord = 15
    val PointCoord = 16
    val FrontFacing = 17
    val SampleId = 18
    val SamplePosition = 19
    val SampleMask = 20
    val FragDepth = 22
    val HelperInvocation = 23
    val NumWorkgroups = 24
    val WorkgroupSize = 25
    val WorkgroupId = 26
    val LocalInvocationId = 27
    val GlobalInvocationId = 28
    val LocalInvocationIndex = 29
    val WorkDim = 30
    val GlobalSize = 31
    val EnqueuedWorkgroupSize = 32
    val GlobalOffset = 33
    val GlobalLinearId = 34
    val SubgroupSize = 36
    val SubgroupMaxSize = 37
    val NumSubgroups = 38
    val NumEnqueuedSubgroups = 39
    val SubgroupId = 40
    val SubgroupLocalInvocationId = 41
    val VertexIndex = 42
    val InstanceIndex = 43
    val SubgroupEqMaskKHR = 4416
    val SubgroupGeMaskKHR = 4417
    val SubgroupGtMaskKHR = 4418
    val SubgroupLeMaskKHR = 4419
    val SubgroupLtMaskKHR = 4420
    val BaseVertex = 4424
    val BaseInstance = 4425
    val DrawIndex = 4426
    val DeviceIndex = 4438
    val ViewIndex = 4440
    val BaryCoordNoPerspAMD = 4992
    val BaryCoordNoPerspCentroidAMD = 4993
    val BaryCoordNoPerspSampleAMD = 4994
    val BaryCoordSmoothAMD = 4995
    val BaryCoordSmoothCentroidAMD = 4996
    val BaryCoordSmoothSampleAMD = 4997
    val BaryCoordPullModelAMD = 4998
    val FragStencilRefEXT = 5014
    val ViewportMaskNV = 5253
    val SecondaryPositionNV = 5257
    val SecondaryViewportMaskNV = 5258
    val PositionPerViewNV = 5261
    val ViewportMaskPerViewNV = 5262
  }

  object SelectionControlShift {
    val Flatten = 0
    val DontFlatten = 1
  }

  object SelectionControlMask {
    val MaskNone = 0
    val Flatten = 0x00000001
    val DontFlatten = 0x00000002
  }

  object LoopControlShift {
    val Unroll = 0
    val DontUnroll = 1
    val DependencyInfinite = 2
    val DependencyLength = 3
  }

  object LoopControlMask {
    val MaskNone = 0
    val Unroll = 0x00000001
    val DontUnroll = 0x00000002
    val DependencyInfinite = 0x00000004
    val DependencyLength = 0x00000008
  }

  object FunctionControlShift {
    val Inline = 0
    val DontInline = 1
    val Pure = 2
    val Const = 3
  }

  object FunctionControlMask {
    val MaskNone = 0
    val Inline = 0x00000001
    val DontInline = 0x00000002
    val Pure = 0x00000004
    val Const = 0x00000008
  }

  object MemorySemanticsShift {
    val Acquire = 1
    val Release = 2
    val AcquireRelease = 3
    val SequentiallyConsistent = 4
    val UniformMemory = 6
    val SubgroupMemory = 7
    val WorkgroupMemory = 8
    val CrossWorkgroupMemory = 9
    val AtomicCounterMemory = 10
    val ImageMemory = 11
  }

  object MemorySemanticsMask {
    val MaskNone = 0
    val Acquire = 0x00000002
    val Release = 0x00000004
    val AcquireRelease = 0x00000008
    val SequentiallyConsistent = 0x00000010
    val UniformMemory = 0x00000040
    val SubgroupMemory = 0x00000080
    val WorkgroupMemory = 0x00000100
    val CrossWorkgroupMemory = 0x00000200
    val AtomicCounterMemory = 0x00000400
    val ImageMemory = 0x00000800
  }

  object MemoryAccessShift {
    val Volatile = 0
    val Aligned = 1
    val Nontemporal = 2
  }

  object MemoryAccessMask {
    val MaskNone = 0
    val Volatile = 0x00000001
    val Aligned = 0x00000002
    val Nontemporal = 0x00000004
  }

  object Scope {
    val CrossDevice = 0
    val Device = 1
    val Workgroup = 2
    val Subgroup = 3
    val Invocation = 4
  }

  object GroupOperation {
    val Reduce = 0
    val InclusiveScan = 1
    val ExclusiveScan = 2
  }

  object KernelEnqueueFlags {
    val NoWait = 0
    val WaitKernel = 1
    val WaitWorkGroup = 2
  }

  object KernelProfilingInfoShift {
    val CmdExecTime = 0
  }

  object KernelProfilingInfoMask {
    val MaskNone = 0
    val CmdExecTime = 0x00000001
  }

  object Capability {
    val Matrix = 0
    val Shader = 1
    val Geometry = 2
    val Tessellation = 3
    val Addresses = 4
    val Linkage = 5
    val Kernel = 6
    val Vector16 = 7
    val Float16Buffer = 8
    val Float16 = 9
    val Float64 = 10
    val Int64 = 11
    val Int64Atomics = 12
    val ImageBasic = 13
    val ImageReadWrite = 14
    val ImageMipmap = 15
    val Pipes = 17
    val Groups = 18
    val DeviceEnqueue = 19
    val LiteralSampler = 20
    val AtomicStorage = 21
    val Int16 = 22
    val TessellationPointSize = 23
    val GeometryPointSize = 24
    val ImageGatherExtended = 25
    val StorageImageMultisample = 27
    val UniformBufferArrayDynamicIndexing = 28
    val SampledImageArrayDynamicIndexing = 29
    val StorageBufferArrayDynamicIndexing = 30
    val StorageImageArrayDynamicIndexing = 31
    val ClipDistance = 32
    val CullDistance = 33
    val ImageCubeArray = 34
    val SampleRateShading = 35
    val ImageRect = 36
    val SampledRect = 37
    val GenericPointer = 38
    val Int8 = 39
    val InputAttachment = 40
    val SparseResidency = 41
    val MinLod = 42
    val Sampled1D = 43
    val Image1D = 44
    val SampledCubeArray = 45
    val SampledBuffer = 46
    val ImageBuffer = 47
    val ImageMSArray = 48
    val StorageImageExtendedFormats = 49
    val ImageQuery = 50
    val DerivativeControl = 51
    val InterpolationFunction = 52
    val TransformFeedback = 53
    val GeometryStreams = 54
    val StorageImageReadWithoutFormat = 55
    val StorageImageWriteWithoutFormat = 56
    val MultiViewport = 57
    val SubgroupDispatch = 58
    val NamedBarrier = 59
    val PipeStorage = 60
    val SubgroupBallotKHR = 4423
    val DrawParameters = 4427
    val SubgroupVoteKHR = 4431
    val StorageBuffer16BitAccess = 4433
    val StorageUniformBufferBlock16 = 4433
    val StorageUniform16 = 4434
    val UniformAndStorageBuffer16BitAccess = 4434
    val StoragePushConstant16 = 4435
    val StorageInputOutput16 = 4436
    val DeviceGroup = 4437
    val MultiView = 4439
    val VariablePointersStorageBuffer = 4441
    val VariablePointers = 4442
    val AtomicStorageOps = 4445
    val SampleMaskPostDepthCoverage = 4447
    val ImageGatherBiasLodAMD = 5009
    val FragmentMaskAMD = 5010
    val StencilExportEXT = 5013
    val ImageReadWriteLodAMD = 5015
    val SampleMaskOverrideCoverageNV = 5249
    val GeometryShaderPassthroughNV = 5251
    val ShaderViewportIndexLayerEXT = 5254
    val ShaderViewportIndexLayerNV = 5254
    val ShaderViewportMaskNV = 5255
    val ShaderStereoViewNV = 5259
    val PerViewAttributesNV = 5260
    val SubgroupShuffleINTEL = 5568
    val SubgroupBufferBlockIOINTEL = 5569
    val SubgroupImageBlockIOINTEL = 5570
  }

  object Op {
    val OpNop = 0
    val OpUndef = 1
    val OpSourceContinued = 2
    val OpSource = 3
    val OpSourceExtension = 4
    val OpName = 5
    val OpMemberName = 6
    val OpString = 7
    val OpLine = 8
    val OpExtension = 10
    val OpExtInstImport = 11
    val OpExtInst = 12
    val OpMemoryModel = 14
    val OpEntryPoint = 15
    val OpExecutionMode = 16
    val OpCapability = 17
    val OpTypeVoid = 19
    val OpTypeBool = 20
    val OpTypeInt = 21
    val OpTypeFloat = 22
    val OpTypeVector = 23
    val OpTypeMatrix = 24
    val OpTypeImage = 25
    val OpTypeSampler = 26
    val OpTypeSampledImage = 27
    val OpTypeArray = 28
    val OpTypeRuntimeArray = 29
    val OpTypeStruct = 30
    val OpTypeOpaque = 31
    val OpTypePointer = 32
    val OpTypeFunction = 33
    val OpTypeEvent = 34
    val OpTypeDeviceEvent = 35
    val OpTypeReserveId = 36
    val OpTypeQueue = 37
    val OpTypePipe = 38
    val OpTypeForwardPointer = 39
    val OpConstantTrue = 41
    val OpConstantFalse = 42
    val OpConstant = 43
    val OpConstantComposite = 44
    val OpConstantSampler = 45
    val OpConstantNull = 46
    val OpSpecConstantTrue = 48
    val OpSpecConstantFalse = 49
    val OpSpecConstant = 50
    val OpSpecConstantComposite = 51
    val OpSpecConstantOp = 52
    val OpFunction = 54
    val OpFunctionParameter = 55
    val OpFunctionEnd = 56
    val OpFunctionCall = 57
    val OpVariable = 59
    val OpImageTexelPointer = 60
    val OpLoad = 61
    val OpStore = 62
    val OpCopyMemory = 63
    val OpCopyMemorySized = 64
    val OpAccessChain = 65
    val OpInBoundsAccessChain = 66
    val OpPtrAccessChain = 67
    val OpArrayLength = 68
    val OpGenericPtrMemSemantics = 69
    val OpInBoundsPtrAccessChain = 70
    val OpDecorate = 71
    val OpMemberDecorate = 72
    val OpDecorationGroup = 73
    val OpGroupDecorate = 74
    val OpGroupMemberDecorate = 75
    val OpVectorExtractDynamic = 77
    val OpVectorInsertDynamic = 78
    val OpVectorShuffle = 79
    val OpCompositeConstruct = 80
    val OpCompositeExtract = 81
    val OpCompositeInsert = 82
    val OpCopyObject = 83
    val OpTranspose = 84
    val OpSampledImage = 86
    val OpImageSampleImplicitLod = 87
    val OpImageSampleExplicitLod = 88
    val OpImageSampleDrefImplicitLod = 89
    val OpImageSampleDrefExplicitLod = 90
    val OpImageSampleProjImplicitLod = 91
    val OpImageSampleProjExplicitLod = 92
    val OpImageSampleProjDrefImplicitLod = 93
    val OpImageSampleProjDrefExplicitLod = 94
    val OpImageFetch = 95
    val OpImageGather = 96
    val OpImageDrefGather = 97
    val OpImageRead = 98
    val OpImageWrite = 99
    val OpImage = 100
    val OpImageQueryFormat = 101
    val OpImageQueryOrder = 102
    val OpImageQuerySizeLod = 103
    val OpImageQuerySize = 104
    val OpImageQueryLod = 105
    val OpImageQueryLevels = 106
    val OpImageQuerySamples = 107
    val OpConvertFToU = 109
    val OpConvertFToS = 110
    val OpConvertSToF = 111
    val OpConvertUToF = 112
    val OpUConvert = 113
    val OpSConvert = 114
    val OpFConvert = 115
    val OpQuantizeToF16 = 116
    val OpConvertPtrToU = 117
    val OpSatConvertSToU = 118
    val OpSatConvertUToS = 119
    val OpConvertUToPtr = 120
    val OpPtrCastToGeneric = 121
    val OpGenericCastToPtr = 122
    val OpGenericCastToPtrExplicit = 123
    val OpBitcast = 124
    val OpSNegate = 126
    val OpFNegate = 127
    val OpIAdd = 128
    val OpFAdd = 129
    val OpISub = 130
    val OpFSub = 131
    val OpIMul = 132
    val OpFMul = 133
    val OpUDiv = 134
    val OpSDiv = 135
    val OpFDiv = 136
    val OpUMod = 137
    val OpSRem = 138
    val OpSMod = 139
    val OpFRem = 140
    val OpFMod = 141
    val OpVectorTimesScalar = 142
    val OpMatrixTimesScalar = 143
    val OpVectorTimesMatrix = 144
    val OpMatrixTimesVector = 145
    val OpMatrixTimesMatrix = 146
    val OpOuterProduct = 147
    val OpDot = 148
    val OpIAddCarry = 149
    val OpISubBorrow = 150
    val OpUMulExtended = 151
    val OpSMulExtended = 152
    val OpAny = 154
    val OpAll = 155
    val OpIsNan = 156
    val OpIsInf = 157
    val OpIsFinite = 158
    val OpIsNormal = 159
    val OpSignBitSet = 160
    val OpLessOrGreater = 161
    val OpOrdered = 162
    val OpUnordered = 163
    val OpLogicalEqual = 164
    val OpLogicalNotEqual = 165
    val OpLogicalOr = 166
    val OpLogicalAnd = 167
    val OpLogicalNot = 168
    val OpSelect = 169
    val OpIEqual = 170
    val OpINotEqual = 171
    val OpUGreaterThan = 172
    val OpSGreaterThan = 173
    val OpUGreaterThanEqual = 174
    val OpSGreaterThanEqual = 175
    val OpULessThan = 176
    val OpSLessThan = 177
    val OpULessThanEqual = 178
    val OpSLessThanEqual = 179
    val OpFOrdEqual = 180
    val OpFUnordEqual = 181
    val OpFOrdNotEqual = 182
    val OpFUnordNotEqual = 183
    val OpFOrdLessThan = 184
    val OpFUnordLessThan = 185
    val OpFOrdGreaterThan = 186
    val OpFUnordGreaterThan = 187
    val OpFOrdLessThanEqual = 188
    val OpFUnordLessThanEqual = 189
    val OpFOrdGreaterThanEqual = 190
    val OpFUnordGreaterThanEqual = 191
    val OpShiftRightLogical = 194
    val OpShiftRightArithmetic = 195
    val OpShiftLeftLogical = 196
    val OpBitwiseOr = 197
    val OpBitwiseXor = 198
    val OpBitwiseAnd = 199
    val OpNot = 200
    val OpBitFieldInsert = 201
    val OpBitFieldSExtract = 202
    val OpBitFieldUExtract = 203
    val OpBitReverse = 204
    val OpBitCount = 205
    val OpDPdx = 207
    val OpDPdy = 208
    val OpFwidth = 209
    val OpDPdxFine = 210
    val OpDPdyFine = 211
    val OpFwidthFine = 212
    val OpDPdxCoarse = 213
    val OpDPdyCoarse = 214
    val OpFwidthCoarse = 215
    val OpEmitVertex = 218
    val OpEndPrimitive = 219
    val OpEmitStreamVertex = 220
    val OpEndStreamPrimitive = 221
    val OpControlBarrier = 224
    val OpMemoryBarrier = 225
    val OpAtomicLoad = 227
    val OpAtomicStore = 228
    val OpAtomicExchange = 229
    val OpAtomicCompareExchange = 230
    val OpAtomicCompareExchangeWeak = 231
    val OpAtomicIIncrement = 232
    val OpAtomicIDecrement = 233
    val OpAtomicIAdd = 234
    val OpAtomicISub = 235
    val OpAtomicSMin = 236
    val OpAtomicUMin = 237
    val OpAtomicSMax = 238
    val OpAtomicUMax = 239
    val OpAtomicAnd = 240
    val OpAtomicOr = 241
    val OpAtomicXor = 242
    val OpPhi = 245
    val OpLoopMerge = 246
    val OpSelectionMerge = 247
    val OpLabel = 248
    val OpBranch = 249
    val OpBranchConditional = 250
    val OpSwitch = 251
    val OpKill = 252
    val OpReturn = 253
    val OpReturnValue = 254
    val OpUnreachable = 255
    val OpLifetimeStart = 256
    val OpLifetimeStop = 257
    val OpGroupAsyncCopy = 259
    val OpGroupWaitEvents = 260
    val OpGroupAll = 261
    val OpGroupAny = 262
    val OpGroupBroadcast = 263
    val OpGroupIAdd = 264
    val OpGroupFAdd = 265
    val OpGroupFMin = 266
    val OpGroupUMin = 267
    val OpGroupSMin = 268
    val OpGroupFMax = 269
    val OpGroupUMax = 270
    val OpGroupSMax = 271
    val OpReadPipe = 274
    val OpWritePipe = 275
    val OpReservedReadPipe = 276
    val OpReservedWritePipe = 277
    val OpReserveReadPipePackets = 278
    val OpReserveWritePipePackets = 279
    val OpCommitReadPipe = 280
    val OpCommitWritePipe = 281
    val OpIsValidReserveId = 282
    val OpGetNumPipePackets = 283
    val OpGetMaxPipePackets = 284
    val OpGroupReserveReadPipePackets = 285
    val OpGroupReserveWritePipePackets = 286
    val OpGroupCommitReadPipe = 287
    val OpGroupCommitWritePipe = 288
    val OpEnqueueMarker = 291
    val OpEnqueueKernel = 292
    val OpGetKernelNDrangeSubGroupCount = 293
    val OpGetKernelNDrangeMaxSubGroupSize = 294
    val OpGetKernelWorkGroupSize = 295
    val OpGetKernelPreferredWorkGroupSizeMultiple = 296
    val OpRetainEvent = 297
    val OpReleaseEvent = 298
    val OpCreateUserEvent = 299
    val OpIsValidEvent = 300
    val OpSetUserEventStatus = 301
    val OpCaptureEventProfilingInfo = 302
    val OpGetDefaultQueue = 303
    val OpBuildNDRange = 304
    val OpImageSparseSampleImplicitLod = 305
    val OpImageSparseSampleExplicitLod = 306
    val OpImageSparseSampleDrefImplicitLod = 307
    val OpImageSparseSampleDrefExplicitLod = 308
    val OpImageSparseSampleProjImplicitLod = 309
    val OpImageSparseSampleProjExplicitLod = 310
    val OpImageSparseSampleProjDrefImplicitLod = 311
    val OpImageSparseSampleProjDrefExplicitLod = 312
    val OpImageSparseFetch = 313
    val OpImageSparseGather = 314
    val OpImageSparseDrefGather = 315
    val OpImageSparseTexelsResident = 316
    val OpNoLine = 317
    val OpAtomicFlagTestAndSet = 318
    val OpAtomicFlagClear = 319
    val OpImageSparseRead = 320
    val OpSizeOf = 321
    val OpTypePipeStorage = 322
    val OpConstantPipeStorage = 323
    val OpCreatePipeFromPipeStorage = 324
    val OpGetKernelLocalSizeForSubgroupCount = 325
    val OpGetKernelMaxNumSubgroups = 326
    val OpTypeNamedBarrier = 327
    val OpNamedBarrierInitialize = 328
    val OpMemoryNamedBarrier = 329
    val OpModuleProcessed = 330
    val OpSubgroupBallotKHR = 4421
    val OpSubgroupFirstInvocationKHR = 4422
    val OpSubgroupAllKHR = 4428
    val OpSubgroupAnyKHR = 4429
    val OpSubgroupAllEqualKHR = 4430
    val OpSubgroupReadInvocationKHR = 4432
    val OpGroupIAddNonUniformAMD = 5000
    val OpGroupFAddNonUniformAMD = 5001
    val OpGroupFMinNonUniformAMD = 5002
    val OpGroupUMinNonUniformAMD = 5003
    val OpGroupSMinNonUniformAMD = 5004
    val OpGroupFMaxNonUniformAMD = 5005
    val OpGroupUMaxNonUniformAMD = 5006
    val OpGroupSMaxNonUniformAMD = 5007
    val OpFragmentMaskFetchAMD = 5011
    val OpFragmentFetchAMD = 5012
    val OpSubgroupShuffleINTEL = 5571
    val OpSubgroupShuffleDownINTEL = 5572
    val OpSubgroupShuffleUpINTEL = 5573
    val OpSubgroupShuffleXorINTEL = 5574
    val OpSubgroupBlockReadINTEL = 5575
    val OpSubgroupBlockWriteINTEL = 5576
    val OpSubgroupImageBlockReadINTEL = 5577
    val OpSubgroupImageBlockWriteINTEL = 5578
  }

}
