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


void FsrEasuCon(
    out uvec4 con0,
    out uvec4 con1,
    out uvec4 con2,
    out uvec4 con3,

    float inputViewportInPixelsX,
    float inputViewportInPixelsY,

    float inputSizeInPixelsX,
    float inputSizeInPixelsY,

    float outputSizeInPixelsX,
    float outputSizeInPixelsY) {
    con0[0] = floatBitsToUint(float(inputViewportInPixelsX * (1.0f / outputSizeInPixelsX)));
    con0[1] = floatBitsToUint(float(inputViewportInPixelsY * (1.0f / outputSizeInPixelsY)));
    con0[2] = floatBitsToUint(float((float(0.5)) * inputViewportInPixelsX * (1.0f / outputSizeInPixelsX) - (float(0.5))));
    con0[3] = floatBitsToUint(float((float(0.5)) * inputViewportInPixelsY * (1.0f / outputSizeInPixelsY / 1.0f) - (float(0.5))));

    con1[0] = floatBitsToUint(float((1.0f / inputSizeInPixelsX)));
    con1[1] = floatBitsToUint(float((1.0f / inputSizeInPixelsY)));

    con1[2] = floatBitsToUint(float((float(1.0)) * (1.0f / inputSizeInPixelsX)));
    con1[3] = floatBitsToUint(float((float(-1.0)) * (1.0f / inputSizeInPixelsY)));

    con2[0] = floatBitsToUint(float((float(-1.0)) * (1.0f / inputSizeInPixelsX)));
    con2[1] = floatBitsToUint(float((float(2.0)) * (1.0f / inputSizeInPixelsY)));
    con2[2] = floatBitsToUint(float((float(1.0)) * (1.0f / inputSizeInPixelsX)));
    con2[3] = floatBitsToUint(float((float(2.0)) * (1.0f / inputSizeInPixelsY)));
    con3[0] = floatBitsToUint(float((float(0.0)) * (1.0f / inputSizeInPixelsX)));
    con3[1] = floatBitsToUint(float((float(4.0)) * (1.0f / inputSizeInPixelsY)));
    con3[2] = con3[3] = 0;
}

void FsrRcasCon(
    out uvec4 con,

    float sharpness) {
    sharpness = exp2(float(-sharpness));
    vec2 hSharp = vec2(sharpness, sharpness);
    con[0] = floatBitsToUint(float(sharpness));
    con[1] = packHalf2x16(hSharp);
    con[2] = 0;
    con[3] = 0;
}