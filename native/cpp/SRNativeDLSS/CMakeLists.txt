cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeMain)
message(STATUS "生成DLSS模块")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (NOT (ON_WINDOWS) )
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持DLSS")
endif()
if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    add_definitions(-DON_WIN64)
    add_definitions(-D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)
else()
    message(FATAL_ERROR "不支持的平台: ${CMAKE_SYSTEM_NAME}")
endif()
find_package(Vulkan REQUIRED)
link_directories(
    ${PROJECT_SOURCE_DIR}/libraries/${LIB_PLATFORM}
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/libraries/${LIB_PLATFORM}
    ${PROJECT_SOURCE_DIR}/third_party/DLSS/lib/Windows_x86_64/x64
)
include_directories(
    ${PROJECT_SOURCE_DIR}/include
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/include
    ${PROJECT_SOURCE_DIR}/third_party
    ${PROJECT_SOURCE_DIR}/src
    ${PROJECT_SOURCE_DIR}/third_party/DLSS/include
)
aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
add_library(SR_DLSS_LIB SHARED ${ALL_SRC})
set_target_properties(SR_DLSS_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionDLSS+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_link_libraries(SR_DLSS_LIB
        SR_MAIN_LIB
        nvsdk_ngx_s_dbg
        ${Vulkan_LIBRARIES}
    )
else()
    target_link_libraries(SR_DLSS_LIB
        SR_MAIN_LIB
        nvsdk_ngx_s
        ${Vulkan_LIBRARIES}
    )
endif()

if(MSVC)
    add_compile_options(/utf-8 /ZI)
endif()