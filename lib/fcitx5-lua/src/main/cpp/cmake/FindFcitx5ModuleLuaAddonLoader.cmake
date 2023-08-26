set(Fcitx5ModuleLuaAddonLoader_FOUND TRUE)

# find prefab dependency
find_package(fcitx5-lua REQUIRED CONFIG)

if (NOT TARGET Fcitx5::Module::LuaAddonLoader)
    # fix target name
    add_library(Fcitx5::Module::LuaAddonLoader ALIAS fcitx5-lua::luaaddonloader)
endif()

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5ModuleLuaAddonLoader
    FOUND_VAR
        Fcitx5ModuleLuaAddonLoader_FOUND
    REQUIRED_VARS
        Fcitx5ModuleLuaAddonLoader_FOUND
)
