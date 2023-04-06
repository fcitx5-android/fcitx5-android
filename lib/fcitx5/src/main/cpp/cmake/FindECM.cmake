if(DEFINED ENV{ECM_DIR})
    set(ECM_DIR $ENV{ECM_DIR})
else()
    set(ECM_DIR /usr/share/ECM/cmake)
endif()

find_package(ECM REQUIRED CONFIG)
