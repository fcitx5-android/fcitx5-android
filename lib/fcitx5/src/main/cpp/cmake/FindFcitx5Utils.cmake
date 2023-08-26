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

set(CMAKE_INSTALL_PREFIX /usr)
# fcitx5/src/lib/fcitx-utils/Fcitx5UtilsConfig.cmake.in
set(FCITX_INSTALL_USE_FCITX_SYS_PATHS ON)
set(FCITX_INSTALL_PREFIX "/usr")
set(FCITX_INSTALL_INCLUDEDIR "/usr/include")
set(FCITX_INSTALL_LIBDIR "/usr/lib")
set(FCITX_INSTALL_LIBDATADIR "/usr/lib")
set(FCITX_INSTALL_LIBEXECDIR "/usr/lib")
set(FCITX_INSTALL_DATADIR "/usr/share")
set(FCITX_INSTALL_PKGDATADIR "/usr/share/fcitx5")
set(FCITX_INSTALL_BINDIR "/usr/bin")
set(FCITX_INSTALL_LOCALEDIR "/usr/share/locale")
set(FCITX_INSTALL_ADDONDIR "/usr/lib/fcitx5")
set(FCITX_INSTALL_MODULE_HEADER_DIR "/usr/include/Fcitx5/Module/fcitx-module")

include("${CMAKE_CURRENT_LIST_DIR}/Fcitx5Utils/Fcitx5Macros.cmake")

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5Utils
    FOUND_VAR
        Fcitx5Utils_FOUND
    REQUIRED_VARS
        FCITX_INSTALL_CMAKECONFIG_DIR
)
