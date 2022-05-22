set(Fcitx5Core_FOUND TRUE)

find_package(Fcitx5Utils REQUIRED)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5Core
    FOUND_VAR
        Fcitx5Core_FOUND
    REQUIRED_VARS
        Fcitx5Core_FOUND
)
