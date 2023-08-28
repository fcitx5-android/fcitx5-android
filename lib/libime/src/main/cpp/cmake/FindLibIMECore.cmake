set(LibIMECore_FOUND TRUE)

# find prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
find_package(libime REQUIRED CONFIG)

if (NOT TARGET LibIME::Core)
    # fix target dependency
    set_target_properties(libime::IMECore PROPERTIES INTERFACE_LINK_LIBRARIES fcitx5::Fcitx5Utils)
    # fix target name
    add_library(LibIME::Core ALIAS libime::IMECore)
endif()

# libime/src/libime/core/LibIMECoreConfig.cmake.in
set(LIBIME_INSTALL_PKGDATADIR table)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibIMECore
    FOUND_VAR
        LibIMECore_FOUND
    REQUIRED_VARS
        LibIMECore_FOUND
)
