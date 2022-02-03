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
    androidStudioPackages.beta
  ];
  ANDROID_SDK_ROOT = "${androidComposition.androidsdk}/libexec/android-sdk";
  NDK_VERSION = ndkVersion;
  GRADLE_OPTS =
    "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidComposition.androidsdk}/libexec/android-sdk/build-tools/${buildToolsVersion}/aapt2";
  ECM_DIR = "${extra-cmake-modules}/share/ECM/cmake/";
  JAVA_HOME = "${jdk11}";
  shellHook = ''
    export PATH="$ANDROID_SDK_ROOT/cmake/${cmakeVersion}/bin:$PATH"
    echo sdk.dir=$ANDROID_SDK_ROOT > local.properties
  '';
}
