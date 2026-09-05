cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeDLSSNR)
message(STATUS "生成DLSSNR模块")

if (NOT (ON_WINDOWS))
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持DLSSNR")
endif ()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (MSVC)
    add_compile_options(/utf-8 /ZI)
endif ()

find_package(Vulkan REQUIRED)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

include_directories(
        ${PROJECT_SOURCE_DIR}/include
        ${PROJECT_SOURCE_DIR}/../SRNativeMain/include
        ${PROJECT_SOURCE_DIR}/../SRNativeNGX/third_party/DLSS/include
        ${PROJECT_SOURCE_DIR}/src
)

aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
add_library(SR_DLSSNR_LIB SHARED ${ALL_SRC})
add_library(SR_DLSSNR_NGX_PROXY SHARED ${PROJECT_SOURCE_DIR}/proxy/nvngx_proxy.cpp)

target_compile_definitions(SR_DLSSNR_LIB PRIVATE
        ON_WIN64
        _DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR
        SRLIB_VERSION="${SRLIB_VERSION}"
)

set_target_properties(SR_DLSSNR_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionDLSSNR+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
set_target_properties(SR_DLSSNR_NGX_PROXY PROPERTIES OUTPUT_NAME "nvngx")

target_link_libraries(SR_DLSSNR_LIB
        SR_MAIN_LIB
        ${Vulkan_LIBRARIES}
)

target_link_libraries(SR_DLSSNR_NGX_PROXY
        ${Vulkan_LIBRARIES}
)
