include(GNUInstallDirs)

set(ANDROID_PUNCTUATION_DATA_FILES
        punc.mb.zh_CN
        )

install(FILES ${ANDROID_PUNCTUATION_DATA_FILES} DESTINATION "${CMAKE_INSTALL_DATADIR}/fcitx5/punctuation"
        COMPONENT config)
