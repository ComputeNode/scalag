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
          vulkan-headers
          vulkan-validation-layers
        ];
      runScript = pkgs.writeShellScript "idea-wrapper.sh" ''
        export LD_LIBRARY_PATH=${pkgs.vulkan-loader}/lib:$LD_LIBRARY_PATH
        exec idea-ultimate
      '';
    };
  in {
    devShells.${system}.default = fhs.env;
  };
}
