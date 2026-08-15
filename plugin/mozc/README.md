# Fcitx5 for Android :: Mozc Plugin

Japanese input, powered by [Mozc](https://github.com/google/mozc) via
[fcitx5-mozc](https://github.com/fcitx-contrib/fcitx5-mozc).

## Fetch sources

Mozc sources (mozc itself, abseil-cpp and protobuf) are big, so the
`plugin/mozc/src/main/cpp/fcitx5-mozc` submodule is marked `update = none` and is not
fetched by `git submodule update --init --recursive`. The gradle module is only enabled
when the submodule has been checked out. To fetch it, run in the repository root:

```shell
git submodule update --init --recursive --checkout plugin/mozc/src/main/cpp/fcitx5-mozc
```

## How it is built

There are two ways to provide the mozc converter library:

1. **Prebuilt static library** (used automatically when present): put `libmozc-static.a`
   together with its headers and the `libabsl.a`/`libprotobuf.a`/`libutf8_validity.a`
   archives under `lib/fcitx5/src/main/cpp/prebuilt/libmozc/<abi>/{lib,include/mozc}`,
   built with [prebuilder](https://github.com/fcitx5-android/prebuilder).

2. **Build from source** (default when no prebuilt library is found). During CMake
   configuration the build will additionally:
   - build `protoc` for the build host (`plugin/mozc/build/mozc/host`), since protoc
     generated code must be produced by a host-runnable binary during cross compilation.
     Pass `-DPROTOC_EXECUTABLE=/path/to/protoc` to CMake to use an existing protoc
     matching the protobuf version pinned by fcitx5-mozc;
   - download the pregenerated dictionary data `mozc_data.inc` from
     [fcitx5-mozc releases](https://github.com/fcitx-contrib/fcitx5-mozc/releases/tag/latest)
     to `plugin/mozc/build/mozc/mozc_data.inc` (place it there manually when building
     offline). Its checksum is pinned alongside the fcitx5-mozc revision, and the
     dictionary is embedded into `libmozc.so`.

Requirements on top of the main project (CMake, NDK, extra-cmake-modules, gettext):
a host C++ toolchain and `python3` (for mozc code generation scripts).
