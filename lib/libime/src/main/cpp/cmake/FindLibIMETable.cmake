set(LibIMETable_FOUND TRUE)

# find prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
find_package(libime REQUIRED CONFIG)

find_package(LibIMECore MODULE)

if (NOT TARGET LibIME::Table)
    # fix target dependency
    set_target_properties(libime::IMETable PROPERTIES INTERFACE_LINK_LIBRARIES fcitx5::Fcitx5Utils)
    set_target_properties(libime::IMEPinyin PROPERTIES INTERFACE_LINK_LIBRARIES libime::IMECore)
    # fix target name
    add_library(LibIME::Table ALIAS libime::IMETable)
endif()

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibIMETable
    FOUND_VAR
        LibIMETable_FOUND
    REQUIRED_VARS
        LibIMETable_FOUND
)
