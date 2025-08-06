cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeMain)

link_directories(${PROJECT_SOURCE_DIR}/libraries/${LIB_PLATFORM})
include_directories(
    ${PROJECT_SOURCE_DIR}/include
    ${PROJECT_SOURCE_DIR}/src
)

if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    add_definitions(-DON_LINUX64 -DFFX_GCC)
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    add_definitions(-DON_WIN64)
    add_definitions(-D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)
elseif(CMAKE_SYSTEM_NAME MATCHES "Android")
    add_definitions(-DON_ANDROID)
elseif(CMAKE_SYSTEM_NAME MATCHES "Darwin")
    add_definitions(-DON_MACOS)
else()
    message(FATAL_ERROR "不支持的平台: ${CMAKE_SYSTEM_NAME}")
endif()


aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
add_library(SR_MAIN_LIB SHARED ${ALL_SRC})
if(WIN32)
    if (CMAKE_BUILD_TYPE STREQUAL "Debug")
        target_link_libraries(SR_MAIN_LIB
            GenericCodeGend
            glslangd
            glslang-default-resource-limitsd
            MachineIndependentd
            OSDependentd
            SPIRVd
            SPIRV-Toolsd
            SPIRV-Tools-diffd
            SPIRV-Tools-linkd
            SPIRV-Tools-lintd
            SPIRV-Tools-optd
            SPIRV-Tools-reduced
            SPIRV-Tools-sharedd
        )
    else()
        target_link_libraries(SR_MAIN_LIB
            GenericCodeGen
            glslang
            glslang-default-resource-limits
            MachineIndependent
            OSDependent
            SPIRV
            SPIRV-Tools
            SPIRV-Tools-diff
            SPIRV-Tools-link
            SPIRV-Tools-lint
            SPIRV-Tools-opt
            SPIRV-Tools-reduce
            SPIRV-Tools-shared
        )
    endif()

    set_target_properties(SR_MAIN_LIB PROPERTIES OUTPUT_NAME "libSuperResolution+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
else()
    set_target_properties(SR_MAIN_LIB PROPERTIES OUTPUT_NAME "SuperResolution+${LIB_PLATFORM}+${SR_BUILD_TYPE}")
    target_link_libraries(SR_MAIN_LIB
        GenericCodeGen
        glslang-default-resource-limits
        glslang
        MachineIndependent
        OSDependent
        SPIRV-Tools-diff
        SPIRV-Tools-lint
        SPIRV-Tools-opt
        SPIRV-Tools-reduce
        SPIRV-Tools
        SPVRemapper
    )
endif()

if(NOT WIN32)
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    target_link_libraries(SR_MAIN_LIB -lstdc++fs -pthread -ldl)
endif()