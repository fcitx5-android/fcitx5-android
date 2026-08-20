cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-lib-common VERSION ${VERSION_NAME})

add_library(android-ipc-bridge INTERFACE headers/androidipcbridge_public.h)
