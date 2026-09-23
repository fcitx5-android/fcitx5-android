cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-cloud-pinyin VERSION ${VERSION_NAME})

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(Fcitx5Core MODULE)

include("${FCITX_INSTALL_CMAKECONFIG_DIR}/Fcitx5Utils/Fcitx5CompilerSettings.cmake")

find_package(common)

# prebuilt nlohmann_json
set(nlohmann_json_DIR "${PREBUILT_DIR}/nlohmann_json/${ANDROID_ABI}/share/cmake/nlohmann_json")
find_package(nlohmann_json)

add_subdirectory(androidcloudpinyin)
