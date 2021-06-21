set(LibIMETable_FOUND TRUE)
set(LibIMETable_VERSION 1.0.3)
set(FCITX_INSTALL_CMAKECONFIG_DIR "${CMAKE_SOURCE_DIR}/cmake/dummy")

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibIMETable
    FOUND_VAR
        LibIMETable_FOUND
    REQUIRED_VARS
        FCITX_INSTALL_CMAKECONFIG_DIR
    VERSION_VAR
        LibIMETable_VERSION
)

mark_as_advanced(LibIMETable_FOUND FCITX_INSTALL_CMAKECONFIG_DIR LibIMETable_VERSION)
