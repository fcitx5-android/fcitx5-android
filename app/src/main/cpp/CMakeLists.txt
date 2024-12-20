cmake_minimum_required(VERSION 3.18)

project(fcitx5-android VERSION ${VERSION_NAME})

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

# prebuilt fmt
set(fmt_DIR "${PREBUILT_DIR}/fmt/${ANDROID_ABI}/lib/cmake/fmt")
find_package(fmt)

# prebuilt libuv
set(libuv_DIR "${PREBUILT_DIR}/libuv/${ANDROID_ABI}/lib/cmake/libuv")
find_package(libuv)

# prebuilt boost
list(APPEND CMAKE_FIND_ROOT_PATH "${PREBUILT_DIR}/boost/${ANDROID_ABI}/lib/cmake")
find_package(Boost 1.86.0 REQUIRED COMPONENTS headers iostreams CONFIG)

set(CHINESE_ADDONS_PINYIN_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../lib/fcitx5-chinese-addons/src/main/cpp/fcitx5-chinese-addons/im/pinyin")
add_library(pinyin-customphrase STATIC "${CHINESE_ADDONS_PINYIN_DIR}/customphrase.cpp")
target_include_directories(pinyin-customphrase INTERFACE "${CHINESE_ADDONS_PINYIN_DIR}")
target_link_libraries(pinyin-customphrase PRIVATE fmt::fmt-header-only Fcitx5::Utils LibIME::Core)

add_library(native-lib SHARED native-lib.cpp)
target_link_libraries(native-lib
        log
        libuv::uv_a
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

# copy module libraries from dependency lib
add_custom_target(copy-fcitx5-modules
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_PROPERTY:fcitx5::clipboard,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5::imselector,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5::quickphrase,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5::spell,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5::unicode,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5-lua::luaaddonloader,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5-chinese-addons::pinyin,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5-chinese-addons::table,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5-chinese-addons::scel2org5,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5-chinese-addons::chttrans,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5-chinese-addons::fullwidth,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5-chinese-addons::pinyinhelper,IMPORTED_LOCATION>
        $<TARGET_PROPERTY:fcitx5-chinese-addons::punctuation,IMPORTED_LOCATION>
        ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        COMMENT "Copying fcitx5 module libraries to :app"
        )

# install prebuilt assets
install(FILES "${PREBUILT_DIR}/spell-dict/en_dict.fscd" DESTINATION "${FCITX_INSTALL_PKGDATADIR}/spell" COMPONENT prebuilt-assets)
install(FILES "${PREBUILT_DIR}/chinese-addons-data/pinyin/chaizi.dict" DESTINATION "${FCITX_INSTALL_PKGDATADIR}/pinyin" COMPONENT prebuilt-assets)
install(DIRECTORY "${PREBUILT_DIR}/chinese-addons-data/pinyinhelper" DESTINATION "${FCITX_INSTALL_PKGDATADIR}" COMPONENT prebuilt-assets)
install(DIRECTORY "${PREBUILT_DIR}/libime/table" DESTINATION "${FCITX_INSTALL_PKGDATADIR}" COMPONENT prebuilt-assets)
install(DIRECTORY "${PREBUILT_DIR}/libime/data/" DESTINATION "${FCITX_INSTALL_DATADIR}/libime" COMPONENT prebuilt-assets)
install(DIRECTORY "${PREBUILT_DIR}/opencc/data/" DESTINATION "${FCITX_INSTALL_DATADIR}/opencc" COMPONENT prebuilt-assets)
