cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-lib-fcitx5-chinese-addons VERSION 0.0.7)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Utils MODULE)

# dummy target to export src/main/cpp/cmake
add_custom_target(cmake)

# prebuilt dir. at least it works.
set(PREBUILT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../fcitx5/src/main/cpp/prebuilt")

# prebuilt fmt
set(fmt_DIR "${PREBUILT_DIR}/fmt/${ANDROID_ABI}/lib/cmake/fmt")
find_package(fmt)

# prebuilt boost
set(BOOST_VERSION "1.80.0")
set(BOOST_MODULES headers filesystem atomic iostreams)
set(BOOST_ROOT "${PREBUILT_DIR}/boost/${ANDROID_ABI}")
set(Boost_DIR "${BOOST_ROOT}/lib/cmake/Boost-${BOOST_VERSION}")
foreach (mod IN LISTS BOOST_MODULES)
    set("boost_${mod}_DIR" "${BOOST_ROOT}/lib/cmake/boost_${mod}-${BOOST_VERSION}")
endforeach ()

set(LIBIME_INSTALL_PKGDATADIR table)
option(ENABLE_TEST "" OFF)
add_subdirectory(libime)
# kenlm/util/exception.hh uses __FILE__ macro
target_compile_options(kenlm PRIVATE "-ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}/libime/src/libime/core/kenlm=.")
