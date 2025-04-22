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
vec2 AF2_x(float a) {
    return vec2(a, a);
}
vec3 AF3_x(float a) {
    return vec3(a, a, a);
}
vec4 AF4_x(float a) {
    return vec4(a, a, a, a);
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
vec3 AMax3F3(vec3 x, vec3 y, vec3 z) {
    return max(x, max(y, z));
}
vec3 AMin3F3(vec3 x, vec3 y, vec3 z) {
    return min(x, min(y, z));
}

float ARcpF1(float x) {
    return AF1_x(float(1.0)) / x;
}

float ASatF1(float x) {
    return clamp(x, AF1_x(float(0.0)), AF1_x(float(1.0)));
}
float APrxLoRcpF1(float a) {
    return uintBitsToFloat(uint(AU1_x(uint(0x7ef07ebb)) - floatBitsToUint(float(a))));
}
float APrxLoRsqF1(float a) {
    return uintBitsToFloat(uint(AU1_x(uint(0x5f347d74)) - (floatBitsToUint(float(a)) >> AU1_x(uint(1)))));
}

uvec2 ARmp8x8(uint a) {
    return uvec2(ABfe(a, 1u, 3u), ABfiM(ABfe(a, 3u, 3u), a, 1u));
}

vec4 FsrEasuRF(vec2 p) {
    return textureGather(inImage, p, 0);
}
vec4 FsrEasuGF(vec2 p) {
    return textureGather(inImage, p, 1);
}
vec4 FsrEasuBF(vec2 p) {
    return textureGather(inImage, p, 2);
}

void FsrEasuTapF(
inout vec3 aC,
inout float aW,
      vec2 off,
      vec2 dir,
      vec2 len,
      float lob,
      float clp,
      vec3 c) {
    vec2 v;
    v.x = (off.x * (dir.x)) + (off.y * dir.y);
    v.y = (off.x * (-dir.y)) + (off.y * dir.x);

    v *= len;

    float d2 = v.x * v.x + v.y * v.y;

    d2 = min(d2, clp);

    float wB = AF1_x(float(2.0 / 5.0)) * d2 + AF1_x(float(-1.0));
    float wA = lob * d2 + AF1_x(float(-1.0));
    wB *= wB;
    wA *= wA;
    wB = AF1_x(float(25.0 / 16.0)) * wB + AF1_x(float(-(25.0 / 16.0 - 1.0)));
    float w = wB * wA;

    aC += c * w;
    aW += w;
}

void FsrEasuSetF(
inout vec2 dir,
inout float len,
      vec2 pp,
      bool biS, bool biT, bool biU, bool biV,
      float lA, float lB, float lC, float lD, float lE) {
    float w = AF1_x(float(0.0));
    if (biS) w = (AF1_x(float(1.0)) - pp.x) * (AF1_x(float(1.0)) - pp.y);
    if (biT) w = pp.x * (AF1_x(float(1.0)) - pp.y);
    if (biU) w = (AF1_x(float(1.0)) - pp.x) * pp.y;
    if (biV) w = pp.x * pp.y;

    float dc = lD - lC;
    float cb = lC - lB;
    float lenX = max(abs(dc), abs(cb));
    lenX = APrxLoRcpF1(lenX);
    float dirX = lD - lB;
    dir.x += dirX * w;
    lenX = ASatF1(abs(dirX) * lenX);
    lenX *= lenX;
    len += lenX * w;

    float ec = lE - lC;
    float ca = lC - lA;
    float lenY = max(abs(ec), abs(ca));
    lenY = APrxLoRcpF1(lenY);
    float dirY = lE - lA;
    dir.y += dirY * w;
    lenY = ASatF1(abs(dirY) * lenY);
    lenY *= lenY;
    len += lenY * w;
}

void FsrEasuF(
out vec3 pix,
    uvec2 ip,
    uvec4 con0,
    uvec4 con1,
    uvec4 con2,
    uvec4 con3) {
    vec2 pp = vec2(ip) * uintBitsToFloat(uvec2(con0.xy)) + uintBitsToFloat(uvec2(con0.zw));
    vec2 fp = floor(pp);
    pp -= fp;

    vec2 p0 = fp * uintBitsToFloat(uvec2(con1.xy)) + uintBitsToFloat(uvec2(con1.zw));

    vec2 p1 = p0 + uintBitsToFloat(uvec2(con2.xy));
    vec2 p2 = p0 + uintBitsToFloat(uvec2(con2.zw));
    vec2 p3 = p0 + uintBitsToFloat(uvec2(con3.xy));
    vec4 bczzR = FsrEasuRF(p0);
    vec4 bczzG = FsrEasuGF(p0);
    vec4 bczzB = FsrEasuBF(p0);
    vec4 ijfeR = FsrEasuRF(p1);
    vec4 ijfeG = FsrEasuGF(p1);
    vec4 ijfeB = FsrEasuBF(p1);
    vec4 klhgR = FsrEasuRF(p2);
    vec4 klhgG = FsrEasuGF(p2);
    vec4 klhgB = FsrEasuBF(p2);
    vec4 zzonR = FsrEasuRF(p3);
    vec4 zzonG = FsrEasuGF(p3);
    vec4 zzonB = FsrEasuBF(p3);

    vec4 bczzL = bczzB * AF4_x(float(0.5)) + (bczzR * AF4_x(float(0.5)) + bczzG);
    vec4 ijfeL = ijfeB * AF4_x(float(0.5)) + (ijfeR * AF4_x(float(0.5)) + ijfeG);
    vec4 klhgL = klhgB * AF4_x(float(0.5)) + (klhgR * AF4_x(float(0.5)) + klhgG);
    vec4 zzonL = zzonB * AF4_x(float(0.5)) + (zzonR * AF4_x(float(0.5)) + zzonG);

    float bL = bczzL.x;
    float cL = bczzL.y;
    float iL = ijfeL.x;
    float jL = ijfeL.y;
    float fL = ijfeL.z;
    float eL = ijfeL.w;
    float kL = klhgL.x;
    float lL = klhgL.y;
    float hL = klhgL.z;
    float gL = klhgL.w;
    float oL = zzonL.z;
    float nL = zzonL.w;

    vec2 dir = AF2_x(float(0.0));
    float len = AF1_x(float(0.0));
    FsrEasuSetF(dir, len, pp, true, false, false, false, bL, eL, fL, gL, jL);
    FsrEasuSetF(dir, len, pp, false, true, false, false, cL, fL, gL, hL, kL);
    FsrEasuSetF(dir, len, pp, false, false, true, false, fL, iL, jL, kL, nL);
    FsrEasuSetF(dir, len, pp, false, false, false, true, gL, jL, kL, lL, oL);

    vec2 dir2 = dir * dir;
    float dirR = dir2.x + dir2.y;
    bool zro = dirR < AF1_x(float(1.0 / 32768.0));
    dirR = APrxLoRsqF1(dirR);
    dirR = zro ? AF1_x(float(1.0)) : dirR;
    dir.x = zro ? AF1_x(float(1.0)) : dir.x;
    dir *= AF2_x(float(dirR));

    len = len * AF1_x(float(0.5));
    len *= len;

    float stretch = (dir.x * dir.x + dir.y * dir.y) * APrxLoRcpF1(max(abs(dir.x), abs(dir.y)));

    vec2 len2 = vec2(AF1_x(float(1.0)) + (stretch - AF1_x(float(1.0))) * len, AF1_x(float(1.0)) + AF1_x(float(-0.5)) * len);

    float lob = AF1_x(float(0.5)) + AF1_x(float((1.0 / 4.0 - 0.04) - 0.5)) * len;

    float clp = APrxLoRcpF1(lob);

    vec3 min4 = min(AMin3F3(vec3(ijfeR.z, ijfeG.z, ijfeB.z), vec3(klhgR.w, klhgG.w, klhgB.w), vec3(ijfeR.y, ijfeG.y, ijfeB.y)),
                    vec3(klhgR.x, klhgG.x, klhgB.x));
    vec3 max4 = max(AMax3F3(vec3(ijfeR.z, ijfeG.z, ijfeB.z), vec3(klhgR.w, klhgG.w, klhgB.w), vec3(ijfeR.y, ijfeG.y, ijfeB.y)),
                    vec3(klhgR.x, klhgG.x, klhgB.x));

    vec3 aC = AF3_x(float(0.0));
    float aW = AF1_x(float(0.0));
    FsrEasuTapF(aC, aW, vec2(0.0, -1.0) - pp, dir, len2, lob, clp, vec3(bczzR.x, bczzG.x, bczzB.x));
    FsrEasuTapF(aC, aW, vec2(1.0, -1.0) - pp, dir, len2, lob, clp, vec3(bczzR.y, bczzG.y, bczzB.y));
    FsrEasuTapF(aC, aW, vec2(-1.0, 1.0) - pp, dir, len2, lob, clp, vec3(ijfeR.x, ijfeG.x, ijfeB.x));
    FsrEasuTapF(aC, aW, vec2(0.0, 1.0) - pp, dir, len2, lob, clp, vec3(ijfeR.y, ijfeG.y, ijfeB.y));
    FsrEasuTapF(aC, aW, vec2(0.0, 0.0) - pp, dir, len2, lob, clp, vec3(ijfeR.z, ijfeG.z, ijfeB.z));
    FsrEasuTapF(aC, aW, vec2(-1.0, 0.0) - pp, dir, len2, lob, clp, vec3(ijfeR.w, ijfeG.w, ijfeB.w));
    FsrEasuTapF(aC, aW, vec2(1.0, 1.0) - pp, dir, len2, lob, clp, vec3(klhgR.x, klhgG.x, klhgB.x));
    FsrEasuTapF(aC, aW, vec2(2.0, 1.0) - pp, dir, len2, lob, clp, vec3(klhgR.y, klhgG.y, klhgB.y));
    FsrEasuTapF(aC, aW, vec2(2.0, 0.0) - pp, dir, len2, lob, clp, vec3(klhgR.z, klhgG.z, klhgB.z));
    FsrEasuTapF(aC, aW, vec2(1.0, 0.0) - pp, dir, len2, lob, clp, vec3(klhgR.w, klhgG.w, klhgB.w));
    FsrEasuTapF(aC, aW, vec2(1.0, 2.0) - pp, dir, len2, lob, clp, vec3(zzonR.z, zzonG.z, zzonB.z));
    FsrEasuTapF(aC, aW, vec2(0.0, 2.0) - pp, dir, len2, lob, clp, vec3(zzonR.w, zzonG.w, zzonB.w));

    pix = min(max4, max(min4, aC * AF3_x(float(ARcpF1(aW)))));
}