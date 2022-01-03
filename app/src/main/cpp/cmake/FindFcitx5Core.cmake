set(Fcitx5Core_FOUND TRUE)
set(Fcitx5Core_VERSION 5.0.11)

find_package(Fcitx5Utils REQUIRED)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5Core
    FOUND_VAR
        Fcitx5Core_FOUND
    REQUIRED_VARS
        Fcitx5Core_FOUND
    VERSION_VAR
        Fcitx5Core_VERSION
)
