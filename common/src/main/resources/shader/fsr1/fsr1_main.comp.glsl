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
#extension GL_GOOGLE_include_directive : require
#if FSR_FP16_CRITERIA == 1
#extension GL_EXT_shader_16bit_storage: require
#extension GL_EXT_shader_explicit_arithmetic_types: require
#elif FSR_FP16_CRITERIA == 2
#extension GL_NV_gpu_shader5: enable
#endif

layout(local_size_x = 64, local_size_y = 1, local_size_z = 1) in;
//输入/输出
#if SR_VULKAN
    #if FSR_EASU == 1
    layout(set = 0,binding = 0) uniform sampler2D inImage;
    layout(set = 0,binding = 1, SR_INTERNAL_TEXTURE_FORMAT) writeonly uniform image2D outImage;
    #endif
    #if FSR_RCAS == 1
    layout(set = 0,binding = 0, SR_INTERNAL_TEXTURE_FORMAT) readonly uniform image2D inImage;
    layout(set = 0,binding = 1, SR_INTERNAL_TEXTURE_FORMAT) writeonly uniform image2D outImage;
    #endif
#else
    #if FSR_EASU == 1
    layout(binding = 0) uniform sampler2D inImage;
    layout(binding = 1, SR_INTERNAL_TEXTURE_FORMAT) writeonly uniform image2D outImage;
    #endif
    #if FSR_RCAS == 1
    layout(binding = 0, SR_INTERNAL_TEXTURE_FORMAT) readonly uniform image2D inImage;
    layout(binding = 1, SR_INTERNAL_TEXTURE_FORMAT) writeonly uniform image2D outImage;
    #endif
#endif
//uniforms
#if SR_VULKAN
layout(set = 0,binding = 2, std140) uniform fsr1_data_t {
    vec2 renderViewportSize;
    vec2 containerTextureSize;
    vec2 upscaledViewportSize;
    float sharpness;
} fsr1_data;
#else
layout(binding = 2, std140) uniform fsr1_data_t {
    vec2 renderViewportSize;
    vec2 containerTextureSize;
    vec2 upscaledViewportSize;
    float sharpness;
} fsr1_data;
#endif

#define A_GPU 1
#define A_GLSL 1

#if FSR_HALF == 1
    #define A_HALF
    #define A_NO_16_BIT_CAST 1
#endif

#include "fsr1/ffx_a.h"

#if FSR_EASU == 1
    #if FSR_HALF == 1
        #define FSR_EASU_H 1
    #else
        #define FSR_EASU_F 1
    #endif
#endif

#if FSR_RCAS == 1
    #if FSR_HALF == 1
        #define FSR_RCAS_H 1
    #else
        #define FSR_RCAS_F 1
    #endif
#endif

#if FSR_EASU == 1
    #if FSR_HALF == 1
AH4 FsrEasuRH(AF2 p) { return AH4(textureGather(inImage, p, 0)); }
AH4 FsrEasuGH(AF2 p) { return AH4(textureGather(inImage, p, 1)); }
AH4 FsrEasuBH(AF2 p) { return AH4(textureGather(inImage, p, 2)); }
    #else
AF4 FsrEasuRF(AF2 p) { return AF4(textureGather(inImage, p, 0)); }
AF4 FsrEasuGF(AF2 p) { return AF4(textureGather(inImage, p, 1)); }
AF4 FsrEasuBF(AF2 p) { return AF4(textureGather(inImage, p, 2)); }
    #endif
#endif

#if FSR_RCAS == 1
    #if FSR_HALF == 1
AH4 FsrRcasLoadH(ASW2 p) { return AH4(imageLoad(inImage, ivec2(p))); }
void FsrRcasInputH(inout AH1 r, inout AH1 g, inout AH1 b) {}
    #else
AF4 FsrRcasLoadF(ASU2 p) { return AF4(imageLoad(inImage, ivec2(p))); }
void FsrRcasInputF(inout AF1 r, inout AF1 g, inout AF1 b) {}
    #endif
#endif


#include "fsr1/ffx_fsr1.h"

bool fsr1InBounds(uvec2 p, uvec2 sizeXY) {
    return p.x < sizeXY.x && p.y < sizeXY.y;
}



#if FSR_RCAS == 1
void mainRcas() {
    uvec4 const0;
    FsrRcasCon(const0, fsr1_data.sharpness);
    uvec2 sizeXY = uvec2(imageSize(outImage));
    uvec2 gxy = ARmp8x8(gl_LocalInvocationID.x) + uvec2(gl_WorkGroupID.x << 4u, gl_WorkGroupID.y << 4u);

    if (fsr1InBounds(gxy, sizeXY)) {
        #if FSR_HALF == 1
        AH1 outR = AH1_(0.0), outG = AH1_(0.0), outB = AH1_(0.0);
        FsrRcasH(outR, outG, outB, gxy, const0);
        imageStore(outImage, ivec2(gxy), vec4(AF1(outR), AF1(outG), AF1(outB), 1.0));
        #else
        AF1 outR = AF1_(0.0), outG = AF1_(0.0), outB = AF1_(0.0);
        FsrRcasF(outR, outG, outB, gxy, const0);
        imageStore(outImage, ivec2(gxy), vec4(outR, outG, outB, 1.0));
        #endif
    }

    gxy.x += 8;
    if (fsr1InBounds(gxy, sizeXY)) {
        #if FSR_HALF == 1
        AH1 outR = AH1_(0.0), outG = AH1_(0.0), outB = AH1_(0.0);
        FsrRcasH(outR, outG, outB, gxy, const0);
        imageStore(outImage, ivec2(gxy), vec4(AF1(outR), AF1(outG), AF1(outB), 1.0));
        #else
        AF1 outR = AF1_(0.0), outG = AF1_(0.0), outB = AF1_(0.0);
        FsrRcasF(outR, outG, outB, gxy, const0);
        imageStore(outImage, ivec2(gxy), vec4(outR, outG, outB, 1.0));
        #endif
    }

    gxy.y += 8;
    if (fsr1InBounds(gxy, sizeXY)) {
        #if FSR_HALF == 1
        AH1 outR = AH1_(0.0), outG = AH1_(0.0), outB = AH1_(0.0);
        FsrRcasH(outR, outG, outB, gxy, const0);
        imageStore(outImage, ivec2(gxy), vec4(AF1(outR), AF1(outG), AF1(outB), 1.0));
        #else
        AF1 outR = AF1_(0.0), outG = AF1_(0.0), outB = AF1_(0.0);
        FsrRcasF(outR, outG, outB, gxy, const0);
        imageStore(outImage, ivec2(gxy), vec4(outR, outG, outB, 1.0));
        #endif
    }

    gxy.x -= 8;
    if (fsr1InBounds(gxy, sizeXY)) {
        #if FSR_HALF == 1
        AH1 outR = AH1_(0.0), outG = AH1_(0.0), outB = AH1_(0.0);
        FsrRcasH(outR, outG, outB, gxy, const0);
        imageStore(outImage, ivec2(gxy), vec4(AF1(outR), AF1(outG), AF1(outB), 1.0));
        #else
        AF1 outR = AF1_(0.0), outG = AF1_(0.0), outB = AF1_(0.0);
        FsrRcasF(outR, outG, outB, gxy, const0);
        imageStore(outImage, ivec2(gxy), vec4(outR, outG, outB, 1.0));
        #endif
    }
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

    uvec2 sizeXY = uvec2(imageSize(outImage));
    uvec2 gxy = ARmp8x8(gl_LocalInvocationID.x) + uvec2(gl_WorkGroupID.x << 4u, gl_WorkGroupID.y << 4u);

    if (fsr1InBounds(gxy, sizeXY)) {
        #if FSR_HALF == 1
        AH3 gamma2Color = AH3_(0.0);
        FsrEasuH(gamma2Color, gxy, const0, const1, const2, const3);
        imageStore(outImage, ivec2(gxy), vec4(AF3(gamma2Color), 1.0));
        #else
        AF3 gamma2Color = AF3_(0.0);
        FsrEasuF(gamma2Color, gxy, const0, const1, const2, const3);
        imageStore(outImage, ivec2(gxy), vec4(gamma2Color, 1.0));
        #endif
    }

    gxy.x += 8;
    if (fsr1InBounds(gxy, sizeXY)) {
        #if FSR_HALF == 1
        AH3 gamma2Color = AH3_(0.0);
        FsrEasuH(gamma2Color, gxy, const0, const1, const2, const3);
        imageStore(outImage, ivec2(gxy), vec4(AF3(gamma2Color), 1.0));
        #else
        AF3 gamma2Color = AF3_(0.0);
        FsrEasuF(gamma2Color, gxy, const0, const1, const2, const3);
        imageStore(outImage, ivec2(gxy), vec4(gamma2Color, 1.0));
        #endif
    }

    gxy.y += 8;
    if (fsr1InBounds(gxy, sizeXY)) {
        #if FSR_HALF == 1
        AH3 gamma2Color = AH3_(0.0);
        FsrEasuH(gamma2Color, gxy, const0, const1, const2, const3);
        imageStore(outImage, ivec2(gxy), vec4(AF3(gamma2Color), 1.0));
        #else
        AF3 gamma2Color = AF3_(0.0);
        FsrEasuF(gamma2Color, gxy, const0, const1, const2, const3);
        imageStore(outImage, ivec2(gxy), vec4(gamma2Color, 1.0));
        #endif
    }

    gxy.x -= 8;
    if (fsr1InBounds(gxy, sizeXY)) {
        #if FSR_HALF == 1
        AH3 gamma2Color = AH3_(0.0);
        FsrEasuH(gamma2Color, gxy, const0, const1, const2, const3);
        imageStore(outImage, ivec2(gxy), vec4(AF3(gamma2Color), 1.0));
        #else
        AF3 gamma2Color = AF3_(0.0);
        FsrEasuF(gamma2Color, gxy, const0, const1, const2, const3);
        imageStore(outImage, ivec2(gxy), vec4(gamma2Color, 1.0));
        #endif
    }
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
