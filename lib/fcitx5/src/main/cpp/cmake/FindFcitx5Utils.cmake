set(Fcitx5Utils_FOUND TRUE)
set(FCITX_INSTALL_CMAKECONFIG_DIR "${CMAKE_CURRENT_LIST_DIR}")

# find prefab dependency
find_package(fcitx5 REQUIRED CONFIG)

if (NOT TARGET Fcitx5::Utils)
    # fix target name
    add_library(Fcitx5::Utils ALIAS fcitx5::Fcitx5Utils)
endif()

# fcitx5_translate_desktop_file needs ${GETTEXT_MSGFMT_EXECUTABLE}
find_package(Gettext REQUIRED)
include("${CMAKE_CURRENT_LIST_DIR}/Fcitx5Utils/Fcitx5Macros.cmake")

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5Utils
    FOUND_VAR
        Fcitx5Utils_FOUND
    REQUIRED_VARS
        FCITX_INSTALL_CMAKECONFIG_DIR
)
