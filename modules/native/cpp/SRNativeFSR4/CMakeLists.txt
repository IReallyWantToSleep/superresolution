cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeFSR4)
message(STATUS "生成FSR4模块")

if (NOT ON_WINDOWS)
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持FSR4（FFX API 仅支持 Windows D3D12）")
endif ()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (MSVC)
    add_compile_options(/utf-8 /ZI)
endif ()
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

include_directories(
        ${PROJECT_SOURCE_DIR}/include
        ${PROJECT_SOURCE_DIR}/../SRNativeMain/include
        ${PROJECT_SOURCE_DIR}/../third_party/FidelityFX/2.3.0/api/include
        ${PROJECT_SOURCE_DIR}/../third_party/FidelityFX/2.3.0/upscalers/include
        ${PROJECT_SOURCE_DIR}/src
)

aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
add_library(SR_FSR4_LIB SHARED ${ALL_SRC})

target_compile_definitions(SR_FSR4_LIB PRIVATE
        ON_WIN64
        _DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR
        SRLIB_VERSION="${SRLIB_VERSION}"
)

set_target_properties(SR_FSR4_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionFSR4+${LIB_PLATFORM}+${SR_BUILD_TYPE}")

target_link_libraries(SR_FSR4_LIB
        SR_MAIN_LIB
)
