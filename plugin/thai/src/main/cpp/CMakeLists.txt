cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-thai VERSION ${VERSION_NAME})

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

set(Iconv_LIBRARY "${PREBUILT_DIR}/libiconv/${ANDROID_ABI}/lib/libiconv.a")
set(Iconv_INCLUDE_DIR "${PREBUILT_DIR}/libiconv/${ANDROID_ABI}/include")

add_library(Thai_static STATIC IMPORTED)
set_target_properties(Thai_static PROPERTIES
        IMPORTED_LOCATION "${PREBUILT_DIR}/libthai/${ANDROID_ABI}/lib/libthai.a"
        INTERFACE_INCLUDE_DIRECTORIES "${PREBUILT_DIR}/libthai/${ANDROID_ABI}/include"
        )

set(THAI_TARGET Thai_static)
add_subdirectory(fcitx5-libthai)
