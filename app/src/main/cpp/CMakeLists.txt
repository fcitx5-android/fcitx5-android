cmake_minimum_required(VERSION 3.18)

project(fcitx5-android VERSION 0.0.6)

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

add_subdirectory(po)
add_subdirectory(androidfrontend)
add_subdirectory(androidkeyboard)

# prebuilt dir. at least it works.
set(PREBUILT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../lib/fcitx5/src/main/cpp/prebuilt")

# prebuilt fmt
set(fmt_DIR "${PREBUILT_DIR}/fmt/${ANDROID_ABI}/lib/cmake/fmt")
find_package(fmt)

# prebuilt libevent
set(Libevent_DIR "${PREBUILT_DIR}/libevent/${ANDROID_ABI}/lib/cmake/libevent")
find_package(Libevent)

# prebuilt boost
set(BOOST_VERSION "1.80.0")
set(BOOST_MODULES headers filesystem atomic iostreams)
set(BOOST_ROOT "${PREBUILT_DIR}/boost/${ANDROID_ABI}")
set(Boost_DIR "${BOOST_ROOT}/lib/cmake/Boost-${BOOST_VERSION}")
foreach(mod IN LISTS BOOST_MODULES)
    set("boost_${mod}_DIR" "${BOOST_ROOT}/lib/cmake/boost_${mod}-${BOOST_VERSION}")
endforeach()

option(ENABLE_TEST "" OFF)
set(LIBIME_INSTALL_PKGDATADIR table)
add_subdirectory(libime)
# kenlm/util/exception.hh uses __FILE__ macro
target_compile_options(kenlm PRIVATE "-ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}/libime/src/libime/core/kenlm=.")

# prebuilt lua
set(Lua_DIR "${PREBUILT_DIR}/lua/${ANDROID_ABI}/share/cmake/Lua")
find_package(Lua CONFIG)

# we are using static linking
option(USE_DLOPEN "" OFF)
add_subdirectory(fcitx5-lua)

# prebuilt opencc
set(OpenCC_DIR "${PREBUILT_DIR}/opencc/${ANDROID_ABI}/lib/cmake/opencc")
find_package(OpenCC)

option(ENABLE_TEST "" OFF)
option(ENABLE_GUI "" OFF)
option(ENABLE_BROWSER "" OFF)
option(USE_WEBKIT "" OFF)
option(ENABLE_CLOUDPINYIN "" OFF)
# prefer OpenCC_DIR rather than fcitx5-chinese-addons/cmake/FindOpenCC.cmake
set(CMAKE_FIND_PACKAGE_PREFER_CONFIG ON)
add_subdirectory(fcitx5-chinese-addons)
# rename to include executable in apk
set_target_properties(scel2org5 PROPERTIES OUTPUT_NAME libscel2org5.so)

option(ENABLE_TEST "" OFF)
option(ENABLE_QT "" OFF)
add_subdirectory(fcitx5-unikey)
# suppress "illegal character encoding in character literal" warning in unikey/data.cpp
target_compile_options(unikey-lib PRIVATE "-Wno-invalid-source-encoding")

add_library(native-lib SHARED native-lib.cpp)
target_link_libraries(native-lib
        log
        libevent::core
        Fcitx5::Utils
        Fcitx5::Config
        Fcitx5::Core
        Fcitx5::Module::QuickPhrase
        Fcitx5::Module::Unicode
        Fcitx5::Module::Clipboard
        LibIME::Pinyin
        LibIME::Table)

add_custom_target(copy-fcitx5-modules
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::clipboard,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::imselector,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::quickphrase,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::spell,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::unicode,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMENT "Copying fcitx5 module libraries to :app"
        )
