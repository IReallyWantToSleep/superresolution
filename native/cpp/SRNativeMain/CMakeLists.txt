cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeMain)

set(THIRD_PARTY_DIR "${PROJECT_SOURCE_DIR}/third_party")
set(FT_DIR "${THIRD_PARTY_DIR}/freetype")
set(GLSLANG_DIR "${THIRD_PARTY_DIR}/glslang")
#set(TGFX_DIR "${THIRD_PARTY_DIR}/tgfx")

include_directories(
    ${PROJECT_SOURCE_DIR}/include
    ${PROJECT_SOURCE_DIR}/src
    ${GLSLANG_DIR}
    #${TGFX_DIR}/include
)

if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    add_definitions(-DON_LINUX64 -DFFX_GCC)
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    add_definitions(-DON_WIN64)
    add_definitions(-D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
elseif(CMAKE_SYSTEM_NAME MATCHES "Android")
    add_definitions(-DON_ANDROID)
elseif(CMAKE_SYSTEM_NAME MATCHES "Darwin")
    add_definitions(-DON_MACOS)
else()
    message(FATAL_ERROR "不支持的平台: ${CMAKE_SYSTEM_NAME}")
endif()
if(MSVC)
    add_compile_options(/utf-8 /ZI)
endif()
# Ensure static deps are built with PIC when linking into shared libraries on Linux.
if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    set(CMAKE_POSITION_INDEPENDENT_CODE ON)
endif()
#add freetype
add_subdirectory("${FT_DIR}")
#add glslang
set(GLSLANG_ENABLE_INSTALL_DEFAULT OFF)
set(BUILD_SHARED_LIBS OFF)
set(LIB_TYPE STATIC)
set(ENABLE_GLSLANG_BINARIES OFF)
set(GLSLANG_TESTS_DEFAULT OFF)
set(ENABLE_HLSL OFF)
set(BUILD_EXTERNAL ON)
set(ENABLE_SPIRV_TOOLS_INSTALL OFF)
set(SPIRV_TOOLS_BUILD_STATIC ON)
set(SPIRV_SKIP_EXECUTABLES ON)
add_subdirectory(${GLSLANG_DIR})
#add tgfx
#set(TGFX_BUILD_TESTS OFF)
#set(TGFX_USE_PNG_DECODE ON)
#set(TGFX_USE_JPEG_DECODE OFF)
#set(TGFX_USE_WEBP_DECODE OFF)
#set(TGFX_USE_WEBP_ENCODE OFF)
#set(TGFX_USE_JPEG_ENCODE OFF)
#set(TGFX_USE_PNG_ENCODE OFF)
#set(TGFX_USE_THREADS ON)
#set(TGFX_USE_FREETYPE ON)
#set(TGFX_USE_INSPECTOR OFF)
#add_subdirectory(${TGFX_DIR})

#add source files
aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
aux_source_directory(${PROJECT_SOURCE_DIR}/src/nvg NVG_SRC)
add_library(SR_MAIN_LIB SHARED ${ALL_SRC} ${NVG_SRC})
if(WIN32)
    set_target_properties(SR_MAIN_LIB PROPERTIES OUTPUT_NAME "libSuperResolution+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
else()
    set_target_properties(SR_MAIN_LIB PROPERTIES OUTPUT_NAME "SuperResolution+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
endif()
target_link_libraries(SR_MAIN_LIB
    glslang
    glslang-default-resource-limits
    SPIRV
    freetype
)
if(NOT WIN32)
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    target_link_libraries(SR_MAIN_LIB -lstdc++fs -pthread -ldl)
endif()

