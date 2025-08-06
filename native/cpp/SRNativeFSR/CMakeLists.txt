cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeMain)

if (NOT WIN32)
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持FSR")
endif()
find_package(Vulkan REQUIRED)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded")
link_directories(
    ${PROJECT_SOURCE_DIR}/libraries/${LIB_PLATFORM}
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/libraries/${LIB_PLATFORM}
)
include_directories(
    ${PROJECT_SOURCE_DIR}/include
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/include
    ${PROJECT_SOURCE_DIR}/src
)
aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
add_library(SR_FSR_LIB SHARED ${ALL_SRC})
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_link_libraries(SR_FSR_LIB
        SR_MAIN_LIB
        ffx_backend_vk_x64d
        ffx_fsr1_x64d
        ffx_fsr2_x64d
        ffx_fsr3upscaler_x64d
        ${Vulkan_LIBRARIES}
    )
else()
    target_link_libraries(SR_FSR_LIB
        SR_MAIN_LIB
        ffx_backend_vk_x64
        ffx_fsr1_x64
        ffx_fsr2_x64
        ffx_fsr3upscaler_x64
        ${Vulkan_LIBRARIES}
    )
endif()

set_target_properties(SR_FSR_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionFSR+${LIB_PLATFORM}+${SR_BUILD_TYPE}")