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

# begin sampler and texture bindings at 8 so generated image bindings start at a low index
set(FFX_SC_GL_BASE_ARGS
    -compiler=glslang -e main --target-env opengl --target-env spirv1.3 --amb --stb comp 8 --ssb comp 8 --sib comp 0 --suavb comp 0 -Os -S comp -DFFX_GLSL=1)

#set(CMAKE_CXX_FLAGS "${CMAKE_C_FLAGS} -O3 -g -Wall")
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -g -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_C_FLAGS} -g -Wall")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -g -Wall")

file(GLOB SHADERS
    "${CMAKE_CURRENT_SOURCE_DIR}/../shaders/*.h"
    "${CMAKE_CURRENT_SOURCE_DIR}/../shaders/*.glsl2")

set(PASS_SHADERS
    ${CMAKE_CURRENT_SOURCE_DIR}/../shaders/ffx_fsr2_tcr_autogen_pass.glsl2
    ${CMAKE_CURRENT_SOURCE_DIR}/../shaders/ffx_fsr2_autogen_reactive_pass.glsl2
    ${CMAKE_CURRENT_SOURCE_DIR}/../shaders/ffx_fsr2_accumulate_pass.glsl2
    ${CMAKE_CURRENT_SOURCE_DIR}/../shaders/ffx_fsr2_compute_luminance_pyramid_pass.glsl2
    ${CMAKE_CURRENT_SOURCE_DIR}/../shaders/ffx_fsr2_depth_clip_pass.glsl2
    ${CMAKE_CURRENT_SOURCE_DIR}/../shaders/ffx_fsr2_lock_pass.glsl2
    ${CMAKE_CURRENT_SOURCE_DIR}/../shaders/ffx_fsr2_reconstruct_previous_depth_pass.glsl2
    ${CMAKE_CURRENT_SOURCE_DIR}/../shaders/ffx_fsr2_rcas_pass.glsl2)

file(GLOB_RECURSE GL
    "${CMAKE_CURRENT_SOURCE_DIR}/../ffx_assert.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/*.h"
    "${CMAKE_CURRENT_SOURCE_DIR}/*.cpp")

add_library(ffx_fsr2_api_gl_${FSR2_PLATFORM_NAME} ${GL})

find_library(LOG_LIBRARY LOG)
find_library(OPENGL_ES3_LIBRARY OpenGL ES3)
#find_library(OpenGL REQUIRED)

target_include_directories(ffx_fsr2_api_gl_${FSR2_PLATFORM_NAME} PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/../shaders/gl)

file(MAKE_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/../shaders/gl)

if (FSR2_AUTO_COMPILE_SHADERS)
    set(FFX_SC_DEPENDENT_TARGET ffx_fsr2_api_gl_${FSR2_PLATFORM_NAME})
else()
    set(FFX_SC_DEPENDENT_TARGET ffx_fsr2_api_gl_shaders_${FSR2_PLATFORM_NAME})
    add_custom_target(${FFX_SC_DEPENDENT_TARGET})
endif()

if(${CMAKE_VERSION} VERSION_GREATER_EQUAL "3.20.0") 
cmake_policy(SET CMP0116 OLD)
endif()
get_filename_component(PASS_SHADER_OUTPUT_PATH ${CMAKE_CURRENT_BINARY_DIR}/../shaders/gl ABSOLUTE)
foreach(PASS_SHADER ${PASS_SHADERS})
    get_filename_component(PASS_SHADER_FILENAME ${PASS_SHADER} NAME_WE)
    get_filename_component(PASS_SHADER_TARGET ${PASS_SHADER} NAME_WLE)
    set(PERMUTATION_HEADER ${PASS_SHADER_OUTPUT_PATH}/${PASS_SHADER_TARGET}_permutations.h)

    # combine base and permutation args
    if (${PASS_SHADER_FILENAME} STREQUAL "ffx_fsr2_compute_luminance_pyramid_pass")
        # skip 16-bit permutations for the compute luminance pyramid pass
        set(FFX_SC_ARGS ${FFX_SC_BASE_ARGS} ${FFX_SC_GL_BASE_ARGS} ${FFX_SC_PERMUTATION_ARGS} -DFFX_HALF=0)
    else()
        set(FFX_SC_ARGS ${FFX_SC_BASE_ARGS} ${FFX_SC_GL_BASE_ARGS} ${FFX_SC_PERMUTATION_ARGS} -DFFX_HALF={0,1})
    endif()

    if(USE_DEPFILE)
        add_custom_command(
            OUTPUT ${PERMUTATION_HEADER}
            COMMAND ${FFX_SC_EXECUTABLE} ${FFX_SC_ARGS} -name=${PASS_SHADER_FILENAME} -I${CMAKE_CURRENT_SOURCE_DIR}/shaders -output=${PASS_SHADER_OUTPUT_PATH} ${PASS_SHADER}
            WORKING_DIRECTORY ${CMAKE_BINARY_DIR}
            DEPENDS ${PASS_SHADER}
            DEPFILE ${PERMUTATION_HEADER}.d
        )
        list(APPEND PERMUTATION_OUTPUTS ${PERMUTATION_HEADER})
    else()
        add_custom_command(
            OUTPUT ${PERMUTATION_HEADER}
            COMMAND ${FFX_SC_EXECUTABLE} ${FFX_SC_ARGS} -name=${PASS_SHADER_FILENAME} -I${CMAKE_CURRENT_SOURCE_DIR}/shaders -output=${PASS_SHADER_OUTPUT_PATH} ${PASS_SHADER}
            WORKING_DIRECTORY ${CMAKE_BINARY_DIR}
            DEPENDS ${PASS_SHADER}
        )
        list(APPEND PERMUTATION_OUTPUTS ${PERMUTATION_HEADER})
    endif()
endforeach(PASS_SHADER)

add_custom_target(shader_permutations_gl DEPENDS ${PERMUTATION_OUTPUTS})
add_dependencies(${FFX_SC_DEPENDENT_TARGET} shader_permutations_gl)

target_link_libraries(ffx_fsr2_api_gl_${FSR2_PLATFORM_NAME} ffx_lib_glad)

source_group("source"  FILES ${GL})
source_group("shaders" FILES ${SHADERS})

set(LIBRARY_OUTPUT_PATH ${PROJECT_BINARY_DIR}/../bin)