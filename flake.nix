{
  description = "Dev shell flake for fcitx5-android";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  inputs.flake-compat = {
    url = "github:edolstra/flake-compat";
    flake = false;
  };

  outputs = { self, nixpkgs, ... }:
    let
      pkgs = import nixpkgs {
        system = "x86_64-linux";
        config.android_sdk.accept_license = true;
        config.allowUnfree = true;
        overlays = [ self.overlays.default ];
      };
    in with pkgs;
    with fcitx5-android-sdk;
    {
      devShells.x86_64-linux.default = mkShell {
        buildInputs = [
          androidComposition.androidsdk
          extra-cmake-modules
          glibc
          gettext
          python39
          icu
          androidStudioPackages.beta
        ];
        ANDROID_SDK_ROOT =
          "${androidComposition.androidsdk}/libexec/android-sdk";
        NDK_VERSION = ndkVersion;
        GRADLE_OPTS =
          "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidComposition.androidsdk}/libexec/android-sdk/build-tools/${buildToolsVersion}/aapt2";
        ECM_DIR = "${extra-cmake-modules}/share/ECM/cmake/";
        JAVA_HOME = "${jdk11}";
        shellHook = ''
          export PATH="$ANDROID_SDK_ROOT/cmake/${cmakeVersion}/bin:$PATH"
          echo sdk.dir=$ANDROID_SDK_ROOT > local.properties
        '';
      };
    } // {
      overlays.default = final: prev: {
        fcitx5-android-sdk = rec {
          cmakeVersion = "3.22.1";
          buildToolsVersion = "31.0.0";
          platformToolsVersion = "33.0.2";
          platformVersion = "31";
          ndkVersion = "24.0.8215888";
          abiVersions = [ "arm64-v8a" "armeabi-v7a" ];
          androidComposition = prev.androidenv.composeAndroidPackages {
            inherit platformToolsVersion ndkVersion;
            buildToolsVersions = [ buildToolsVersion ];
            platformVersions = [ platformVersion ];
            inherit abiVersions;
            cmakeVersions = [ cmakeVersion ];
            includeNDK = true;
            includeEmulator = false;
          };
        };
      };
    };
}
