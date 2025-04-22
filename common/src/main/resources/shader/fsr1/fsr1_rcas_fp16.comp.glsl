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

uint ABfe(uint src, uint off, uint bits) {
    return bitfieldExtract(src, int(off), int(bits));
}

uint ABfiM(uint src, uint ins, uint bits) {
    return bitfieldInsert(src, ins, 0, int(bits));
}

float16_t AH1_x(float16_t a) {
    return float16_t(a);
}

uint16_t AW1_x(uint16_t a) {
    return uint16_t(a);
}

float16_t AMax3H1(float16_t x, float16_t y, float16_t z) {
    return max(x, max(y, z));
}

float16_t AMin3H1(float16_t x, float16_t y, float16_t z) {
    return min(x, min(y, z));
}

float16_t ARcpH1(float16_t x) {
    return AH1_x(float16_t(1.0)) / x;
}

float16_t ASatH1(float16_t x) {
    return clamp(x, AH1_x(float16_t(0.0)), AH1_x(float16_t(1.0)));
}

float16_t APrxMedRcpH1(float16_t a) {
    float16_t b = uint16BitsToHalf(uint16_t(AW1_x(uint16_t(0x778d)) - halfBitsToUint16(float16_t(a))));
    return b * (-b * a + AH1_x(float16_t(2.0)));
}

uvec2 ARmp8x8(uint a) {
    return uvec2(ABfe(a, 1u, 3u), ABfiM(ABfe(a, 3u, 3u), a, 1u));
}

f16vec4 FsrRcasLoadH(i16vec2 p) {
    return f16vec4(imageLoad(inImage, ivec2(p)));
}
void FsrRcasInputH(inout float16_t r, inout float16_t g, inout float16_t b) {}

void FsrRcasH(
out float16_t pixR,
out float16_t pixG,
out float16_t pixB,

    uvec2 ip,
    uvec4 con) {
    i16vec2 sp = i16vec2(ip);
    f16vec3 b = FsrRcasLoadH(sp + i16vec2(0, -1)).rgb;
    f16vec3 d = FsrRcasLoadH(sp + i16vec2(-1, 0)).rgb;

    f16vec3 e = FsrRcasLoadH(sp).rgb;

    f16vec3 f = FsrRcasLoadH(sp + i16vec2(1, 0)).rgb;
    f16vec3 h = FsrRcasLoadH(sp + i16vec2(0, 1)).rgb;

    float16_t bR = b.r;
    float16_t bG = b.g;
    float16_t bB = b.b;
    float16_t dR = d.r;
    float16_t dG = d.g;
    float16_t dB = d.b;
    float16_t eR = e.r;
    float16_t eG = e.g;
    float16_t eB = e.b;
    float16_t fR = f.r;
    float16_t fG = f.g;
    float16_t fB = f.b;
    float16_t hR = h.r;
    float16_t hG = h.g;
    float16_t hB = h.b;

    FsrRcasInputH(bR, bG, bB);
    FsrRcasInputH(dR, dG, dB);
    FsrRcasInputH(eR, eG, eB);
    FsrRcasInputH(fR, fG, fB);
    FsrRcasInputH(hR, hG, hB);

    float16_t bL = bB * AH1_x(float16_t(0.5)) + (bR * AH1_x(float16_t(0.5)) + bG);
    float16_t dL = dB * AH1_x(float16_t(0.5)) + (dR * AH1_x(float16_t(0.5)) + dG);
    float16_t eL = eB * AH1_x(float16_t(0.5)) + (eR * AH1_x(float16_t(0.5)) + eG);
    float16_t fL = fB * AH1_x(float16_t(0.5)) + (fR * AH1_x(float16_t(0.5)) + fG);
    float16_t hL = hB * AH1_x(float16_t(0.5)) + (hR * AH1_x(float16_t(0.5)) + hG);

    float16_t nz = AH1_x(float16_t(0.25)) * bL + AH1_x(float16_t(0.25)) * dL + AH1_x(float16_t(0.25)) * fL + AH1_x(float16_t(0.25)) * hL - eL;
    nz = ASatH1(abs(nz) * APrxMedRcpH1(AMax3H1(AMax3H1(bL, dL, eL), fL, hL) - AMin3H1(AMin3H1(bL, dL, eL), fL, hL)));
    nz = AH1_x(float16_t(-0.5)) * nz + AH1_x(float16_t(1.0));

    float16_t mn4R = min(AMin3H1(bR, dR, fR), hR);
    float16_t mn4G = min(AMin3H1(bG, dG, fG), hG);
    float16_t mn4B = min(AMin3H1(bB, dB, fB), hB);
    float16_t mx4R = max(AMax3H1(bR, dR, fR), hR);
    float16_t mx4G = max(AMax3H1(bG, dG, fG), hG);
    float16_t mx4B = max(AMax3H1(bB, dB, fB), hB);

    f16vec2 peakC = f16vec2(1.0, -1.0 * 4.0);

    float16_t hitMinR = min(mn4R, eR) * ARcpH1(AH1_x(float16_t(4.0)) * mx4R);
    float16_t hitMinG = min(mn4G, eG) * ARcpH1(AH1_x(float16_t(4.0)) * mx4G);
    float16_t hitMinB = min(mn4B, eB) * ARcpH1(AH1_x(float16_t(4.0)) * mx4B);
    float16_t hitMaxR = (peakC.x - max(mx4R, eR)) * ARcpH1(AH1_x(float16_t(4.0)) * mn4R + peakC.y);
    float16_t hitMaxG = (peakC.x - max(mx4G, eG)) * ARcpH1(AH1_x(float16_t(4.0)) * mn4G + peakC.y);
    float16_t hitMaxB = (peakC.x - max(mx4B, eB)) * ARcpH1(AH1_x(float16_t(4.0)) * mn4B + peakC.y);
    float16_t lobeR = max(-hitMinR, hitMaxR);
    float16_t lobeG = max(-hitMinG, hitMaxG);
    float16_t lobeB = max(-hitMinB, hitMaxB);
    float16_t lobe = max(AH1_x(float16_t(-(0.25 - (1.0 / 16.0)))), min(AMax3H1(lobeR, lobeG, lobeB), AH1_x(float16_t(0.0)))) * unpackFloat2x16(uint(con.y)).x;

    float16_t rcpL = APrxMedRcpH1(AH1_x(float16_t(4.0)) * lobe + AH1_x(float16_t(1.0)));
    pixR = (lobe * bR + lobe * dR + lobe * hR + lobe * fR + eR) * rcpL;
    pixG = (lobe * bG + lobe * dG + lobe * hG + lobe * fG + eG) * rcpL;
    pixB = (lobe * bB + lobe * dB + lobe * hB + lobe * fB + eB) * rcpL;
}
