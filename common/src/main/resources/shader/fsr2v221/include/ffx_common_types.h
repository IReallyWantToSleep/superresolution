// This file is part of the FidelityFX SDK.
//
// Copyright (c) 2022-2023 Advanced Micro Devices, Inc. All rights reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
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
#ifndef FFX_COMMON_TYPES_H
#define FFX_COMMON_TYPES_H

#if defined(FFX_CPU)
#define FFX_PARAMETER_IN
#define FFX_PARAMETER_OUT
#define FFX_PARAMETER_INOUT
#elif defined(FFX_GLSL)
#define FFX_PARAMETER_IN        in
#define FFX_PARAMETER_OUT       out
#define FFX_PARAMETER_INOUT     inout
#endif // #if defined(FFX_CPU)

#if defined(FFX_GLSL)
#define FfxBoolean   bool
#define FfxFloat32   float
#define FfxFloat32x2 vec2
#define FfxFloat32x3 vec3
#define FfxFloat32x4 vec4
#define FfxUInt32    uint
#define FfxUInt32x2  uvec2
#define FfxUInt32x3  uvec3
#define FfxUInt32x4  uvec4
#define FfxInt32     int
#define FfxInt32x2   ivec2
#define FfxInt32x3   ivec3
#define FfxInt32x4   ivec4
#if FFX_HALF
#define FfxFloat16   float16_t
#define FfxFloat16x2 f16vec2
#define FfxFloat16x3 f16vec3
#define FfxFloat16x4 f16vec4
#define FfxUInt16    uint16_t
#define FfxUInt16x2  u16vec2
#define FfxUInt16x3  u16vec3
#define FfxUInt16x4  u16vec4
#define FfxInt16     int16_t
#define FfxInt16x2   i16vec2
#define FfxInt16x3   i16vec3
#define FfxInt16x4   i16vec4
#endif // FFX_HALF
#endif // #if defined(FFX_GLSL)

// Global toggles:
// #define FFX_HALF            (1)
// #define FFX_HLSL_6_2        (1)

#if FFX_HALF
#else //FFX_HALF

#define FFX_MIN16_SCALAR( TypeName, BaseComponentType )           typedef BaseComponentType TypeName;
#define FFX_MIN16_VECTOR( TypeName, BaseComponentType, COL )      typedef vector<BaseComponentType, COL> TypeName;
#define FFX_MIN16_MATRIX( TypeName, BaseComponentType, ROW, COL ) typedef matrix<BaseComponentType, ROW, COL> TypeName;

#define FFX_16BIT_SCALAR( TypeName, BaseComponentType )           typedef BaseComponentType TypeName;
#define FFX_16BIT_VECTOR( TypeName, BaseComponentType, COL )      typedef vector<BaseComponentType, COL> TypeName;
#define FFX_16BIT_MATRIX( TypeName, BaseComponentType, ROW, COL ) typedef matrix<BaseComponentType, ROW, COL> TypeName;

#endif //FFX_HALF

#if defined(FFX_GPU)
#if defined(FFX_GLSL)

#if FFX_HALF

#define  FFX_MIN16_F  float16_t
#define  FFX_MIN16_F2 f16vec2
#define  FFX_MIN16_F3 f16vec3
#define  FFX_MIN16_F4 f16vec4

#define  FFX_MIN16_I  int16_t
#define  FFX_MIN16_I2 i16vec2
#define  FFX_MIN16_I3 i16vec3
#define  FFX_MIN16_I4 i16vec4

#define  FFX_MIN16_U  uint16_t
#define  FFX_MIN16_U2 u16vec2
#define  FFX_MIN16_U3 u16vec3
#define  FFX_MIN16_U4 u16vec4

#define FFX_16BIT_F  float16_t
#define FFX_16BIT_F2 f16vec2
#define FFX_16BIT_F3 f16vec3
#define FFX_16BIT_F4 f16vec4

#define FFX_16BIT_I  int16_t
#define FFX_16BIT_I2 i16vec2
#define FFX_16BIT_I3 i16vec3
#define FFX_16BIT_I4 i16vec4

#define FFX_16BIT_U  uint16_t
#define FFX_16BIT_U2 u16vec2
#define FFX_16BIT_U3 u16vec3
#define FFX_16BIT_U4 u16vec4

#else // FFX_HALF

#define  FFX_MIN16_F  float
#define  FFX_MIN16_F2 vec2
#define  FFX_MIN16_F3 vec3
#define  FFX_MIN16_F4 vec4

#define  FFX_MIN16_I  int
#define  FFX_MIN16_I2 ivec2
#define  FFX_MIN16_I3 ivec3
#define  FFX_MIN16_I4 ivec4

#define  FFX_MIN16_U  uint
#define  FFX_MIN16_U2 uvec2
#define  FFX_MIN16_U3 uvec3
#define  FFX_MIN16_U4 uvec4

#define FFX_16BIT_F  float
#define FFX_16BIT_F2 vec2
#define FFX_16BIT_F3 vec3
#define FFX_16BIT_F4 vec4

#define FFX_16BIT_I  int
#define FFX_16BIT_I2 ivec2
#define FFX_16BIT_I3 ivec3
#define FFX_16BIT_I4 ivec4

#define FFX_16BIT_U  uint
#define FFX_16BIT_U2 uvec2
#define FFX_16BIT_U3 uvec3
#define FFX_16BIT_U4 uvec4

#endif // FFX_HALF

#endif // #if defined(FFX_GLSL)

#endif // #if defined(FFX_GPU)
#endif // #ifndef FFX_COMMON_TYPES_H
