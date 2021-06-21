set(Fcitx5Core_FOUND TRUE)
set(Fcitx5Core_VERSION 5.0.4)
set(FCITX_INSTALL_CMAKECONFIG_DIR "${CMAKE_SOURCE_DIR}/cmake/dummy")

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5Core
    FOUND_VAR
        Fcitx5Core_FOUND
    REQUIRED_VARS
        FCITX_INSTALL_CMAKECONFIG_DIR
    VERSION_VAR
        Fcitx5Core_VERSION
)

mark_as_advanced(Fcitx5Core_FOUND FCITX_INSTALL_CMAKECONFIG_DIR Fcitx5Core_VERSION)
