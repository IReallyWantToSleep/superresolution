cmake_minimum_required(VERSION 3.15)
project(SuperResolution)
set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)

if(NOT DEFINED SRLIB_VERSION)
    string(TIMESTAMP COMPILE_TIME "%Y%m%d_%H%M%S")
    set(SRLIB_VERSION ${COMPILE_TIME})
    message(STATUS "SRLIB_VERSION not defined, using default value: ${SRLIB_VERSION}")
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
else()
    message(FATAL_ERROR "Unsupported platform: ${CMAKE_SYSTEM_NAME}")
endif()
link_directories(${PROJECT_SOURCE_DIR}/third_party/libs/${LIB_PLATFORM})
if(NOT CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -g  -O3")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -g -O3" )
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -g -static-libgcc -static-libstdc++ -O3")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -g -static-libgcc -O3" )
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -static")
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
set_target_properties(lib_shared PROPERTIES OUTPUT_NAME "SuperResolution+${LIB_PLATFORM}")
if (CMAKE_SYSTEM_NAME MATCHES "Android")
    message(STATUS "Android platform detected")
    target_link_libraries(lib_shared
        #OpenGL::GL
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
   )
elseif(CMAKE_SYSTEM_NAME MATCHES "Windows")
    target_link_libraries(lib_shared
        #OpenGL::GL
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
        -static
        -lstdc++fs 
        -lucrt     
        -lmsvcrt   
        -lkernel32
        -luser32
        -lgdi32
        -ladvapi32
    )
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    target_link_libraries(lib_shared
        #OpenGL::GL
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
endif()


set(LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../bin)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../bin)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../bin)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../bin)
set(LIBRARY_OUTPUT_PATH ${PROJECT_BINARY_DIR}/../bin)