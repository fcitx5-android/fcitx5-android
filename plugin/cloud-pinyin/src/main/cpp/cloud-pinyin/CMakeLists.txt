get_filename_component(FCITX5_ANDROID_ROOT
                       "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../.." ABSOLUTE)
set(FCITX5_CHINESE_ADDONS_SOURCE_DIR
    "${FCITX5_ANDROID_ROOT}/lib/fcitx5-chinese-addons/src/main/cpp/fcitx5-chinese-addons")

add_fcitx5_addon(cloudpinyin cloudpinyin.cpp)
target_compile_definitions(cloudpinyin PRIVATE -DFCITX_GETTEXT_DOMAIN=\"fcitx5-chinese-addons\")
target_link_libraries(cloudpinyin Fcitx5::Core Fcitx5::Config android log)
target_compile_features(cloudpinyin PRIVATE cxx_std_20)
target_include_directories(
        cloudpinyin PRIVATE
        "${FCITX5_CHINESE_ADDONS_SOURCE_DIR}/modules/cloudpinyin")
install(TARGETS cloudpinyin DESTINATION "${CMAKE_INSTALL_LIBDIR}/fcitx5")

configure_file(cloudpinyin.conf.in.in cloudpinyin.conf.in)
fcitx5_translate_desktop_file(
        "${CMAKE_CURRENT_BINARY_DIR}/cloudpinyin.conf.in" cloudpinyin.conf
        PO_DIRECTORY
        "${FCITX5_CHINESE_ADDONS_SOURCE_DIR}/po")
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/cloudpinyin.conf"
        DESTINATION "${FCITX_INSTALL_PKGDATADIR}/addon" COMPONENT config)
