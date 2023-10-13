add_definitions(-DFCITX_GETTEXT_DOMAIN=\"fcitx5-android\")

add_library(androidkeyboard MODULE androidkeyboard.cpp)
target_link_libraries(androidkeyboard Fcitx5::Core Fcitx5::Utils Fcitx5::Module::Spell Fcitx5::Module::Emoji)

configure_file(androidkeyboard.conf.in.in androidkeyboard.conf.in @ONLY)
fcitx5_translate_desktop_file(${CMAKE_CURRENT_BINARY_DIR}/androidkeyboard.conf.in androidkeyboard.conf)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/androidkeyboard.conf"
        DESTINATION "${FCITX_INSTALL_PKGDATADIR}/addon"
        COMPONENT config)