cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeNGX)
message(STATUS "生成NGX模块")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT (ON_WINDOWS OR ON_LINUX))
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持NGX")
endif()

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    add_definitions(-DON_WIN64 -D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    add_definitions(-DON_LINUX64 -DFFX_GCC)
endif()

if(MSVC)
    add_compile_options(/utf-8 /ZI)
endif()

find_package(Vulkan REQUIRED)

set(DLSS_SDK_DIR "${PROJECT_SOURCE_DIR}/third_party/DLSS")
if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    link_directories("${DLSS_SDK_DIR}/lib/Windows_x86_64/x64")
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    link_directories("${DLSS_SDK_DIR}/lib/Linux_x86_64")
endif()

include_directories(
    ${CMAKE_SOURCE_DIR}/SRNativeMain/include
    ${CMAKE_SOURCE_DIR}/third_party/jni
    "${DLSS_SDK_DIR}/include"
    ${PROJECT_SOURCE_DIR}/src
)

add_library(SR_NGX_LIB SHARED
    ${PROJECT_SOURCE_DIR}/src/ngx_jni.cpp
)

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set_target_properties(SR_NGX_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionNGX+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
    if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        target_link_libraries(SR_NGX_LIB nvsdk_ngx_s_dbg ${Vulkan_LIBRARIES})
    else()
        target_link_libraries(SR_NGX_LIB nvsdk_ngx_s ${Vulkan_LIBRARIES})
    endif()
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    set_target_properties(SR_NGX_LIB PROPERTIES OUTPUT_NAME "SuperResolutionNGX+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
    target_link_libraries(SR_NGX_LIB nvsdk_ngx ${Vulkan_LIBRARIES} dl pthread -static-libgcc)
endif()
