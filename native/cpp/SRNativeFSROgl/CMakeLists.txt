cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeMain)
message(STATUS "生成FSR OpenGL模块")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (NOT (ON_LINUX OR ON_WINDOWS) )
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持FSR")
endif()
if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    add_definitions(-DON_LINUX64 -DFFX_GCC)
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    add_definitions(-DON_WIN64)
    add_definitions(-D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)
elseif(CMAKE_SYSTEM_NAME MATCHES "Android")
    add_definitions(-DON_ANDROID)
elseif(CMAKE_SYSTEM_NAME MATCHES "Darwin")
    add_definitions(-DON_MACOS)
else()
    message(FATAL_ERROR "不支持的平台: ${CMAKE_SYSTEM_NAME}")
endif()
find_package(OpenGL REQUIRED)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded")
link_directories(
    ${PROJECT_SOURCE_DIR}/libraries/${LIB_PLATFORM}
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/libraries/${LIB_PLATFORM}
)
include_directories(
    ${PROJECT_SOURCE_DIR}/include
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/include
    ${PROJECT_SOURCE_DIR}/third_party
    ${PROJECT_SOURCE_DIR}/src
)
aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
add_library(SR_FSRGL_LIB SHARED ${ALL_SRC})
if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    set_target_properties(SR_FSRGL_LIB PROPERTIES OUTPUT_NAME "SuperResolutionFSRGL+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set_target_properties(SR_FSRGL_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionFSRGL+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
endif()
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_link_libraries(SR_FSRGL_LIB
        SR_MAIN_LIB
        ffx_fsr2_api_gl_x64d
        ffx_fsr2_api_x64d
        ${Vulkan_LIBRARIES}
    )
else()
    target_link_libraries(SR_FSRGL_LIB
        SR_MAIN_LIB
        ffx_fsr2_api_gl_x64
        ffx_fsr2_api_x64
        ${Vulkan_LIBRARIES}
    )
endif()
