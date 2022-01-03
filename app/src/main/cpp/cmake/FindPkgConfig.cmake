set(PkgConfig_FOUND TRUE)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(PkgConfig
    FOUND_VAR
        PkgConfig_FOUND
    REQUIRED_VARS
        PkgConfig_FOUND
)
