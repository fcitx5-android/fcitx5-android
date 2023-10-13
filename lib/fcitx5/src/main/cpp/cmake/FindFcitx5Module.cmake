set(Fcitx5Module_FOUND TRUE)

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)

set(FCITX5_MODULE_NAMES Emoji Clipboard IMSelector Notifications QuickPhrase Spell Unicode)
foreach(Mod IN LISTS FCITX5_MODULE_NAMES)
    string(TOLOWER "${Mod}" mod)
    set(MOD_INTERFACE "fcitx5-module-${mod}-interface")
    if (NOT TARGET "${MOD_INTERFACE}")
        # library target cannot have ":" in their name
        add_library("${MOD_INTERFACE}" INTERFACE)
        if (TARGET "fcitx5::${mod}")
            get_target_property(MOD_INCLUDE_DIR "fcitx5::${mod}" INTERFACE_INCLUDE_DIRECTORIES)
            set_target_properties("${MOD_INTERFACE}" PROPERTIES INTERFACE_INCLUDE_DIRECTORIES "${MOD_INCLUDE_DIR}")
        endif()
        add_library("Fcitx5::Module::${Mod}" ALIAS "${MOD_INTERFACE}")
    endif()
endforeach()

# fix target dependency
set_target_properties(fcitx5::emoji PROPERTIES INTERFACE_LINK_LIBRARIES ZLIB::ZLIB)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Fcitx5Module
    FOUND_VAR
        Fcitx5Module_FOUND
    REQUIRED_VARS
        Fcitx5Module_FOUND
)
