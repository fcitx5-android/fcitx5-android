add_definitions(-DFCITX_GETTEXT_DOMAIN=\"fcitx5-android\")

add_library(androidnotification MODULE androidnotification.cpp)
target_link_libraries(androidnotification Fcitx5::Core Fcitx5::Utils Fcitx5::Module::Notifications)

configure_file(notifications.conf.in.in notifications.conf.in @ONLY)
fcitx5_translate_desktop_file(${CMAKE_CURRENT_BINARY_DIR}/notifications.conf.in notifications.conf)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/notifications.conf"
        DESTINATION "${FCITX_INSTALL_PKGDATADIR}/addon"
        COMPONENT config)
