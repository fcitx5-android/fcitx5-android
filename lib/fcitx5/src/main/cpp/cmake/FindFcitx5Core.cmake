set(Fcitx5Core_FOUND TRUE)

find_package(Fcitx5Utils REQUIRED MODULE)
find_package(Fcitx5Config REQUIRED MODULE)

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)

if (NOT TARGET Fcitx5::Core)
    # fix target dependency
    set_target_properties(fcitx5::Fcitx5Core PROPERTIES INTERFACE_LINK_LIBRARIES fcitx5::Fcitx5Config)
    # fix target name
    add_library(Fcitx5::Core ALIAS fcitx5::Fcitx5Core)
endif()

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5Core
    FOUND_VAR
        Fcitx5Core_FOUND
    REQUIRED_VARS
        Fcitx5Core_FOUND
)
