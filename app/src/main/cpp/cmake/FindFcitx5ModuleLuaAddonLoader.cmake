set(Fcitx5ModuleLuaAddonLoader_FOUND TRUE)
set(FCITX_INSTALL_CMAKECONFIG_DIR "${CMAKE_SOURCE_DIR}/cmake/dummy")

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5ModuleLuaAddonLoader
    FOUND_VAR
        Fcitx5ModuleLuaAddonLoader_FOUND
    REQUIRED_VARS
        FCITX_INSTALL_CMAKECONFIG_DIR
)

mark_as_advanced(Fcitx5ModuleLuaAddonLoader_FOUND FCITX_INSTALL_CMAKECONFIG_DIR)
