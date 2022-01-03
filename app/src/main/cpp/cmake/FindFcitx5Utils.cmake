set(Fcitx5Utils_FOUND TRUE)
set(FCITX_INSTALL_CMAKECONFIG_DIR "${CMAKE_SOURCE_DIR}/cmake")

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5Utils
    FOUND_VAR
        Fcitx5Utils_FOUND
    REQUIRED_VARS
        FCITX_INSTALL_CMAKECONFIG_DIR
)
