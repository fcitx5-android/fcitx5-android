cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-rime VERSION 0.0.7)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

set(PREBUILT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../lib/fcitx5/src/main/cpp/prebuilt")
set(RIME_DATA_DIR "${CMAKE_INSTALL_DATADIR}/rime-data")
set(RIME_TARGET
    "${PREBUILT_DIR}/lua/${ANDROID_ABI}/lib/liblua_static.a"
    "-Wl,--whole-archive"
    "${PREBUILT_DIR}/librime/${ANDROID_ABI}/lib/librime.a"
    "-Wl,--no-whole-archive"
    "${PREBUILT_DIR}/glog/${ANDROID_ABI}/lib/libglog.a"
    "${PREBUILT_DIR}/boost/${ANDROID_ABI}/lib/libboost_filesystem.a"
    "${PREBUILT_DIR}/yaml-cpp/${ANDROID_ABI}/lib/libyaml-cpp.a"
    "${PREBUILT_DIR}/leveldb/${ANDROID_ABI}/lib/libleveldb.a"
    "${PREBUILT_DIR}/marisa/${ANDROID_ABI}/lib/libmarisa.a"
    "${PREBUILT_DIR}/opencc/${ANDROID_ABI}/lib/libopencc.a"
    log)
set(Rime_VERSION "1.9.0")
add_definitions("-I${PREBUILT_DIR}/librime/${ANDROID_ABI}/include")
add_subdirectory(fcitx5-rime)
