add_fcitx5_addon(cloudpinyin androidcloudpinyin.cpp)
target_compile_definitions(cloudpinyin PRIVATE -DFCITX_GETTEXT_DOMAIN=\"fcitx5-chinese-addons\")
target_link_libraries(cloudpinyin Fcitx5::Core Fcitx5::Config common::android-ipc-bridge)
install(TARGETS cloudpinyin DESTINATION "${CMAKE_INSTALL_LIBDIR}/fcitx5")

configure_file(cloudpinyin.conf.in.in cloudpinyin.conf.in)
fcitx5_translate_desktop_file(
        "${CMAKE_CURRENT_BINARY_DIR}/cloudpinyin.conf.in" cloudpinyin.conf
        PO_DIRECTORY
        "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../lib/fcitx5-chinese-addons/src/main/cpp/fcitx5-chinese-addons/po")
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/cloudpinyin.conf"
        DESTINATION "${FCITX_INSTALL_PKGDATADIR}/addon" COMPONENT config)
