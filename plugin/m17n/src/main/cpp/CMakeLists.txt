cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-m17n VERSION ${VERSION_NAME})

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

add_library(M17n_static STATIC IMPORTED)
set_target_properties(M17n_static PROPERTIES
        IMPORTED_LOCATION "${PREBUILT_DIR}/m17n-lib/${ANDROID_ABI}/lib/libm17n.a"
        INTERFACE_INCLUDE_DIRECTORIES "${PREBUILT_DIR}/m17n-lib/${ANDROID_ABI}/include"
        )

set(M17N_TARGET M17n_static)
option(ENABLE_TEST "" OFF)
add_subdirectory(fcitx5-m17n)

install(DIRECTORY "${PREBUILT_DIR}/m17n-db/" DESTINATION "${FCITX_INSTALL_DATADIR}/m17n" COMPONENT prebuilt-assets)
