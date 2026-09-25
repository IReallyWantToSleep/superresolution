cmake_minimum_required(VERSION 3.20)
project(SuperResolutionNativeD3D12 LANGUAGES CXX)

if(NOT WIN32)
    message(FATAL_ERROR "SRNativeD3D12 is available only on Windows")
endif()

option(SR_D3D12_BUILD_SMOKE "Build the standalone D3D12 runtime smoke test" OFF)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

add_library(SR_D3D12_CORE STATIC
    src/d3d12_runtime.cpp
)

target_include_directories(SR_D3D12_CORE
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/../SRNativeMain/include
)

target_compile_definitions(SR_D3D12_CORE PRIVATE
    WIN32_LEAN_AND_MEAN
    NOMINMAX
    UNICODE
    _UNICODE
)

target_compile_options(SR_D3D12_CORE PRIVATE
    $<$<CXX_COMPILER_ID:MSVC>:/W4>
    $<$<CXX_COMPILER_ID:MSVC>:/permissive->
    $<$<CXX_COMPILER_ID:MSVC>:/utf-8>
)

target_link_libraries(SR_D3D12_CORE PUBLIC d3d12 dxgi dxguid)

add_library(SR_D3D12_LIB SHARED
    src/d3d12_jni.cpp
)

target_include_directories(SR_D3D12_LIB PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../third_party/jni
)

target_compile_definitions(SR_D3D12_LIB PRIVATE
    ON_WIN64
    WIN32_LEAN_AND_MEAN
    NOMINMAX
    UNICODE
    _UNICODE
)

target_compile_options(SR_D3D12_LIB PRIVATE
    $<$<CXX_COMPILER_ID:MSVC>:/W4>
    $<$<CXX_COMPILER_ID:MSVC>:/permissive->
    $<$<CXX_COMPILER_ID:MSVC>:/utf-8>
)

target_link_libraries(SR_D3D12_LIB PRIVATE SR_D3D12_CORE)

if(DEFINED LIB_PLATFORM AND DEFINED SR_BUILD_TYPE)
    set_target_properties(SR_D3D12_LIB PROPERTIES
        OUTPUT_NAME "libSuperResolutionD3D12+${LIB_PLATFORM}+${SR_BUILD_TYPE}"
    )
else()
    set_target_properties(SR_D3D12_LIB PROPERTIES OUTPUT_NAME "SuperResolutionD3D12")
endif()