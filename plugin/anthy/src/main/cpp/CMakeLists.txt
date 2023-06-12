cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-anthy VERSION 0.0.6)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

# install prefix for addon conf and locale
set(FCITX_INSTALL_PKGDATADIR /usr/share/fcitx5)
set(FCITX_INSTALL_LOCALEDIR /usr/share/locale)

set(BUILD_SHARED_LIBS OFF)
add_subdirectory(anthy-cmake)
unset(BUILD_SHARED_LIBS)

set(ANTHY_TARGET anthy-unicode)
add_subdirectory(fcitx5-anthy)
