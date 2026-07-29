cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeD3D12Interop)
message(STATUS "生成D3D12互操作模块")

if (NOT ON_WINDOWS)
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持D3D12互操作")
endif ()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (MSVC)
    add_compile_options(/utf-8 /ZI)
endif ()
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
add_library(SR_D3D12_INTEROP_LIB SHARED ${ALL_SRC})

target_compile_definitions(SR_D3D12_INTEROP_LIB PRIVATE
        ON_WIN64
        _DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR
)

set_target_properties(
        SR_D3D12_INTEROP_LIB
        PROPERTIES OUTPUT_NAME "libSuperResolutionD3D12Interop+${LIB_PLATFORM}+${SR_BUILD_TYPE}"
)

target_link_libraries(SR_D3D12_INTEROP_LIB
        d3d12
        dxgi
)
