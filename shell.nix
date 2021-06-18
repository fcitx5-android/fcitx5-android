with import ./nix;
with ourSDK;
mkShell {
  buildInputs = [
    androidComposition.androidsdk
    extra-cmake-modules
    glibc
    gettext
    python39
    icu
  ];
  ANDROID_SDK_ROOT = "${androidComposition.androidsdk}/libexec/android-sdk";

  GRADLE_OPTS =
    "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidComposition.androidsdk}/libexec/android-sdk/build-tools/${buildToolsVersion}/aapt2";
  ECM_DIR = "${extra-cmake-modules}/share/ECM/cmake/";
  shellHook = ''
    export PATH="$ANDROID_SDK_ROOT/cmake/${cmakeVersion}/bin:$PATH"
  '';
}
