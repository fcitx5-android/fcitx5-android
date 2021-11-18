add_library(androidkeyboard MODULE androidkeyboard.cpp)
target_link_libraries(androidkeyboard Fcitx5::Core Fcitx5::Utils)