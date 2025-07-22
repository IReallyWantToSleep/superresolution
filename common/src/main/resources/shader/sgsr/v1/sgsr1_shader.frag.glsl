#version 410

//============================================================================================================
//
//
//                  Copyright (c) 2023, Qualcomm Innovation Center, Inc. All rights reserved.
//                              SPDX-License-Identifier: BSD-3-Clause
//
//============================================================================================================

precision mediump float;
precision highp int;

#ifndef SR_GL41_COMPAT
#extension GL_ARB_shading_language_420pack : enable
#endif

#if SR_GL41_COMPAT
layout(std140) uniform sgsr1_data {
    vec2 renderSize;
    vec2 renderSizeRcp;
    float EdgeSharpness;
    float EdgeThreshold;
};
uniform mediump sampler2D ps0;
#else
layout(binding = 0) uniform sgsr1_data
{
    vec2 renderSize;
    vec2 renderSizeRcp;
    float EdgeSharpness;
    float EdgeThreshold;
};
layout(binding = 1) uniform mediump sampler2D ps0;
#endif


layout(location = 0) in mediump vec2 in_TEXCOORD0;
layout(location = 0) out mediump vec4 out_Target0;

float fastLanczos2(float x)
{
    float wA = x - 4.0;
    float wB = x * wA - wA;
    wA *= wA;
    return wB * wA;
}

#if defined(UseEdgeDirection)
vec2 weightY(float dx, float dy, float c, vec3 data)
#else
vec2 weightY(float dx, float dy, float c, float data)
#endif
{
    #if defined(UseEdgeDirection)
    float std = data.x;
    vec2 dir = data.yz;

    float edgeDis = ((dx * dir.y) + (dy * dir.x));
    float x = (((dx * dx) + (dy * dy)) + ((edgeDis * edgeDis) * ((clamp(((c * c) * std), 0.0, 1.0) * 0.7) + -1.0)));
    #else
    float std = data;
    float x = ((dx * dx) + (dy * dy)) * 0.55 + clamp(abs(c) * std, 0.0, 1.0);
    #endif

    float w = fastLanczos2(x);
    return vec2(w, w * c);
}

vec2 edgeDirection(vec4 left, vec4 right)
{
    vec2 dir;
    float RxLz = (right.x + (-left.z));
    float RwLy = (right.w + (-left.y));
    vec2 delta;
    delta.x = (RxLz + RwLy);
    delta.y = (RxLz + (-RwLy));
    float lengthInv = inversesqrt((delta.x * delta.x + 3.075740e-05) + (delta.y * delta.y));
    dir.x = (delta.x * lengthInv);
    dir.y = (delta.y * lengthInv);
    return dir;
}

void main()
{
    vec4 color;
    color.xyz = textureLod(ps0, in_TEXCOORD0.xy, 0.0).xyz;
    highp float xCenter;
    xCenter = abs(in_TEXCOORD0.x + -0.5);
    highp float yCenter;
    yCenter = abs(in_TEXCOORD0.y + -0.5);
    highp vec2 imgCoord = ((in_TEXCOORD0.xy * renderSize) + vec2(-0.5, 0.5));
    highp vec2 imgCoordPixel = floor(imgCoord);
    highp vec2 coord = (imgCoordPixel * renderSizeRcp);
    vec2 pl = (imgCoord - imgCoordPixel);
    vec4 left = textureGather(ps0, coord, 1);

    float edgeVote = abs(left.z - left.y) + abs(color[1] - left.y) + abs(color[1] - left.z);
    if (edgeVote > EdgeThreshold)
    {
        coord.x += renderSizeRcp.x;
        highp vec2 offset = vec2(renderSizeRcp.x, 0.0);
        vec4 right = textureGather(ps0, coord + offset, 1);
        vec4 upDown;
        highp vec2 offset1 = vec2(0.0, -renderSizeRcp.y);
        upDown.xy = textureGather(ps0, coord + offset1, 1).wz;
        highp vec2 offset2 = vec2(0.0, renderSizeRcp.y);
        upDown.zw = textureGather(ps0, coord + offset2, 1).yx;

        float mean = (left.y + left.z + right.x + right.w) * 0.25;
        left = left - vec4(mean);
        right = right - vec4(mean);
        upDown = upDown - vec4(mean);
        color.w = color[1] - mean;

        float sum = (((((abs(left.x) + abs(left.y)) + abs(left.z)) + abs(left.w)) + (((abs(right.x) + abs(right.y)) + abs(right.z)) + abs(right.w))) + (((abs(upDown.x) + abs(upDown.y)) + abs(upDown.z)) + abs(upDown.w)));
        float sumMean = 1.014185e+01 / sum;
        float std = (sumMean * sumMean);

        #if defined(UseEdgeDirection)
        vec3 data = vec3(std, edgeDirection(left, right));
        #else
        float data = std;
        #endif

        vec2 aWY = weightY(pl.x, pl.y + 1.0, upDown.x, data);
        aWY += weightY(pl.x - 1.0, pl.y + 1.0, upDown.y, data);
        aWY += weightY(pl.x - 1.0, pl.y - 2.0, upDown.z, data);
        aWY += weightY(pl.x, pl.y - 2.0, upDown.w, data);
        aWY += weightY(pl.x + 1.0, pl.y - 1.0, left.x, data);
        aWY += weightY(pl.x, pl.y - 1.0, left.y, data);
        aWY += weightY(pl.x, pl.y, left.z, data);
        aWY += weightY(pl.x + 1.0, pl.y, left.w, data);
        aWY += weightY(pl.x - 1.0, pl.y - 1.0, right.x, data);
        aWY += weightY(pl.x - 2.0, pl.y - 1.0, right.y, data);
        aWY += weightY(pl.x - 2.0, pl.y, right.z, data);
        aWY += weightY(pl.x - 1.0, pl.y, right.w, data);

        float finalY = aWY.y / aWY.x;
        float maxY = max(max(left.y, left.z), max(right.x, right.w));
        float minY = min(min(left.y, left.z), min(right.x, right.w));
        float deltaY = clamp(EdgeSharpness * finalY, minY, maxY) - color.w;

        deltaY = clamp(deltaY, -23.0 / 255.0, 23.0 / 255.0);

        color.x = clamp((color.x + deltaY), 0.0, 1.0);
        color.y = clamp((color.y + deltaY), 0.0, 1.0);
        color.z = clamp((color.z + deltaY), 0.0, 1.0);
    }

    color.w = 1.0;
    out_Target0.xyzw = color;
}