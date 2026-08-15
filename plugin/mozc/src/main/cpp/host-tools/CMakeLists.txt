cmake_minimum_required(VERSION 3.18)

project(mozc-host-tools LANGUAGES C CXX)

if(NOT FCITX5_MOZC_DIR)
    message(FATAL_ERROR "FCITX5_MOZC_DIR must point to the fcitx5-mozc source tree")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(ABSL_ENABLE_INSTALL OFF)
set(ABSL_PROPAGATE_CXX_STD ON)
add_subdirectory("${FCITX5_MOZC_DIR}/abseil-cpp" abseil-cpp)

set(protobuf_INSTALL OFF)
set(protobuf_BUILD_TESTS OFF)
set(protobuf_BUILD_CONFORMANCE OFF)
set(protobuf_BUILD_EXAMPLES OFF)
set(protobuf_BUILD_SHARED_LIBS OFF)
set(protobuf_BUILD_PROTOC_BINARIES ON)
set(protobuf_BUILD_LIBUPB ON)
set(protobuf_LOCAL_DEPENDENCIES_ONLY ON)
add_subdirectory("${FCITX5_MOZC_DIR}/protobuf" protobuf)
