set(LibIMEPinyin_FOUND TRUE)

# find prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
find_package(libime REQUIRED CONFIG)

find_package(LibIMECore MODULE)

if (NOT TARGET LibIME::Pinyin)
    # fix target dependency
    set_target_properties(libime::IMEPinyin PROPERTIES INTERFACE_LINK_LIBRARIES fcitx5::Fcitx5Utils)
    set_target_properties(libime::IMEPinyin PROPERTIES INTERFACE_LINK_LIBRARIES libime::IMECore)
    # fix target name
    add_library(LibIME::Pinyin ALIAS libime::IMEPinyin)
endif()

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibIMEPinyin
    FOUND_VAR
        LibIMEPinyin_FOUND
    REQUIRED_VARS
        LibIMEPinyin_FOUND
)
