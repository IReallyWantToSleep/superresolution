cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeMain)
message(STATUS "生成NSS模块")

if (NOT (ON_LINUX OR ON_WINDOWS))
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持NSS")
endif()

find_package(Vulkan REQUIRED)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# NSS is consumed through the SDK's generic FFX API. The SDK binary is loaded
# at runtime; this path is only used for the matching public headers.
set(SR_NSS_SDK_ROOT
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../arm_nss_srmod/neural-graphics-sdk-for-game-engines"
    CACHE PATH "Arm Neural Graphics SDK root used by the NSS provider")
if (NOT EXISTS "${SR_NSS_SDK_ROOT}/ffx-api/include/ffx_api/ffx_api.h")
    message(FATAL_ERROR
        "Arm Neural Graphics SDK not found at ${SR_NSS_SDK_ROOT}. "
        "Set -DSR_NSS_SDK_ROOT=<path to neural-graphics-sdk-for-game-engines>.")
endif()
message(STATUS "NSS FFX API headers: ${SR_NSS_SDK_ROOT}")

set(_SR_FSR_PLATFORM_DEFS "")
if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    list(APPEND _SR_FSR_PLATFORM_DEFS ON_LINUX64 FFX_GCC)
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    list(APPEND _SR_FSR_PLATFORM_DEFS ON_WIN64 _DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
elseif(CMAKE_SYSTEM_NAME MATCHES "Android")
    list(APPEND _SR_FSR_PLATFORM_DEFS ON_ANDROID)
elseif(CMAKE_SYSTEM_NAME MATCHES "Darwin")
    list(APPEND _SR_FSR_PLATFORM_DEFS ON_MACOS)
else()
    message(FATAL_ERROR "不支持的平台: ${CMAKE_SYSTEM_NAME}")
endif()

if(MSVC)
    add_compile_options(/utf-8 /ZI)
endif()

link_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/../SRNativeMain/libraries/${LIB_PLATFORM}
)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../SRNativeMain/include
    ${SR_NSS_SDK_ROOT}/ffx-api/include
    ${SR_NSS_SDK_ROOT}/sdk/include
    ${CMAKE_CURRENT_SOURCE_DIR}/src
)

aux_source_directory(${CMAKE_CURRENT_SOURCE_DIR}/src ALL_SRC)
add_library(SR_NSS_LIB SHARED ${ALL_SRC})

target_compile_definitions(SR_NSS_LIB PRIVATE
    ${_SR_FSR_PLATFORM_DEFS}
    SRLIB_VERSION="${SRLIB_VERSION}"
)

if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    set_target_properties(SR_NSS_LIB PROPERTIES OUTPUT_NAME "SuperResolutionNSS+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set_target_properties(SR_NSS_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionNSS+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
endif()

target_link_libraries(SR_NSS_LIB
    SR_MAIN_LIB
    ${Vulkan_LIBRARIES}
    ${CMAKE_DL_LIBS}
)

if(NOT WIN32)
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    target_link_libraries(SR_NSS_LIB -static-libgcc)
endif()
