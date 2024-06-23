### GStream concept

## Julia set
```scala 3
def juliaSet(uv: Vec2[Float32]): Int32 = {
  GStream.gen(init = uv, next = v => {
    ((v.x * v.x) - (v.y * v.y), 2.0 * z.x * z.y) + constant
  }).limit(RECURSION_LIMIT).map(length).takeUntil(_ < 2.0).length
}
```

## Sum of squares
```scala 3
def sumOfSquares(uv: Vec2[Float32]): Float32 = {
  GStream.gen(init = uv, next = v => {
    (v.x * v.x, v.y * v.y)
  }).limit(RECURSION_LIMIT).map(length).takeUntil(_ < 2.0).sum
}
```

## GLSL Example

```glsl
#version 450

layout (location = 0) flat in ivec2 pos;
layout (location = 0) out float color;
void main()
{
/**
    * GStream.gen(1.3, x => x*x)
    * .map(x => x / 1.1)
    * .filter(x => x % 2 > 1)
    * .takeWhile(x => x < 3.0)
    * .sum()
    */
    float currentElem = 1.3;
    bool shouldTake = true;
    int i = 0;
    int n = 10;
    float acc = 0;
    while (shouldTake && i < n)
    {
        currentElem = currentElem * currentElem;
        float x = currentElem / 1.1;
        if (mod(x, 2.0) > 1.0) {
            shouldTake = currentElem < 3.0f;
            if(shouldTake) {
                result = result + acc; // takes result and current
            }
        }

    }
    color = float(i) / 10.0f;
}
```

## LOOP

               OpSource GLSL 450
               OpSourceExtension "GL_GOOGLE_cpp_style_line_directive"
               OpSourceExtension "GL_GOOGLE_include_directive"
               OpName %main "main"
               OpName %acc "acc"
               OpName %shouldTake "shouldTake"
               OpName %i "i"
               OpName %n "n"
               OpName %result "result"
               OpName %x "x"
               OpName %color "color"
               OpName %pos "pos"
               OpDecorate %color Location 0
               OpDecorate %pos Flat
               OpDecorate %pos Location 0
       %void = OpTypeVoid
          %3 = OpTypeFunction %void
      %float = OpTypeFloat 32
%_ptr_Function_float = OpTypePointer Function %float
%float_1_29999995 = OpConstant %float 1.29999995
%bool = OpTypeBool
%_ptr_Function_bool = OpTypePointer Function %bool
%true = OpConstantTrue %bool
%int = OpTypeInt 32 1
%_ptr_Function_int = OpTypePointer Function %int
%int_0 = OpConstant %int 0
%int_10 = OpConstant %int 10
%float_0 = OpConstant %float 0
%float_1_10000002 = OpConstant %float 1.10000002
%float_2 = OpConstant %float 2
%float_1 = OpConstant %float 1
%float_3 = OpConstant %float 3
%_ptr_Output_float = OpTypePointer Output %float
%color = OpVariable %_ptr_Output_float Output
%float_10 = OpConstant %float 10
%v2int = OpTypeVector %int 2
%_ptr_Input_v2int = OpTypePointer Input %v2int
%pos = OpVariable %_ptr_Input_v2int Input
%main = OpFunction %void None %3
%5 = OpLabel
%acc = OpVariable %_ptr_Function_float Function
%shouldTake = OpVariable %_ptr_Function_bool Function
%i = OpVariable %_ptr_Function_int Function
%n = OpVariable %_ptr_Function_int Function
%result = OpVariable %_ptr_Function_float Function
%x = OpVariable %_ptr_Function_float Function
OpStore %acc %float_1_29999995
OpStore %shouldTake %true
OpStore %i %int_0
OpStore %n %int_10
OpStore %result %float_0

OpBranch %22
%22 = OpLabel

OpLoopMerge %24 %25 None
OpBranch %26
%26 = OpLabel
%27 = OpLoad %bool %shouldTake
%28 = OpLoad %int %i
%29 = OpLoad %int %n
%30 = OpSLessThan %bool %28 %29
%31 = OpLogicalAnd %bool %27 %30

OpBranchConditional %31 %23 %24
%23 = OpLabel
%32 = OpLoad %float %acc
%33 = OpLoad %float %acc
%34 = OpFMul %float %32 %33
OpStore %acc %34
%36 = OpLoad %float %acc
%38 = OpFDiv %float %36 %float_1_10000002
OpStore %x %38
%39 = OpLoad %float %x
%41 = OpFMod %float %39 %float_2
%43 = OpFOrdGreaterThan %bool %41 %float_1

OpSelectionMerge %45 None
OpBranchConditional %43 %44 %45
%44 = OpLabel
%46 = OpLoad %float %acc
%48 = OpFOrdLessThan %bool %46 %float_3
OpStore %shouldTake %48
%49 = OpLoad %float %result
%50 = OpLoad %float %acc
%51 = OpFAdd %float %49 %50
OpStore %result %51

OpBranch %45
%45 = OpLabel

OpBranch %25
%25 = OpLabel

OpBranch %22
%24 = OpLabel

%54 = OpLoad %int %i
%55 = OpConvertSToF %float %54
%57 = OpFDiv %float %55 %float_10
OpStore %color %57
OpReturn
OpFunctionEnd
