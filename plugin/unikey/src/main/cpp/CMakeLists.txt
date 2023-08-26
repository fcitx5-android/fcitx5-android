cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-lib-fcitx5-lua VERSION 0.0.7)

set(CMAKE_CXX_STANDARD 17)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

# install prefix for addon conf and locale
set(CMAKE_INSTALL_PREFIX /usr)
set(FCITX_INSTALL_PKGDATADIR /usr/share/fcitx5)
set(FCITX_INSTALL_LOCALEDIR /usr/share/locale)

set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake" ${CMAKE_MODULE_PATH})

# cmake/FindECM.cmake
find_package(ECM)

option(ENABLE_TEST "" OFF)
option(ENABLE_QT "" OFF)
add_subdirectory(fcitx5-unikey)
# suppress "illegal character encoding in character literal" warning in unikey/data.cpp
target_compile_options(unikey-lib PRIVATE "-Wno-invalid-source-encoding")
