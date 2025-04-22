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

float AF1_x(float a) {
    return float(a);
}

uint AU1_x(uint a) {
    return uint(a);
}

uint ABfe(uint src, uint off, uint bits) {
    return bitfieldExtract(src, int(off), int(bits));
}

uint ABfiM(uint src, uint ins, uint bits) {
    return bitfieldInsert(src, ins, 0, int(bits));
}

float AMax3F1(float x, float y, float z) {
    return max(x, max(y, z));
}

float AMin3F1(float x, float y, float z) {
    return min(x, min(y, z));
}

float ARcpF1(float x) {
    return AF1_x(float(1.0)) / x;
}

float ASatF1(float x) {
    return clamp(x, AF1_x(float(0.0)), AF1_x(float(1.0)));
}
float APrxMedRcpF1(float a) {
    float b = uintBitsToFloat(uint(AU1_x(uint(0x7ef19fff)) - floatBitsToUint(float(a))));
    return b * (-b * a + AF1_x(float(2.0)));
}

uvec2 ARmp8x8(uint a) {
    return uvec2(ABfe(a, 1u, 3u), ABfiM(ABfe(a, 3u, 3u), a, 1u));
}

vec4 FsrRcasLoadF(ivec2 p) {
    return vec4(imageLoad(inImage, ivec2(p)));
}
void FsrRcasInputF(inout float r, inout float g, inout float b) {}

void FsrRcasF(
out float pixR,
out float pixG,
out float pixB,

    uvec2 ip,
    uvec4 con) {
    ivec2 sp = ivec2(ip);
    vec3 b = FsrRcasLoadF(sp + ivec2(0, -1)).rgb;
    vec3 d = FsrRcasLoadF(sp + ivec2(-1, 0)).rgb;

    vec3 e = FsrRcasLoadF(sp).rgb;

    vec3 f = FsrRcasLoadF(sp + ivec2(1, 0)).rgb;
    vec3 h = FsrRcasLoadF(sp + ivec2(0, 1)).rgb;

    float bR = b.r;
    float bG = b.g;
    float bB = b.b;
    float dR = d.r;
    float dG = d.g;
    float dB = d.b;
    float eR = e.r;
    float eG = e.g;
    float eB = e.b;
    float fR = f.r;
    float fG = f.g;
    float fB = f.b;
    float hR = h.r;
    float hG = h.g;
    float hB = h.b;

    FsrRcasInputF(bR, bG, bB);
    FsrRcasInputF(dR, dG, dB);
    FsrRcasInputF(eR, eG, eB);
    FsrRcasInputF(fR, fG, fB);
    FsrRcasInputF(hR, hG, hB);

    float bL = bB * AF1_x(float(0.5)) + (bR * AF1_x(float(0.5)) + bG);
    float dL = dB * AF1_x(float(0.5)) + (dR * AF1_x(float(0.5)) + dG);
    float eL = eB * AF1_x(float(0.5)) + (eR * AF1_x(float(0.5)) + eG);
    float fL = fB * AF1_x(float(0.5)) + (fR * AF1_x(float(0.5)) + fG);
    float hL = hB * AF1_x(float(0.5)) + (hR * AF1_x(float(0.5)) + hG);

    float nz = AF1_x(float(0.25)) * bL + AF1_x(float(0.25)) * dL + AF1_x(float(0.25)) * fL + AF1_x(float(0.25)) * hL - eL;
    nz = ASatF1(abs(nz) * APrxMedRcpF1(AMax3F1(AMax3F1(bL, dL, eL), fL, hL) - AMin3F1(AMin3F1(bL, dL, eL), fL, hL)));
    nz = AF1_x(float(-0.5)) * nz + AF1_x(float(1.0));

    float mn4R = min(AMin3F1(bR, dR, fR), hR);
    float mn4G = min(AMin3F1(bG, dG, fG), hG);
    float mn4B = min(AMin3F1(bB, dB, fB), hB);
    float mx4R = max(AMax3F1(bR, dR, fR), hR);
    float mx4G = max(AMax3F1(bG, dG, fG), hG);
    float mx4B = max(AMax3F1(bB, dB, fB), hB);

    vec2 peakC = vec2(1.0, -1.0 * 4.0);

    float hitMinR = min(mn4R, eR) * ARcpF1(AF1_x(float(4.0)) * mx4R);
    float hitMinG = min(mn4G, eG) * ARcpF1(AF1_x(float(4.0)) * mx4G);
    float hitMinB = min(mn4B, eB) * ARcpF1(AF1_x(float(4.0)) * mx4B);
    float hitMaxR = (peakC.x - max(mx4R, eR)) * ARcpF1(AF1_x(float(4.0)) * mn4R + peakC.y);
    float hitMaxG = (peakC.x - max(mx4G, eG)) * ARcpF1(AF1_x(float(4.0)) * mn4G + peakC.y);
    float hitMaxB = (peakC.x - max(mx4B, eB)) * ARcpF1(AF1_x(float(4.0)) * mn4B + peakC.y);
    float lobeR = max(-hitMinR, hitMaxR);
    float lobeG = max(-hitMinG, hitMaxG);
    float lobeB = max(-hitMinB, hitMaxB);
    float lobe = max(AF1_x(float(-(0.25 - (1.0 / 16.0)))), min(AMax3F1(lobeR, lobeG, lobeB), AF1_x(float(0.0)))) * uintBitsToFloat(uint(con.x));

    float rcpL = APrxMedRcpF1(AF1_x(float(4.0)) * lobe + AF1_x(float(1.0)));
    pixR = (lobe * bR + lobe * dR + lobe * hR + lobe * fR + eR) * rcpL;
    pixG = (lobe * bG + lobe * dG + lobe * hG + lobe * fG + eG) * rcpL;
    pixB = (lobe * bB + lobe * dB + lobe * hB + lobe * fB + eB) * rcpL;
    return;
}