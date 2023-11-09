cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-jyutping VERSION 0.0.8)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

find_package(libime REQUIRED CONFIG)
get_target_property(LIBIME_CMAKE_MODULES libime::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${LIBIME_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(LibIMECore MODULE)

find_package(fcitx5-chinese-addons REQUIRED CONFIG)
get_target_property(FCITX5_CHINESE_ADDONS_CMAKE_MODULES fcitx5-chinese-addons::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CHINESE_ADDONS_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(Fcitx5ModulePunctuation MODULE)

# prebuilt dir. at least it works.
set(PREBUILT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../lib/fcitx5/src/main/cpp/prebuilt")

# prebuilt fmt
set(fmt_DIR "${PREBUILT_DIR}/fmt/${ANDROID_ABI}/lib/cmake/fmt")
find_package(fmt)

# prebuilt boost
list(APPEND CMAKE_FIND_ROOT_PATH "${PREBUILT_DIR}/boost/${ANDROID_ABI}/lib/cmake")
find_package(Boost 1.83.0 REQUIRED COMPONENTS iostreams CONFIG)

# prebuilt zstd
set(zstd_DIR "${PREBUILT_DIR}/zstd/${ANDROID_ABI}/lib/cmake/zstd")
find_package(zstd)
add_library(PkgConfig::ZSTD ALIAS zstd::libzstd_static)

option(ENABLE_TEST "" OFF)
add_subdirectory(libime-jyutping)
# rename to include executable in apk
set_target_properties(libime_jyutpingdict PROPERTIES OUTPUT_NAME libime_jyutpingdict.so)
