cmake_minimum_required(VERSION 3.15)
project(SuperResolution)
set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -g -static-libgcc -static-libstdc++")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -g -static-libgcc" )
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -static")
if(NOT DEFINED SRLIB_VERSION)
    error("")
endif()
add_definitions(-DSRLIB_VERSION="${SRLIB_VERSION}")

set(FFX_SC_EXECUTABLE
    ${CMAKE_CURRENT_SOURCE_DIR}/tools/FidelityFX_SC.exe)
if(CMAKE_SYSTEM_NAME MATCHES "Linux")
    add_definitions(-DON_LINUX -DFFX_GCC)
endif()
include_directories(
    ${PROJECT_BINARY_DIR}/../include
    ${PROJECT_BINARY_DIR}/../src
    ${PROJECT_BINARY_DIR}/../third_party
    )
add_library(ffx_lib_glad INTERFACE)
target_include_directories(ffx_lib_glad INTERFACE ${PROJECT_BINARY_DIR}/../include)
add_subdirectory(src/ffx-fsr2-api)
aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)
add_library(lib_shared SHARED ${ALL_SRC})
set_target_properties(lib_shared PROPERTIES OUTPUT_NAME "SuperResolution")
find_package(OpenGL REQUIRED)
#find_package(Vulkan REQUIRED)
target_link_libraries(lib_shared
    ffx_fsr2_api_gl_x64

    ffx_fsr2_api_x64
    OpenGL::GL
    #Vulkan::Vulkan
)
set(LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../bin)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../bin)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../bin)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../bin)
set(LIBRARY_OUTPUT_PATH ${PROJECT_BINARY_DIR}/../bin)