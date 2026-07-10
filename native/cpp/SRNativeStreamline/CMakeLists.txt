cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeStreamline)
message(STATUS "生成Streamline模块")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (NOT ON_WINDOWS)
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持Streamline")
endif()

add_definitions(-DON_WIN64)
add_definitions(-D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)

if(MSVC)
    add_compile_options(/utf-8 /ZI)
endif()

find_package(Vulkan REQUIRED)

link_directories(
    ${PROJECT_SOURCE_DIR}/libraries/${LIB_PLATFORM}
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/libraries/${LIB_PLATFORM}
    ${CMAKE_SOURCE_DIR}/third_party/Streamline/lib/x64
)

include_directories(
    ${PROJECT_SOURCE_DIR}/include
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/include
    ${CMAKE_SOURCE_DIR}/third_party
    ${CMAKE_SOURCE_DIR}/third_party/jni
    ${CMAKE_SOURCE_DIR}/third_party/Streamline/include
    ${PROJECT_SOURCE_DIR}/src
)

add_library(SR_STREAMLINE_LIB SHARED
    ${PROJECT_SOURCE_DIR}/src/streamline.cpp
)
set_target_properties(SR_STREAMLINE_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionStreamline+${LIB_PLATFORM}+${SR_BUILD_TYPE}")

target_link_libraries(SR_STREAMLINE_LIB
    SR_MAIN_LIB
    sl.interposer
    ${Vulkan_LIBRARIES}
)
