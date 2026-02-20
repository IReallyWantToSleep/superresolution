cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeMain)
message(STATUS "生成FSR模块")

if (NOT (ON_LINUX OR ON_WINDOWS))
    message(FATAL_ERROR "${LIB_PLATFORM} 平台不支持FSR")
endif()

find_package(Vulkan REQUIRED)

# ============================================================
# FidelityFX SDK: 从子模块源码构建 / 使用预编译库
#
# 重要：add_subdirectory(SDK) 必须在本模块的 add_definitions /
#       compile options 之前调用，避免父级定义泄漏到 SDK 中。
# ============================================================
option(SR_FSR_BUILD_SDK "从源码编译 FidelityFX SDK（需要 SDK 子模块已初始化）" ON)

# SDK 编译产物输出目录（同时也是预编译库查找目录）
set(FFX_SDK_LIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/libraries/${LIB_PLATFORM}")

if(SR_FSR_BUILD_SDK)
    # --- 检查子模块是否已初始化 ---
    if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/SDK/CMakeLists.txt")
        message(FATAL_ERROR
            "FSR SDK 子模块未初始化！请先执行:\n"
            "  git submodule update --init --recursive\n"
            "或使用 -DSR_FSR_BUILD_SDK=OFF 并手动放置预编译库到\n"
            "  ${FFX_SDK_LIB_DIR}\n")
    endif()

    message(STATUS "[FSR] 从源码构建 FidelityFX SDK ...")

    # SDK 未显式设置 CMAKE_CXX_STANDARD。父级的 C++20 设置会导致 SDK 内部
    # 使用 std::popcount 时缺少 <bit> 而编译失败 (GCC)。
    # 临时清除继承的 C++ 标准，让 SDK 使用编译器默认值。
    set(_SR_PARENT_CXX_STANDARD          "${CMAKE_CXX_STANDARD}")
    set(_SR_PARENT_CXX_STANDARD_REQUIRED "${CMAKE_CXX_STANDARD_REQUIRED}")
    unset(CMAKE_CXX_STANDARD)
    unset(CMAKE_CXX_STANDARD_REQUIRED)

    add_subdirectory(SDK)

    # 恢复 C++ 标准
    set(CMAKE_CXX_STANDARD          "${_SR_PARENT_CXX_STANDARD}")
    set(CMAKE_CXX_STANDARD_REQUIRED "${_SR_PARENT_CXX_STANDARD_REQUIRED}")

    # SDK 产生的 CMake target（FFX_PLATFORM_NAME = x64）
    set(FFX_SDK_TARGETS
        ffx_backend_vk_x64
        ffx_fsr1_x64
        ffx_fsr2_x64
        ffx_fsr3upscaler_x64
    )

    # clang-cl 交叉编译时，SDK 自身的 /W4 /WX 会把 clang 独有的诊断
    # （-Wmissing-field-initializers、-Wmissing-braces）提升为错误。
    # 在不修改 SDK 源码的前提下，按 target 追加抑制选项。
    if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
        foreach(_tgt IN LISTS FFX_SDK_TARGETS)
            if(TARGET ${_tgt})
                target_compile_options(${_tgt} PRIVATE
                    -Wno-missing-field-initializers
                    -Wno-missing-braces
                )
            endif()
        endforeach()
    endif()

    # 将 SDK 的静态库输出重定向到 libraries/${LIB_PLATFORM}/
    # 这样无论是后续链接还是外部工具都能在同一位置找到产物
    foreach(_tgt IN LISTS FFX_SDK_TARGETS)
        if(TARGET ${_tgt})
            set_target_properties(${_tgt} PROPERTIES
                ARCHIVE_OUTPUT_DIRECTORY              "${FFX_SDK_LIB_DIR}"
                ARCHIVE_OUTPUT_DIRECTORY_DEBUG         "${FFX_SDK_LIB_DIR}"
                ARCHIVE_OUTPUT_DIRECTORY_RELEASE       "${FFX_SDK_LIB_DIR}"
                ARCHIVE_OUTPUT_DIRECTORY_RELWITHDEBINFO "${FFX_SDK_LIB_DIR}"
            )
        endif()
    endforeach()

    # 记录使用 target 链接的 SDK 库名
    set(FFX_LINK_BACKEND  ffx_backend_vk_x64)
    set(FFX_LINK_FSR2     ffx_fsr2_x64)
    set(FFX_LINK_FSR3UP   ffx_fsr3upscaler_x64)
else()
    # --- 使用预编译 SDK 库 ---
    message(STATUS "[FSR] 使用预编译 SDK 库: ${FFX_SDK_LIB_DIR}")
    if(NOT EXISTS "${FFX_SDK_LIB_DIR}")
        message(FATAL_ERROR
            "预编译 SDK 库目录不存在: ${FFX_SDK_LIB_DIR}\n"
            "请先构建 SDK 或使用 -DSR_FSR_BUILD_SDK=ON 从源码构建")
    endif()
    link_directories("${FFX_SDK_LIB_DIR}")

    # 预编译库文件名带 Debug 后缀 d
    if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        set(FFX_LINK_BACKEND  ffx_backend_vk_x64d)
        set(FFX_LINK_FSR2     ffx_fsr2_x64d)
        set(FFX_LINK_FSR3UP   ffx_fsr3upscaler_x64d)
    else()
        set(FFX_LINK_BACKEND  ffx_backend_vk_x64)
        set(FFX_LINK_FSR2     ffx_fsr2_x64)
        set(FFX_LINK_FSR3UP   ffx_fsr3upscaler_x64)
    endif()
endif()

# ============================================================
# 构建 SRNativeFSR 模块
# ============================================================
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 平台定义——使用 target_compile_definitions 而不是 add_definitions，
# 只对 SRNativeFSR 生效，不会泄漏到 SDK 子目录
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
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party
    ${CMAKE_CURRENT_SOURCE_DIR}/src
)

aux_source_directory(${CMAKE_CURRENT_SOURCE_DIR}/src ALL_SRC)
add_library(SR_FSR_LIB SHARED ${ALL_SRC})

target_compile_definitions(SR_FSR_LIB PRIVATE
    ${_SR_FSR_PLATFORM_DEFS}
    SRLIB_VERSION="${SRLIB_VERSION}"
)

if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    set_target_properties(SR_FSR_LIB PROPERTIES OUTPUT_NAME "SuperResolutionFSR+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set_target_properties(SR_FSR_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionFSR+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
endif()

target_link_libraries(SR_FSR_LIB
    SR_MAIN_LIB
    ${FFX_LINK_BACKEND}
    ${FFX_LINK_FSR2}
    ${FFX_LINK_FSR3UP}
    ${Vulkan_LIBRARIES}
)