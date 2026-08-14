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

# Link the glslang archives downloaded by init.py instead of rebuilding glslang.
set(GLSLANG_PREBUILT_ROOT "${CMAKE_CURRENT_LIST_DIR}/../prebuilt/glslang")
set(GLSLANG_COMMIT_HASH_FILE "${GLSLANG_PREBUILT_ROOT}/commit-hash.txt")
if(NOT EXISTS "${GLSLANG_COMMIT_HASH_FILE}")
    message(FATAL_ERROR
        "未找到 glslang 提交哈希文件: ${GLSLANG_COMMIT_HASH_FILE}\n"
        "请先在 native/cpp 目录运行 python init.py。"
    )
endif()

file(STRINGS "${GLSLANG_COMMIT_HASH_FILE}" GLSLANG_PREBUILT_COMMIT_HASH LIMIT_COUNT 1)
string(STRIP "${GLSLANG_PREBUILT_COMMIT_HASH}" GLSLANG_PREBUILT_COMMIT_HASH)
if(GLSLANG_PREBUILT_COMMIT_HASH STREQUAL "")
    message(FATAL_ERROR
        "glslang 提交哈希文件为空: ${GLSLANG_COMMIT_HASH_FILE}"
    )
endif()

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(GLSLANG_PREBUILT_PLATFORM "windows-x86_64")
    set(GLSLANG_LIBRARY_PREFIX "")
    set(GLSLANG_LIBRARY_EXTENSION ".lib")
    if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        set(GLSLANG_LIBRARY_DEBUG_SUFFIX "d")
    else()
        set(GLSLANG_LIBRARY_DEBUG_SUFFIX "")
    endif()
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    set(GLSLANG_PREBUILT_PLATFORM "linux-x86_64")
    set(GLSLANG_LIBRARY_PREFIX "lib")
    set(GLSLANG_LIBRARY_EXTENSION ".a")
    set(GLSLANG_LIBRARY_DEBUG_SUFFIX "")
else()
    message(FATAL_ERROR
        "glslang 预编译库不支持平台: ${CMAKE_SYSTEM_NAME}"
    )
endif()

string(TOLOWER "${CMAKE_BUILD_TYPE}" GLSLANG_PREBUILT_CONFIG)
set(GLSLANG_PREBUILT_DIR
    "${GLSLANG_PREBUILT_ROOT}/${GLSLANG_PREBUILT_COMMIT_HASH}-${GLSLANG_PREBUILT_PLATFORM}-${GLSLANG_PREBUILT_CONFIG}"
)
set(GLSLANG_LIBRARY_DIR "${GLSLANG_PREBUILT_DIR}/lib")

set(GLSLANG_LIB_GENERIC_CODE_GEN
    "${GLSLANG_LIBRARY_DIR}/${GLSLANG_LIBRARY_PREFIX}GenericCodeGen${GLSLANG_LIBRARY_DEBUG_SUFFIX}${GLSLANG_LIBRARY_EXTENSION}"
)
set(GLSLANG_LIB_DEFAULT_RESOURCE_LIMITS
    "${GLSLANG_LIBRARY_DIR}/${GLSLANG_LIBRARY_PREFIX}glslang-default-resource-limits${GLSLANG_LIBRARY_DEBUG_SUFFIX}${GLSLANG_LIBRARY_EXTENSION}"
)
set(GLSLANG_LIB_GLSLANG
    "${GLSLANG_LIBRARY_DIR}/${GLSLANG_LIBRARY_PREFIX}glslang${GLSLANG_LIBRARY_DEBUG_SUFFIX}${GLSLANG_LIBRARY_EXTENSION}"
)
set(GLSLANG_LIB_MACHINE_INDEPENDENT
    "${GLSLANG_LIBRARY_DIR}/${GLSLANG_LIBRARY_PREFIX}MachineIndependent${GLSLANG_LIBRARY_DEBUG_SUFFIX}${GLSLANG_LIBRARY_EXTENSION}"
)
set(GLSLANG_LIB_OS_DEPENDENT
    "${GLSLANG_LIBRARY_DIR}/${GLSLANG_LIBRARY_PREFIX}OSDependent${GLSLANG_LIBRARY_DEBUG_SUFFIX}${GLSLANG_LIBRARY_EXTENSION}"
)
set(GLSLANG_LIB_SPIRV_TOOLS_OPT
    "${GLSLANG_LIBRARY_DIR}/${GLSLANG_LIBRARY_PREFIX}SPIRV-Tools-opt${GLSLANG_LIBRARY_DEBUG_SUFFIX}${GLSLANG_LIBRARY_EXTENSION}"
)
set(GLSLANG_LIB_SPIRV_TOOLS
    "${GLSLANG_LIBRARY_DIR}/${GLSLANG_LIBRARY_PREFIX}SPIRV-Tools${GLSLANG_LIBRARY_DEBUG_SUFFIX}${GLSLANG_LIBRARY_EXTENSION}"
)
set(GLSLANG_LIB_SPIRV
    "${GLSLANG_LIBRARY_DIR}/${GLSLANG_LIBRARY_PREFIX}SPIRV${GLSLANG_LIBRARY_DEBUG_SUFFIX}${GLSLANG_LIBRARY_EXTENSION}"
)

set(GLSLANG_REQUIRED_FILES
    "${GLSLANG_LIB_GENERIC_CODE_GEN}"
    "${GLSLANG_LIB_DEFAULT_RESOURCE_LIMITS}"
    "${GLSLANG_LIB_GLSLANG}"
    "${GLSLANG_LIB_MACHINE_INDEPENDENT}"
    "${GLSLANG_LIB_OS_DEPENDENT}"
    "${GLSLANG_LIB_SPIRV_TOOLS_OPT}"
    "${GLSLANG_LIB_SPIRV_TOOLS}"
    "${GLSLANG_LIB_SPIRV}"
)
set(GLSLANG_MISSING_FILES)
foreach(GLSLANG_REQUIRED_FILE IN LISTS GLSLANG_REQUIRED_FILES)
    if(NOT EXISTS "${GLSLANG_REQUIRED_FILE}")
        list(APPEND GLSLANG_MISSING_FILES "${GLSLANG_REQUIRED_FILE}")
    endif()
endforeach()
if(GLSLANG_MISSING_FILES)
    list(JOIN GLSLANG_MISSING_FILES "\n  " GLSLANG_MISSING_FILES_TEXT)
    message(FATAL_ERROR
        "glslang ${GLSLANG_PREBUILT_COMMIT_HASH} 的预编译文件不完整:\n"
        "  ${GLSLANG_MISSING_FILES_TEXT}\n"
        "请重新运行 native/cpp/init.py。"
    )
endif()

function(add_sr_glslang_library TARGET_NAME LIBRARY_PATH)
    add_library(${TARGET_NAME} STATIC IMPORTED)
    set_target_properties(${TARGET_NAME} PROPERTIES
        IMPORTED_LOCATION "${LIBRARY_PATH}"
    )
endfunction()

add_sr_glslang_library(sr_glslang_generic_code_gen "${GLSLANG_LIB_GENERIC_CODE_GEN}")
add_sr_glslang_library(sr_glslang_default_resource_limits "${GLSLANG_LIB_DEFAULT_RESOURCE_LIMITS}")
add_sr_glslang_library(sr_glslang "${GLSLANG_LIB_GLSLANG}")
add_sr_glslang_library(sr_glslang_machine_independent "${GLSLANG_LIB_MACHINE_INDEPENDENT}")
add_sr_glslang_library(sr_glslang_os_dependent "${GLSLANG_LIB_OS_DEPENDENT}")
add_sr_glslang_library(sr_glslang_spirv_tools_opt "${GLSLANG_LIB_SPIRV_TOOLS_OPT}")
add_sr_glslang_library(sr_glslang_spirv_tools "${GLSLANG_LIB_SPIRV_TOOLS}")
add_sr_glslang_library(sr_glslang_spirv "${GLSLANG_LIB_SPIRV}")

add_library(sr_glslang_prebuilt INTERFACE)
target_link_libraries(sr_glslang_prebuilt INTERFACE
    sr_glslang
    sr_glslang_default_resource_limits
    sr_glslang_spirv
    sr_glslang_machine_independent
    sr_glslang_generic_code_gen
    sr_glslang_os_dependent
    sr_glslang_spirv_tools_opt
    sr_glslang_spirv_tools
)
message(STATUS "glslang 预编译库: ${GLSLANG_PREBUILT_DIR}")

#add source files
aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
aux_source_directory(${PROJECT_SOURCE_DIR}/src/nvg NVG_SRC)
add_library(SR_MAIN_LIB SHARED ${ALL_SRC} ${NVG_SRC})
target_compile_definitions(SR_MAIN_LIB PRIVATE SRLIB_VERSION="${SRLIB_VERSION}")
if(WIN32)
    set_target_properties(SR_MAIN_LIB PROPERTIES OUTPUT_NAME "libSuperResolution+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
else()
    set_target_properties(SR_MAIN_LIB PROPERTIES OUTPUT_NAME "SuperResolution+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
endif()
target_link_libraries(SR_MAIN_LIB
    sr_glslang_prebuilt
    freetype
)
if(NOT WIN32)
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    target_link_libraries(SR_MAIN_LIB -lstdc++fs -pthread -ldl -static-libgcc)
endif()

