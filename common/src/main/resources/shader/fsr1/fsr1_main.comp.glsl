// This file is part of the FidelityFX SDK.
//
// Copyright (C) 2024 Advanced Micro Devices, Inc.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and /or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

#version 460
//--insert--define--//

//扩展
#extension GL_GOOGLE_include_directive: require
#if FSR_FP16_CRITERIA == 1
#extension GL_EXT_shader_16bit_storage: require
#extension GL_EXT_shader_explicit_arithmetic_types: require
#elif FSR_FP16_CRITERIA == 2
#extension GL_NV_gpu_shader5: enable
#endif

layout(local_size_x = 64, local_size_y = 1, local_size_z = 1) in;
//输入/输出
#if FSR_EASU == 1
layout(binding = 0) uniform sampler2D inImage;
layout(binding = 1, r11f_g11f_b10f) writeonly uniform image2D outImage;
#endif
#if FSR_RCAS == 1
layout(binding = 0, r11f_g11f_b10f) readonly uniform image2D inImage;
layout(binding = 1, r11f_g11f_b10f) writeonly uniform image2D outImage;
#endif
//uniforms
layout(binding = 0, std140) uniform fsr1_data_t {
    vec2 renderViewportSize;
    vec2 containerTextureSize;
    vec2 upscaledViewportSize;
    float sharpness;
} fsr1_data;
//类型
#if FSR_HALF == 1
#define VEC4 f16vec4
#define VEC3 f16vec3
#define VEC2 f16vec2
#define FLOAT float16_t
#define UNIT uint16_t
#else
#define VEC4 vec4
#define VEC3 vec3
#define VEC2 vec2
#define FLOAT float
#define UNIT uint
#endif

#if FSR_HALF == 1
uint16_t halfBitsToUint16(float16_t v) {
    return uint16_t(packFloat2x16(f16vec2(v, 0)));
}
u16vec2 halfBitsToUint16(f16vec2 v) {
    return u16vec2(packFloat2x16(f16vec2(v.x, 0)), packFloat2x16(f16vec2(v.y, 0)));
}
u16vec3 halfBitsToUint16(f16vec3 v) {
    return u16vec3(packFloat2x16(f16vec2(v.x, 0)), packFloat2x16(f16vec2(v.y, 0)), packFloat2x16(f16vec2(v.z, 0)));
}
u16vec4 halfBitsToUint16(f16vec4 v) {
    return u16vec4(packFloat2x16(f16vec2(v.x, 0)), packFloat2x16(f16vec2(v.y, 0)), packFloat2x16(f16vec2(v.z, 0)), packFloat2x16(f16vec2(v.w, 0)));
}

float16_t uint16BitsToHalf(uint16_t v) {
    return unpackFloat2x16(uint(v)).x;
}
f16vec2 uint16BitsToHalf(u16vec2 v) {
    return f16vec2(unpackFloat2x16(uint(v.x)).x, unpackFloat2x16(uint(v.y)).x);
}
f16vec3 uint16BitsToHalf(u16vec3 v) {
    return f16vec3(unpackFloat2x16(uint(v.x)).x, unpackFloat2x16(uint(v.y)).x, unpackFloat2x16(uint(v.z)).x);
}
f16vec4 uint16BitsToHalf(u16vec4 v) {
    return f16vec4(unpackFloat2x16(uint(v.x)).x, unpackFloat2x16(uint(v.y)).x, unpackFloat2x16(uint(v.z)).x, unpackFloat2x16(uint(v.w)).x);
}

uint32_t packUint2x16(u16vec2 v) {
    return (uint(v.y) << 16) | uint(v.x);
}
u16vec2 unpackUint2x16(uint32_t v) {
    return u16vec2(v & 0xffff, (v >> 16) & 0xffff);
}
#endif

#include "fsr1/fsr1_common.glsl"
#if FSR_HALF == 1
#if FSR_RCAS == 1
#include "fsr1/fsr1_rcas_fp16.comp.glsl"
#endif
#if FSR_EASU == 1
#include "fsr1/fsr1_easu_fp16.comp.glsl"
#endif
#define FsrEasu FsrEasuH
#define FsrRcas FsrRcasH
#else
#if FSR_RCAS == 1
#include "fsr1/fsr1_rcas.comp.glsl"
#endif
#if FSR_EASU == 1
#include "fsr1/fsr1_easu.comp.glsl"
#endif
#define FsrEasu FsrEasuF
#define FsrRcas FsrRcasF
#endif

#if FSR_RCAS == 1
void mainRcas() {
    uvec4 const0;
    FsrRcasCon(const0, fsr1_data.sharpness);
    uvec2 gxy = ARmp8x8(gl_LocalInvocationID.x) + uvec2(gl_WorkGroupID.x << 4u, gl_WorkGroupID.y << 4u);
    VEC3 gamma2Color = VEC3(0, 0, 0);
    FsrRcas(gamma2Color.r, gamma2Color.g, gamma2Color.b, gxy, const0);
    imageStore(outImage, ivec2(gxy), vec4(vec3(gamma2Color), 0.0));
    gxy.x += 8;
    FsrRcas(gamma2Color.r, gamma2Color.g, gamma2Color.b, gxy, const0);
    imageStore(outImage, ivec2(gxy), vec4(vec3(gamma2Color), 0.0));
    gxy.y += 8;
    FsrRcas(gamma2Color.r, gamma2Color.g, gamma2Color.b, gxy, const0);
    imageStore(outImage, ivec2(gxy), vec4(vec3(gamma2Color), 0.0));
    gxy.x -= 8;
    FsrRcas(gamma2Color.r, gamma2Color.g, gamma2Color.b, gxy, const0);
    imageStore(outImage, ivec2(gxy), vec4(vec3(gamma2Color), 0.0));
}
#endif
#if FSR_EASU == 1
void mainEasu() {
    uvec4 const0, const1, const2, const3;
    FsrEasuCon(const0, const1, const2, const3,
        float(fsr1_data.renderViewportSize.x),
        float(fsr1_data.renderViewportSize.y),
        float(fsr1_data.containerTextureSize.x),
        float(fsr1_data.containerTextureSize.y),
        float(fsr1_data.upscaledViewportSize.x),
        float(fsr1_data.upscaledViewportSize.y));

    uvec2 gxy = ARmp8x8(gl_LocalInvocationID.x) + uvec2(gl_WorkGroupID.x << 4u, gl_WorkGroupID.y << 4u);
    VEC3 gamma2Color = VEC3(0, 0, 0);
    FsrEasu(gamma2Color, gxy, const0, const1, const2, const3);
    imageStore(outImage, ivec2(gxy), vec4(vec3(gamma2Color), 0.0));
    gxy.x += 8;
    FsrEasu(gamma2Color, gxy, const0, const1, const2, const3);
    imageStore(outImage, ivec2(gxy), vec4(vec3(gamma2Color), 0.0));
    gxy.y += 8;
    FsrEasu(gamma2Color, gxy, const0, const1, const2, const3);
    imageStore(outImage, ivec2(gxy), vec4(vec3(gamma2Color), 0.0));
    gxy.x -= 8;
    FsrEasu(gamma2Color, gxy, const0, const1, const2, const3);
    imageStore(outImage, ivec2(gxy), vec4(vec3(gamma2Color), 0.0));
}
#endif

void main() {
    #if FSR_RCAS == 1
    mainRcas();
    #endif
    #if FSR_EASU == 1
    mainEasu();
    #endif
}
