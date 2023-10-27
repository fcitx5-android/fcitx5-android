cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-chewing VERSION 0.0.7)

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

add_library(Chewing_static STATIC IMPORTED)
set_target_properties(Chewing_static PROPERTIES
        IMPORTED_LOCATION "${PREBUILT_DIR}/libchewing/${ANDROID_ABI}/lib/libchewing.a"
        INTERFACE_INCLUDE_DIRECTORIES "${PREBUILT_DIR}/libchewing/${ANDROID_ABI}/include/chewing"
        )

set(CHEWING_TARGET Chewing_static)
add_subdirectory(fcitx5-chewing)
