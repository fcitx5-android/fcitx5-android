set(LibIMETable_FOUND TRUE)
set(LibIMETable_VERSION 1.0.11)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibIMETable
    FOUND_VAR
        LibIMETable_FOUND
    REQUIRED_VARS
        LibIMETable_FOUND
    VERSION_VAR
        LibIMETable_VERSION
)
