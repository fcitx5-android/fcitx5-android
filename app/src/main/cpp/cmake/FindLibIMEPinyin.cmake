set(LibIMEPinyin_FOUND TRUE)
set(FCITX_INSTALL_CMAKECONFIG_DIR "${CMAKE_SOURCE_DIR}/cmake/dummy")

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibIMEPinyin
    FOUND_VAR
        LibIMEPinyin_FOUND
    REQUIRED_VARS
        FCITX_INSTALL_CMAKECONFIG_DIR
)

mark_as_advanced(LibIMEPinyin_FOUND FCITX_INSTALL_CMAKECONFIG_DIR)
