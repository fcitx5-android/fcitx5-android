cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-anthy VERSION ${VERSION_NAME})

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

option(ENABLE_TEST "" OFF)
set(BUILD_SHARED_LIBS OFF)
# anthy have some GCC pragma that LLVM doesn't recognize
add_compile_options("-Wno-unknown-warning-option")
add_subdirectory(anthy-cmake)
unset(BUILD_SHARED_LIBS)

set(ANTHY_TARGET anthy-unicode)
add_subdirectory(fcitx5-anthy)

install(FILES
        "${PREBUILT_DIR}/anthy-dict/anthy.dic"
        "${CMAKE_CURRENT_LIST_DIR}/anthy-unicode.conf"
        DESTINATION "${FCITX_INSTALL_DATADIR}/anthy"
        COMPONENT prebuilt-assets
        )
