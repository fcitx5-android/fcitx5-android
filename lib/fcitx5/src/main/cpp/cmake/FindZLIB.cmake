find_library(ZLIB z)
add_library(ZLIB::ZLIB SHARED IMPORTED)
set_target_properties(ZLIB::ZLIB PROPERTIES IMPORTED_LOCATION ${ZLIB})
set(ZLIB_LIBRARIES ${ZLIB})
set(ZLIB_INCLUDE_DIRS "${ANDROID_TOOLCHAIN_ROOT}/sysroot/usr/include")
set(ZLIB_FOUND TRUE)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(ZLIB
    FOUND_VAR
        ZLIB_FOUND
    REQUIRED_VARS
        ZLIB_FOUND
)
