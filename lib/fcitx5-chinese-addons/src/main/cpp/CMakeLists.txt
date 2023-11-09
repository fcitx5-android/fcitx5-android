cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-lib-fcitx5-chinese-addons VERSION 0.0.8)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

find_package(libime REQUIRED CONFIG)
get_target_property(LIBIME_CMAKE_MODULES libime::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${LIBIME_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(LibIMECore MODULE)
find_package(LibIMEPinyin MODULE)
find_package(LibIMETable MODULE)

find_package(fcitx5-lua REQUIRED CONFIG)
get_target_property(FCITX5_LUA_CMAKE_MODULES fcitx5-lua::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_LUA_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(Fcitx5ModuleLuaAddonLoader MODULE)

# dummy target to export src/main/cpp/cmake
add_custom_target(cmake)

# prebuilt dir. at least it works.
set(PREBUILT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../fcitx5/src/main/cpp/prebuilt")

# prebuilt fmt
set(fmt_DIR "${PREBUILT_DIR}/fmt/${ANDROID_ABI}/lib/cmake/fmt")
find_package(fmt)

# prebuilt boost
list(APPEND CMAKE_FIND_ROOT_PATH "${PREBUILT_DIR}/boost/${ANDROID_ABI}/lib/cmake")
find_package(Boost 1.83.0 REQUIRED COMPONENTS iostreams CONFIG)

# prebuilt marisa-tire, OpenCC needs it
set(marisa_DIR "${PREBUILT_DIR}/marisa/${ANDROID_ABI}/lib/cmake/marisa")
find_package(marisa)

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
