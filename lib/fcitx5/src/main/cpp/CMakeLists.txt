cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-lib-fcitx5 VERSION 0.0.7)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# dummy target to export src/main/cpp/cmake
add_custom_target(cmake)
# dummy target to export "notifications_public.h"
add_custom_target(notifications)

set(CMAKE_INSTALL_PREFIX /usr)
# fcitx-utils/standardpath.cpp uses FCITX_INSTALL_LIBDATADIR,
# which is CMAKE_INSTALL_LIBDATADIR's absolute path
set(CMAKE_INSTALL_LIBDATADIR /usr/lib)
set(FCITX_INSTALL_PKGDATADIR /usr/share/fcitx5)
set(FCITX_INSTALL_LOCALEDIR /usr/share/locale)

set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake" ${CMAKE_MODULE_PATH})

# cmake/FindECM.cmake
find_package(ECM)

# prebuilt dir
set(PREBUILT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/prebuilt")

# prebuilt fmt
set(fmt_DIR "${PREBUILT_DIR}/fmt/${ANDROID_ABI}/lib/cmake/fmt")
find_package(fmt)

# prebuilt libintl-lite
set(LibIntl_DIR "${PREBUILT_DIR}/libintl-lite/${ANDROID_ABI}/lib/cmake")
find_package(LibIntl)

# prebuilt libevent
set(Libevent_DIR "${PREBUILT_DIR}/libevent/${ANDROID_ABI}/lib/cmake/libevent")
find_package(Libevent)

option(ENABLE_TEST "" OFF)
option(ENABLE_COVERAGE "" OFF)
option(ENABLE_ENCHANT "" OFF)
option(ENABLE_X11 "" OFF)
option(ENABLE_WAYLAND "" OFF)
option(ENABLE_DBUS "" OFF)
option(ENABLE_DOC "" OFF)
option(ENABLE_SERVER "" OFF)
option(ENABLE_KEYBOARD "" OFF)
option(USE_SYSTEMD "" OFF)
option(ENABLE_XDGAUTOSTART "" OFF)
option(ENABLE_EMOJI "" OFF)
option(ENABLE_LIBUUID "" OFF)
add_subdirectory(fcitx5)
