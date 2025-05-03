# This file is part of the FidelityFX SDK.
#
# Copyright (c) 2022-2023 Advanced Micro Devices, Inc. All rights reserved.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

cmake_minimum_required(VERSION 3.15)
set(CMAKE_DEBUG_POSTFIX d)
option (FFX_FSR2_API_GL "Build FSR 2.0 OpenGL backend" ON)
option (FFX_FSR2_API_VK "Build FSR 2.0 VK backend" OFF)

#set(CMAKE_CXX_FLAGS "${CMAKE_C_FLAGS} -O3 -Wall -g")
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -Wall -g")
set(CMAKE_CXX_FLAGS "${CMAKE_C_FLAGS} -Wall -g")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -g")

set(FSR2_AUTO_COMPILE_SHADERS ON CACHE BOOL "Compile shaders automatically as a prebuild step.")

if(CMAKE_GENERATOR STREQUAL "Ninja")
    set(USE_DEPFILE TRUE)
else()
    set(USE_DEPFILE FALSE)
endif()

set(FSR2_PLATFORM_NAME x64)
if ((CMAKE_CXX_COMPILER_ID STREQUAL "GNU"))
    add_compile_definitions(FFX_GCC) # Should work with gcc and others
endif()
## Embed PDBs in the debug versions of the libs
#set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} /Z7")
#set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY_DEBUG   ${CMAKE_HOME_DIRECTORY}/bin)
#set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE ${CMAKE_HOME_DIRECTORY}/bin)

add_compile_definitions(_UNICODE)
add_compile_definitions(UNICODE)
set(FFX_SC_BASE_ARGS
    -reflection -deps=gcc -DFFX_GPU=1
    -DFFX_FSR2_OPTION_UPSAMPLE_SAMPLERS_USE_DATA_HALF=0
    -DFFX_FSR2_OPTION_ACCUMULATE_SAMPLERS_USE_DATA_HALF=0
    -DFFX_FSR2_OPTION_REPROJECT_SAMPLERS_USE_DATA_HALF=1
    -DFFX_FSR2_OPTION_POSTPROCESSLOCKSTATUS_SAMPLERS_USE_DATA_HALF=0
    # Upsample uses lanczos approximation
    -DFFX_FSR2_OPTION_UPSAMPLE_USE_LANCZOS_TYPE=2
    )
if (WIN32)
	set(FFX_SC_PERMUTATION_ARGS
		# Reproject can use either reference lanczos or LUT
		-DFFX_FSR2_OPTION_REPROJECT_USE_LANCZOS_TYPE={0,1}
		-DFFX_FSR2_OPTION_HDR_COLOR_INPUT={0,1}
		-DFFX_FSR2_OPTION_LOW_RESOLUTION_MOTION_VECTORS={0,1}
		-DFFX_FSR2_OPTION_JITTERED_MOTION_VECTORS={0,1}
		-DFFX_FSR2_OPTION_INVERTED_DEPTH={0,1}
		-DFFX_FSR2_OPTION_APPLY_SHARPENING={0,1}
	)
else()
	# Fix bash curly braces replacement
	set(FFX_SC_PERMUTATION_ARGS
		# Reproject can use either reference lanczos or LUT
		'-DFFX_FSR2_OPTION_REPROJECT_USE_LANCZOS_TYPE={0,1}'
		'-DFFX_FSR2_OPTION_HDR_COLOR_INPUT={0,1}'
		'-DFFX_FSR2_OPTION_LOW_RESOLUTION_MOTION_VECTORS={0,1}'
		'-DFFX_FSR2_OPTION_JITTERED_MOTION_VECTORS={0,1}'
		'-DFFX_FSR2_OPTION_INVERTED_DEPTH={0,1}'
		'-DFFX_FSR2_OPTION_APPLY_SHARPENING={0,1}'
	)
endif()

file(GLOB SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/*.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/*.h")
add_library(ffx_fsr2_api_${FSR2_PLATFORM_NAME} ${SOURCES})
add_subdirectory(gl)
#add_subdirectory(vk)
# api
source_group("source"  FILES ${SOURCES})

set_source_files_properties(${SHADERS} PROPERTIES HEADER_FILE_ONLY TRUE)

set(LIBRARY_OUTPUT_PATH ${PROJECT_BINARY_DIR}/../bin)
