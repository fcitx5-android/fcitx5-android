add_library(androidfrontend MODULE androidfrontend.cpp)
target_link_libraries(androidfrontend Fcitx5::Core)
set_target_properties(androidfrontend PROPERTIES PREFIX "")
#install(TARGETS androidfrontend DESTINATION "${FCITX_INSTALL_ADDONDIR}")