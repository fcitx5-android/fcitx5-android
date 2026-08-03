add_definitions(-DFCITX_GETTEXT_DOMAIN=\"fcitx5-android\")

add_library(pluginpanel MODULE pluginpanel.cpp)
target_link_libraries(pluginpanel native-lib Fcitx5::Core Fcitx5::Utils)

configure_file(pluginpanel.conf.in pluginpanel.conf @ONLY)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/pluginpanel.conf"
        DESTINATION "${FCITX_INSTALL_PKGDATADIR}/addon"
        COMPONENT config)
