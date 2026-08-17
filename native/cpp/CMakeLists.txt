cmake_minimum_required(VERSION 3.15)
project(SuperResolution)

option(SR_FSR "启用以支持FSR" ON)
option(SR_FSR4 "启用以支持FSR4（FFX API，仅Windows D3D12）" ON)
option(SR_D3D12_INTEROP "启用OpenGL与D3D12互操作（仅Windows）" ON)
option(SR_XESS "启用以支持XESS" ON)
option(SR_NGX "启用以支持NGX绑定" ON)
option(SR_STREAMLINE "启用以支持Streamline" ON)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Debug CACHE STRING "构建类型" FORCE)
endif()

if(NOT (CMAKE_BUILD_TYPE STREQUAL "Debug" OR CMAKE_BUILD_TYPE STREQUAL "Release"))
    message(FATAL_ERROR "只支持 Debug 或 Release 构建类型)")
endif()

message(STATUS "构建类型: ${CMAKE_BUILD_TYPE}")
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(SR_BUILD_TYPE "debug")
else()
    set(SR_BUILD_TYPE "release")
endif()



if(NOT DEFINED SRLIB_VERSION)
    string(TIMESTAMP COMPILE_TIME "%Y%m%d_%H%M%S")
    set(SRLIB_VERSION ${COMPILE_TIME})
endif()
# 注意：不使用 add_definitions，避免 SRLIB_VERSION 泄漏到 SDK 等第三方子目录
# 各模块通过 target_compile_definitions 自行添加

if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    set(LIB_PLATFORM "linux64")
    set(ON_LINUX ON)
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    set(LIB_PLATFORM "win64")
    set(ON_WINDOWS ON)
elseif(CMAKE_SYSTEM_NAME MATCHES "Android")
    set(LIB_PLATFORM "android")
    set(ON_ANDROID ON)
elseif(CMAKE_SYSTEM_NAME MATCHES "Darwin")
    set(LIB_PLATFORM "macosarm64")
    set(ON_DARWIN ON)
else()
    message(FATAL_ERROR "不支持的平台: ${CMAKE_SYSTEM_NAME}")
endif()


foreach(OUTPUTCONFIG DEBUG RELEASE RELWITHDEBINFO MINSIZEREL)
    string(TOUPPER ${OUTPUTCONFIG} OUTPUTCONFIG_UPPER)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_${OUTPUTCONFIG_UPPER} "${PROJECT_SOURCE_DIR}/output/bin")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_${OUTPUTCONFIG_UPPER} "${PROJECT_SOURCE_DIR}/output/lib")
    set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY_${OUTPUTCONFIG_UPPER} "${PROJECT_SOURCE_DIR}/output/lib")
endforeach()

if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    add_compile_options(-Wall)
    if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        add_compile_options(-O3 -g1)
    elseif(CMAKE_BUILD_TYPE STREQUAL "Release")
        add_compile_options(-O3 -g0 -ffunction-sections -fdata-sections -flto)
        #别问为什么要写个-fuse-ld=lld
        add_link_options(-fuse-ld=lld -flto -Wl,--gc-sections -s)
    endif()
elseif(NOT WIN32)
    add_compile_options(-Wall -O3)
endif()

include_directories(
    ${PROJECT_SOURCE_DIR}/third_party
    ${PROJECT_SOURCE_DIR}/third_party/jni
)

add_subdirectory("SRNativeMain")
if(SR_D3D12_INTEROP AND ON_WINDOWS)
    add_subdirectory("SRNativeD3D12Interop")
endif()
if(SR_FSR)
    add_subdirectory("SRNativeFSR")
endif()
if(SR_XESS)
    add_subdirectory("SRNativeXeSS")
endif()

if(SR_NGX)
    add_subdirectory("SRNativeNGX")
endif()

if(SR_FSR4 AND ON_WINDOWS)
    add_subdirectory("SRNativeFSR4")
endif()

if(SR_STREAMLINE AND ON_WINDOWS)
    add_subdirectory("SRNativeStreamline")
endif()
