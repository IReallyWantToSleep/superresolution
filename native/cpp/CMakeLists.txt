cmake_minimum_required(VERSION 3.15)
project(SuperResolution)

option(SR_FSR "启用以支持FSR" ON)
option(SR_FSROGL "启用以支持FSR OpenGL" ON)
option(SR_XESS "启用以支持XESS" OFF)
option(SR_DLSS "启用以支持DLSS" OFF)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release CACHE STRING "构建类型" FORCE)
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
add_definitions(-DSRLIB_VERSION="${SRLIB_VERSION}")

if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    set(LIB_PLATFORM "linux64")
    set(ON_LINUX ON)
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded")
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


if(NOT WIN32)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -g -O3")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -g -O3")
endif()

include_directories(
    ${PROJECT_SOURCE_DIR}/third_party
)

add_subdirectory("SRNativeMain")

if(SR_FSR)
    add_subdirectory("SRNativeFSR")
endif()

if(SR_FSROGL)
    add_subdirectory("SRNativeFSROgl")
endif()

if(SR_XESS)
    add_subdirectory("SRNativeXeSS")
endif()

if(SR_DLSS)
    add_subdirectory("SRNativeDLSS")
endif()