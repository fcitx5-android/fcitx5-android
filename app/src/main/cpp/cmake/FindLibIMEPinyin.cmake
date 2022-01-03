set(LibIMEPinyin_FOUND TRUE)
set(IMEPinyin_VERSION 1.0.11)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibIMEPinyin
    FOUND_VAR
        LibIMEPinyin_FOUND
    REQUIRED_VARS
        LibIMEPinyin_FOUND
    VERSION_VAR
        IMEPinyin_VERSION
)
