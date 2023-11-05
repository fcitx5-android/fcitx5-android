cmake_minimum_required(VERSION 3.18)

project(fcitx5-android VERSION 0.0.7)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

find_package(libime REQUIRED CONFIG)
get_target_property(LIBIME_CMAKE_MODULES libime::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${LIBIME_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(LibIMECore MODULE)
find_package(LibIMEPinyin MODULE)
find_package(LibIMETable MODULE)

find_package(fcitx5-lua REQUIRED CONFIG)
find_package(fcitx5-chinese-addons REQUIRED CONFIG)

include("${FCITX_INSTALL_CMAKECONFIG_DIR}/Fcitx5Utils/Fcitx5CompilerSettings.cmake")

add_subdirectory(po)
add_subdirectory(androidfrontend)
add_subdirectory(androidkeyboard)
add_subdirectory(androidnotification)

# prebuilt dir. at least it works.
set(PREBUILT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../lib/fcitx5/src/main/cpp/prebuilt")

# prebuilt fmt
set(fmt_DIR "${PREBUILT_DIR}/fmt/${ANDROID_ABI}/lib/cmake/fmt")
find_package(fmt)

# prebuilt libevent
set(Libevent_DIR "${PREBUILT_DIR}/libevent/${ANDROID_ABI}/lib/cmake/libevent")
find_package(Libevent)

# prebuilt boost
list(APPEND CMAKE_FIND_ROOT_PATH "${PREBUILT_DIR}/boost/${ANDROID_ABI}/lib/cmake")
find_package(Boost 1.83.0 REQUIRED COMPONENTS headers filesystem iostreams CONFIG)

set(CHINESE_ADDONS_PINYIN_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../lib/fcitx5-chinese-addons/src/main/cpp/fcitx5-chinese-addons/im/pinyin")
add_library(pinyin-customphrase STATIC "${CHINESE_ADDONS_PINYIN_DIR}/customphrase.cpp")
target_include_directories(pinyin-customphrase INTERFACE "${CHINESE_ADDONS_PINYIN_DIR}")
target_link_libraries(pinyin-customphrase PRIVATE fmt::fmt-header-only Fcitx5::Utils LibIME::Core)

add_library(native-lib SHARED native-lib.cpp androidaddonloader/androidaddonloader.cpp)
target_link_libraries(native-lib
        log
        libevent::core
        Fcitx5::Utils
        Fcitx5::Config
        Fcitx5::Core
        Fcitx5::Module::QuickPhrase
        Fcitx5::Module::Unicode
        Fcitx5::Module::Clipboard
        Boost::headers
        Boost::iostreams
        LibIME::Pinyin
        LibIME::Table
        pinyin-customphrase
        )

add_custom_target(copy-fcitx5-modules
        # fcitx5
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::clipboard,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::imselector,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::quickphrase,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::spell,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5::unicode,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        # fcitx5-lua
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5-lua::luaaddonloader,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        # fcitx5-chinese-addons
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5-chinese-addons::pinyin,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5-chinese-addons::table,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5-chinese-addons::scel2org5,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5-chinese-addons::chttrans,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5-chinese-addons::fullwidth,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5-chinese-addons::pinyinhelper,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_PROPERTY:fcitx5-chinese-addons::punctuation,IMPORTED_LOCATION> ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMENT "Copying fcitx5 module libraries to :app"
        )
