cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeStreamlineDLSSProvider)
message(STATUS "生成Streamline DLSS provider模块")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (NOT ON_WINDOWS)
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持Streamline DLSS provider")
endif()

add_definitions(-DON_WIN64)
add_definitions(-D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)

if(MSVC)
    add_compile_options(/utf-8 /ZI)
endif()

link_directories(
    ${CMAKE_SOURCE_DIR}/third_party/Streamline/lib/x64
)

include_directories(
    ${PROJECT_SOURCE_DIR}/../SRNativeStreamline/include
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/include
)

add_library(SR_STREAMLINE_DLSS_PROVIDER_LIB SHARED
    ${PROJECT_SOURCE_DIR}/src/sr_provider.cpp
)
set_target_properties(SR_STREAMLINE_DLSS_PROVIDER_LIB PROPERTIES
    OUTPUT_NAME "libSuperResolutionStreamlineDLSS+${LIB_PLATFORM}+${SR_BUILD_TYPE}"
)

target_link_libraries(SR_STREAMLINE_DLSS_PROVIDER_LIB
    SR_MAIN_LIB
    SR_STREAMLINE_LIB
)
