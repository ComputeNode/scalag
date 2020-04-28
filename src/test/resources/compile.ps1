"$Env:VULKAN_SDK\Bin\glslangValidator.exe -V copy.comp -o copy.spv" | Invoke-Expression
"$Env:VULKAN_SDK\Bin\glslangValidator.exe -V two_copy.comp -o two_copy.spv" | Invoke-Expression