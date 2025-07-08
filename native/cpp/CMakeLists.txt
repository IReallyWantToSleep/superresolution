cmake_minimum_required(VERSION 3.15)
project(SuperResolution)

set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)

if(NOT DEFINED SRLIB_VERSION)
    string(TIMESTAMP COMPILE_TIME "%Y%m%d_%H%M%S")
    set(SRLIB_VERSION ${COMPILE_TIME})
endif()
add_definitions(-DSRLIB_VERSION="${SRLIB_VERSION}")

if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    add_definitions(-DON_LINUX64 -DFFX_GCC)
    set(LIB_PLATFORM "linux64")
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    add_definitions(-DON_WIN64)
    set(LIB_PLATFORM "win64")
elseif(CMAKE_SYSTEM_NAME MATCHES "Android")
    add_definitions(-DON_ANDROID)
    set(LIB_PLATFORM "android")
elseif(CMAKE_SYSTEM_NAME MATCHES "Darwin")
    add_definitions(-DON_MACOS)
    set(LIB_PLATFORM "macosarm64")
else()
    message(FATAL_ERROR "Unsupported platform: ${CMAKE_SYSTEM_NAME}")
endif()

link_directories(${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM})

if(WIN32)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -g -static-libgcc -static-libstdc++ -O3")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -g -static-libgcc -O3")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -static")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -g -O3")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -g -O3")
endif()

include_directories(
    ${PROJECT_SOURCE_DIR}/include
    ${PROJECT_SOURCE_DIR}/src
    ${PROJECT_SOURCE_DIR}/third_party
)

aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)

add_library(lib_shared SHARED ${ALL_SRC})
set_target_properties(lib_shared PROPERTIES OUTPUT_NAME "SuperResolution+${LIB_PLATFORM}")

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
)

if(WIN32)
    target_link_libraries(lib_shared
        -lstdc++fs
        -lucrt
        -lmsvcrt
        -lkernel32
        -luser32
        -lgdi32
        -ladvapi32
    )
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    target_link_libraries(lib_shared -lstdc++fs -pthread -ldl)
endif()

# ---- Output directories ----
set_target_properties(lib_shared PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/bin"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/bin"
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/bin"
)
