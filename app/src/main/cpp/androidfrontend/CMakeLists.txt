add_library(androidfrontend MODULE androidfrontend.cpp)
target_link_libraries(androidfrontend Fcitx5::Core)

configure_file(androidfrontend.conf.in.in androidfrontend.conf.in @ONLY)
fcitx5_translate_desktop_file(${CMAKE_CURRENT_BINARY_DIR}/androidfrontend.conf.in androidfrontend.conf)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/androidfrontend.conf"
        DESTINATION "${FCITX_INSTALL_PKGDATADIR}/addon"
        COMPONENT config)