# let
#   hostPkgs = import <nixpkgs> { };
#   pinnedPkgs = hostPkgs.fetchFromGitHub {
#     owner = "berberman";
#     repo = "nixpkgs";
#     rev = "0ad7ad0f0d06cc7e98434eb4d5ae803462f0687a";
#     sha256 = "yv46KKe1FFkllG2aqvrIzpl92kP0v0Lt3Epl40U5fjE=";
#   };
# in
import <nixpkgs> {
  config.android_sdk.accept_license = true;
  config.allowUnfree = true;
  overlays = [ (self: super: (import ./sdk.nix) self super) ];
}
