cmake_minimum_required(VERSION 3.15)
project(SuperResolution)
set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)

if(NOT CMAKE_CXX_COMPILER_ID STREQUAL "Clang" OR NOT CMAKE_C_COMPILER_ID STREQUAL "Clang")
    message(FATAL_ERROR "请使用Clang编译器")
endif()

if(NOT DEFINED SRLIB_VERSION)
    string(TIMESTAMP COMPILE_TIME "%Y%m%d_%H%M%S")
    set(SRLIB_VERSION ${COMPILE_TIME})
    message(STATUS "SRLIB_VERSION not defined, using default value: ${SRLIB_VERSION}")
endif()
add_definitions(-DSRLIB_VERSION="${SRLIB_VERSION}")

set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)

if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    add_definitions(-DON_LINUX64 -DFFX_GCC)
    set(LIB_PLATFORM "linux64")
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    add_definitions(-DON_WIN64)
    set(LIB_PLATFORM "win64")
elseif(CMAKE_SYSTEM_NAME MATCHES "Android")
    add_definitions(-DON_ANDROID)
    set(LIB_PLATFORM "android")
else()
    message(FATAL_ERROR "Unsupported platform: ${CMAKE_SYSTEM_NAME}")
endif()

link_directories(${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM})

if(NOT CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -g0 -fvisibility=hidden -O3")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -g0 -fvisibility=hidden -O3")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -O3")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -O3")
endif()
message(STATUS "Current platform: ${CMAKE_SYSTEM_NAME}")
message(STATUS "Current lib platform: ${LIB_PLATFORM}")
message(STATUS "CMAKE_CXX_FLAGS: ${CMAKE_CXX_FLAGS}")
message(STATUS "CMAKE_C_FLAGS: ${CMAKE_C_FLAGS}")

include_directories(
    ${PROJECT_BINARY_DIR}/../include
    ${PROJECT_BINARY_DIR}/../src
    ${PROJECT_BINARY_DIR}/../third_party
)

aux_source_directory(
    ${PROJECT_SOURCE_DIR}/src
    ALL_SRC
)

add_library(lib_shared SHARED ${ALL_SRC})
set_target_properties(lib_shared PROPERTIES 
    OUTPUT_NAME "SuperResolution+${LIB_PLATFORM}"
    WINDOWS_EXPORT_ALL_SYMBOLS TRUE 
)

if(CMAKE_SYSTEM_NAME MATCHES "Android")
    message(STATUS "Android platform detected")
    target_link_libraries(lib_shared
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libGenericCodeGen.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libglslang-default-resource-limits.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libglslang.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libMachineIndependent.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libOSDependent.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools-diff.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools-lint.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools-opt.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools-reduce.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPVRemapper.a
        -llog
        -latomic
        -lc
        -lm
    )
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set_target_properties(lib_shared PROPERTIES OUTPUT_NAME "libSuperResolution+${LIB_PLATFORM}")
    target_link_libraries(lib_shared
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/glslang-default-resource-limits.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/glslang.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/OSDependent.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/MachineIndependent.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/GenericCodeGen.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/SPIRV.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/SPIRV-Tools.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/SPIRV-Tools-opt.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/SPIRV-Tools-reduce.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/SPIRV-Tools-lint.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/SPIRV-Tools-diff.lib
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/SPVRemapper.lib
    )
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    target_link_libraries(lib_shared
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libGenericCodeGen.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libglslang-default-resource-limits.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libglslang.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libMachineIndependent.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libOSDependent.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools-diff.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools-lint.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools-opt.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools-reduce.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV-Tools.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPIRV.a
        ${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM}/libSPVRemapper.a
        -lstdc++fs 
    )
    
    set_target_properties(lib_shared PROPERTIES
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
    )
    target_compile_options(lib_shared PRIVATE -fvisibility=hidden)
endif()

set_target_properties(lib_shared PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/bin"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/bin"
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/bin"
)