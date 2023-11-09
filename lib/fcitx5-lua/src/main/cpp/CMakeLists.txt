cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-lib-fcitx5-lua VERSION 0.0.8)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

# dummy target to export src/main/cpp/cmake
add_custom_target(cmake)

# prebuilt dir. at least it works.
set(PREBUILT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../fcitx5/src/main/cpp/prebuilt")

# prebuilt lua
set(Lua_DIR "${PREBUILT_DIR}/lua/${ANDROID_ABI}/share/cmake/Lua")
find_package(Lua CONFIG)

# we are using static linking
option(USE_DLOPEN "" OFF)
option(ENABLE_TEST "" OFF)
add_subdirectory(fcitx5-lua)
