{
  description = "Development environment for Vulkan";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
  };

  outputs = {
    self,
    nixpkgs,
  }: let
    system = "x86_64-linux";
    pkgs = import nixpkgs {inherit system;};
    fhs = pkgs.buildFHSUserEnv {
      name = "Idea vulkan";
      targetPkgs = pkgs:
        with pkgs; [
          glslang
          vulkan-loader
          vulkan-validation-layers
          spirv-tools
        ];
      runScript = pkgs.writeShellScript "idea-wrapper.sh" ''
        export LD_LIBRARY_PATH=/lib64:$LD_LIBRARY_PATH
        exec bash
      '';
    };
  in {
    devShells.${system}.default = fhs.env;
  };
}
