cmake_minimum_required(VERSION 3.18)

project(fcitx-lib)

set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake" ${CMAKE_MODULE_PATH})

# prebuilt fmt
set(fmt_DIR "${CMAKE_SOURCE_DIR}/prebuilt/fmt/${ANDROID_ABI}/lib/cmake/fmt")

# prebuilt libintl-lite
set(LibIntl_DIR "${CMAKE_SOURCE_DIR}/prebuilt/libintl-lite/${ANDROID_ABI}/lib/cmake")
find_package(LibIntl)
add_library(LibIntl::LibIntl ALIAS intl)

# prebuilt libevent
set(Libevent_DIR "${CMAKE_SOURCE_DIR}/prebuilt/libevent/${ANDROID_ABI}/lib/cmake/libevent")
set(LIBEVENT_STATIC_LINK ON)
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

add_subdirectory(androidfrontend)

# prebuilt boost
set(BOOST_VERSION "1.74.0")
set(BOOST_MODULES filesystem iostreams regex)
set(BOOST_ROOT "${CMAKE_SOURCE_DIR}/prebuilt/boost/${ANDROID_ABI}")

set(Boost_DIR "${BOOST_ROOT}/lib/cmake/Boost-${BOOST_VERSION}/")

set(boost_headers_DIR "${BOOST_ROOT}/lib/cmake/boost_headers-${BOOST_VERSION}/")
include_directories("${BOOST_ROOT}/include")
add_library(Boost::boost INTERFACE IMPORTED)

foreach(mod IN LISTS BOOST_MODULES)
    set("boost_${mod}_DIR" "${BOOST_ROOT}/lib/cmake/boost_${mod}-${BOOST_VERSION}/")
endforeach()

option(ENABLE_TEST "" OFF)
set(_Fcitx5Macro_SELF_DIR ${CMAKE_CURRENT_SOURCE_DIR}/fcitx5/src/lib/fcitx-utils)
add_subdirectory(libime)

option(ENABLE_TEST "" OFF)
option(ENABLE_OPENCC "" OFF)
option(ENABLE_GUI "" OFF)
option(ENABLE_BROWSER "" OFF)
option(USE_WEBKIT "" OFF)
add_subdirectory(fcitx5-chinese-addons)

add_library(native-lib SHARED native-lib.cpp)
find_library(Libc c)
find_library(AndroidLog log)
target_link_libraries(native-lib ${Libc} ${AndroidLog} Fcitx5::Core)
