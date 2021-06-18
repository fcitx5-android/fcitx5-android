let
  hostPkgs = import <nixpkgs> { };
  pinnedPkgs = hostPkgs.fetchFromGitHub {
    owner = "berberman";
    repo = "nixpkgs";
    rev = "43af7a9fba97e51ef23ea0eab9359b51892ef7cc";
    sha256 = "0wl0r7richxwsh7cggmici3sk8qz82wg97w30kq4v55am2wgdwlk";
  };
in import pinnedPkgs {
  config.android_sdk.accept_license = true;
  overlays = [ (self: super: (import ./sdk.nix) self super) ];
}
