cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-mozc VERSION ${VERSION_NAME})

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

add_library(Mozc_static STATIC IMPORTED)
set_target_properties(Mozc_static PROPERTIES
        IMPORTED_LOCATION "${PREBUILT_DIR}/libmozc/${ANDROID_ABI}/lib/libmozc-static.a"
        INTERFACE_INCLUDE_DIRECTORIES "${PREBUILT_DIR}/libmozc/${ANDROID_ABI}/include/mozc"
        INTERFACE_LINK_LIBRARIES "-llog -L${PREBUILT_DIR}/libmozc/${ANDROID_ABI}/lib -labsl -lprotobuf -lutf8_validity"
        )

set(MOZC_TARGET Mozc_static)
add_subdirectory(fcitx5-mozc)

target_include_directories(mozc PRIVATE
        "${PROJECT_SOURCE_DIR}/../../../../../lib/fcitx5/build/headers/usr/include"
        "${PROJECT_SOURCE_DIR}/../../../../../lib/fcitx5/build/headers/usr/include/Fcitx5/Module"
        )
