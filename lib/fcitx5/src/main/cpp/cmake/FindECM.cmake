if(DEFINED ENV{ECM_DIR})
    set(ECM_DIR $ENV{ECM_DIR})
elseif(CMAKE_HOST_APPLE)
    set(ECM_DIR /opt/homebrew/share/ECM/cmake)
else()
    set(ECM_DIR /usr/share/ECM/cmake)
endif()

find_package(ECM REQUIRED CONFIG)
