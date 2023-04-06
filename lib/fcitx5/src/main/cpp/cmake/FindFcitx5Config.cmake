set(Fcitx5Config_FOUND TRUE)

find_package(Fcitx5Utils REQUIRED MODULE)

# find prefab dependency
find_package(fcitx5 REQUIRED CONFIG)

if (NOT TARGET Fcitx5::Config)
    # fix target dependency
    set_target_properties(fcitx5::Fcitx5Config PROPERTIES INTERFACE_LINK_LIBRARIES fcitx5::Fcitx5Utils)
    # fix target name
    add_library(Fcitx5::Config ALIAS fcitx5::Fcitx5Config)
endif()

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5Config
    FOUND_VAR
        Fcitx5Config_FOUND
    REQUIRED_VARS
        Fcitx5Config_FOUND
)
