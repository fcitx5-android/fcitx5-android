{
  description = "Dev shell flake for fcitx5-android";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  inputs.flake-compat = {
    url = "github:edolstra/flake-compat";
    flake = false;
  };
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.android_sdk.accept_license = true;
          config.allowUnfree = true;
          overlays = [ self.overlays.default ];
        };
      in {
        devShells = {
          default = pkgs.fcitx5-android.sdk.shell;
          noAS =
            pkgs.fcitx5-android.sdk.shell.override { androidStudio = null; };
        };
      }) // {
        overlays.default = final: prev: {
          fcitx5-android = {
            sdk = final.lib.makeExtensible (self: {

              # Update versions here
              # This should match to build-logic/convention/src/main/kotlin/Versions.kt
              cmakeVersion = "3.22.1";
              buildToolsVersion = "34.0.0";
              platformToolsVersion = "34.0.5";
              platformVersion = "34";
              ndkVersion = "25.2.9519653";

              includeNDK = true;
              androidComposition = final.androidenv.composeAndroidPackages {
                inherit (self) platformToolsVersion ndkVersion includeNDK;
                buildToolsVersions = [ self.buildToolsVersion ];
                platformVersions = [ self.platformVersion ];
                cmakeVersions = [ self.cmakeVersion ];
                includeEmulator = false;
                includeSources = true;
              };

              shell = final.lib.makeOverridable
                ({ androidStudio, generateLocalProperties, exportCMakeBin }:
                  with final;
                  with self;
                  mkShell rec {
                    buildInputs = [
                      androidComposition.androidsdk
                      extra-cmake-modules
                      gettext
                      python39
                      icu
                      androidStudio
                    ];
                    ANDROID_SDK_ROOT =
                      "${androidComposition.androidsdk}/libexec/android-sdk";
                    ANDROID_HOME = ANDROID_SDK_ROOT;
                    NDK_VERSION = ndkVersion;
                    BUILD_TOOLS_VERSION = buildToolsVersion;
                    GRADLE_OPTS =
                      "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidComposition.androidsdk}/libexec/android-sdk/build-tools/${buildToolsVersion}/aapt2";
                    ECM_DIR = "${extra-cmake-modules}/share/ECM/cmake/";
                    JAVA_HOME = "${jdk17}";
                    shellHook = lib.optionalString exportCMakeBin ''
                      export PATH="$ANDROID_SDK_ROOT/cmake/${cmakeVersion}/bin:$PATH"
                    '' + lib.optionalString generateLocalProperties ''
                      echo sdk.dir=$ANDROID_SDK_ROOT > local.properties
                    '';
                  }) {
                    androidStudio = final.androidStudioPackages.beta;
                    generateLocalProperties = true;
                    exportCMakeBin = true;
                  };
            });
          };
        };
      };
}
