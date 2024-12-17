cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-hangul VERSION ${VERSION_NAME})

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

add_library(Hangul_static STATIC IMPORTED)
set_target_properties(Hangul_static PROPERTIES
        IMPORTED_LOCATION "${PREBUILT_DIR}/libhangul/${ANDROID_ABI}/lib/libhangul.a"
        INTERFACE_INCLUDE_DIRECTORIES "${PREBUILT_DIR}/libhangul/${ANDROID_ABI}/include/hangul-1.0"
        )

set(HANGUL_TARGET Hangul_static)
option(ENABLE_TEST "" OFF)
add_subdirectory(fcitx5-hangul)

install(DIRECTORY "${PREBUILT_DIR}/libhangul/data/libhangul" DESTINATION "${FCITX_INSTALL_DATADIR}" COMPONENT prebuilt-assets)
