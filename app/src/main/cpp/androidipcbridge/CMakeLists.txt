add_definitions(-DFCITX_GETTEXT_DOMAIN=\"fcitx5-android\")

add_library(androidipcbridge MODULE androidipcbridge.cpp)
target_link_libraries(androidipcbridge Fcitx5::Core Fcitx5::Utils common::android-ipc-bridge)

configure_file(androidipcbridge.conf.in.in androidipcbridge.conf.in @ONLY)
fcitx5_translate_desktop_file(${CMAKE_CURRENT_BINARY_DIR}/androidipcbridge.conf.in androidipcbridge.conf)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/androidipcbridge.conf"
        DESTINATION "${FCITX_INSTALL_PKGDATADIR}/addon"
        COMPONENT config)
