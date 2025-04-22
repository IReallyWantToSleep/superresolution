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

uint ABfe(uint src, uint off, uint bits) {
    return bitfieldExtract(src, int(off), int(bits));
}

uint ABfiM(uint src, uint ins, uint bits) {
    return bitfieldInsert(src, ins, 0, int(bits));
}

float ARcpF1(float x) {
    return AF1_x(float(1.0)) / x;
}

float16_t AH1_x(float16_t a) {
    return float16_t(a);
}
f16vec2 AH2_x(float16_t a) {
    return f16vec2(a, a);
}
f16vec3 AH3_x(float16_t a) {
    return f16vec3(a, a, a);
}
f16vec4 AH4_x(float16_t a) {
    return f16vec4(a, a, a, a);
}

uint16_t AW1_x(uint16_t a) {
    return uint16_t(a);
}

float16_t ARcpH1(float16_t x) {
    return AH1_x(float16_t(1.0)) / x;
}
f16vec2 ARcpH2(f16vec2 x) {
    return AH2_x(float16_t(1.0)) / x;
}
f16vec2 ASatH2(f16vec2 x) {
    return clamp(x, AH2_x(float16_t(0.0)), AH2_x(float16_t(1.0)));
}

float16_t APrxLoRcpH1(float16_t a) {
    return uint16BitsToHalf(uint16_t(AW1_x(uint16_t(0x7784)) - halfBitsToUint16(float16_t(a))));
}

float16_t APrxLoRsqH1(float16_t a) {
    return uint16BitsToHalf(uint16_t(AW1_x(uint16_t(0x59a3)) - (halfBitsToUint16(float16_t(a)) >> AW1_x(uint16_t(1)))));
}

uvec2 ARmp8x8(uint a) {
    return uvec2(ABfe(a, 1u, 3u), ABfiM(ABfe(a, 3u, 3u), a, 1u));
}

f16vec4 FsrEasuRH(vec2 p) {
    return f16vec4(textureGather(inImage, p, 0));
}
f16vec4 FsrEasuGH(vec2 p) {
    return f16vec4(textureGather(inImage, p, 1));
}
f16vec4 FsrEasuBH(vec2 p) {
    return f16vec4(textureGather(inImage, p, 2));
}

void FsrEasuTapH(
inout f16vec2 aCR, inout f16vec2 aCG, inout f16vec2 aCB,
inout f16vec2 aW,
      f16vec2 offX, f16vec2 offY,
      f16vec2 dir,
      f16vec2 len,
      float16_t lob,
      float16_t clp,
      f16vec2 cR, f16vec2 cG, f16vec2 cB) {
    f16vec2 vX, vY;
    vX = offX * dir.xx + offY * dir.yy;
    vY = offX * (-dir.yy) + offY * dir.xx;
    vX *= len.x;
    vY *= len.y;
    f16vec2 d2 = vX * vX + vY * vY;
    d2 = min(d2, AH2_x(float16_t(clp)));
    f16vec2 wB = AH2_x(float16_t(2.0 / 5.0)) * d2 + AH2_x(float16_t(-1.0));
    f16vec2 wA = AH2_x(float16_t(lob)) * d2 + AH2_x(float16_t(-1.0));
    wB *= wB;
    wA *= wA;
    wB = AH2_x(float16_t(25.0 / 16.0)) * wB + AH2_x(float16_t(-(25.0 / 16.0 - 1.0)));
    f16vec2 w = wB * wA;
    aCR += cR * w;
    aCG += cG * w;
    aCB += cB * w;
    aW += w;
}

void FsrEasuSetH(
inout f16vec2 dirPX, inout f16vec2 dirPY,
inout f16vec2 lenP,
      f16vec2 pp,
      bool biST, bool biUV,
      f16vec2 lA, f16vec2 lB, f16vec2 lC, f16vec2 lD, f16vec2 lE) {
    f16vec2 w = AH2_x(float16_t(0.0));
    if (biST) w = (f16vec2(1.0, 0.0) + f16vec2(-pp.x, pp.x)) * AH2_x(float16_t(AH1_x(float16_t(1.0)) - pp.y));
    if (biUV) w = (f16vec2(1.0, 0.0) + f16vec2(-pp.x, pp.x)) * AH2_x(float16_t(pp.y));

    f16vec2 dc = lD - lC;
    f16vec2 cb = lC - lB;
    f16vec2 lenX = max(abs(dc), abs(cb));
    lenX = ARcpH2(lenX);
    f16vec2 dirX = lD - lB;
    dirPX += dirX * w;
    lenX = ASatH2(abs(dirX) * lenX);
    lenX *= lenX;
    lenP += lenX * w;
    f16vec2 ec = lE - lC;
    f16vec2 ca = lC - lA;
    f16vec2 lenY = max(abs(ec), abs(ca));
    lenY = ARcpH2(lenY);
    f16vec2 dirY = lE - lA;
    dirPY += dirY * w;
    lenY = ASatH2(abs(dirY) * lenY);
    lenY *= lenY;
    lenP += lenY * w;
}

void FsrEasuH(
out f16vec3 pix,
    uvec2 ip,
    uvec4 con0,
    uvec4 con1,
    uvec4 con2,
    uvec4 con3) {
    vec2 pp = vec2(ip) * uintBitsToFloat(uvec2(con0.xy)) + uintBitsToFloat(uvec2(con0.zw));
    vec2 fp = floor(pp);
    pp -= fp;
    f16vec2 ppp = f16vec2(pp);

    vec2 p0 = fp * uintBitsToFloat(uvec2(con1.xy)) + uintBitsToFloat(uvec2(con1.zw));
    vec2 p1 = p0 + uintBitsToFloat(uvec2(con2.xy));
    vec2 p2 = p0 + uintBitsToFloat(uvec2(con2.zw));
    vec2 p3 = p0 + uintBitsToFloat(uvec2(con3.xy));
    f16vec4 bczzR = FsrEasuRH(p0);
    f16vec4 bczzG = FsrEasuGH(p0);
    f16vec4 bczzB = FsrEasuBH(p0);
    f16vec4 ijfeR = FsrEasuRH(p1);
    f16vec4 ijfeG = FsrEasuGH(p1);
    f16vec4 ijfeB = FsrEasuBH(p1);
    f16vec4 klhgR = FsrEasuRH(p2);
    f16vec4 klhgG = FsrEasuGH(p2);
    f16vec4 klhgB = FsrEasuBH(p2);
    f16vec4 zzonR = FsrEasuRH(p3);
    f16vec4 zzonG = FsrEasuGH(p3);
    f16vec4 zzonB = FsrEasuBH(p3);

    f16vec4 bczzL = bczzB * AH4_x(float16_t(0.5)) + (bczzR * AH4_x(float16_t(0.5)) + bczzG);
    f16vec4 ijfeL = ijfeB * AH4_x(float16_t(0.5)) + (ijfeR * AH4_x(float16_t(0.5)) + ijfeG);
    f16vec4 klhgL = klhgB * AH4_x(float16_t(0.5)) + (klhgR * AH4_x(float16_t(0.5)) + klhgG);
    f16vec4 zzonL = zzonB * AH4_x(float16_t(0.5)) + (zzonR * AH4_x(float16_t(0.5)) + zzonG);
    float16_t bL = bczzL.x;
    float16_t cL = bczzL.y;
    float16_t iL = ijfeL.x;
    float16_t jL = ijfeL.y;
    float16_t fL = ijfeL.z;
    float16_t eL = ijfeL.w;
    float16_t kL = klhgL.x;
    float16_t lL = klhgL.y;
    float16_t hL = klhgL.z;
    float16_t gL = klhgL.w;
    float16_t oL = zzonL.z;
    float16_t nL = zzonL.w;

    f16vec2 dirPX = AH2_x(float16_t(0.0));
    f16vec2 dirPY = AH2_x(float16_t(0.0));
    f16vec2 lenP = AH2_x(float16_t(0.0));
    FsrEasuSetH(dirPX, dirPY, lenP, ppp, true, false, f16vec2(bL, cL), f16vec2(eL, fL), f16vec2(fL, gL), f16vec2(gL, hL), f16vec2(jL, kL));
    FsrEasuSetH(dirPX, dirPY, lenP, ppp, false, true, f16vec2(fL, gL), f16vec2(iL, jL), f16vec2(jL, kL), f16vec2(kL, lL), f16vec2(nL, oL));
    f16vec2 dir = f16vec2(dirPX.r + dirPX.g, dirPY.r + dirPY.g);
    float16_t len = lenP.r + lenP.g;

    f16vec2 dir2 = dir * dir;
    float16_t dirR = dir2.x + dir2.y;
    bool zro = dirR < AH1_x(float16_t(1.0 / 32768.0));
    dirR = APrxLoRsqH1(dirR);
    dirR = zro ? AH1_x(float16_t(1.0)) : dirR;
    dir.x = zro ? AH1_x(float16_t(1.0)) : dir.x;
    dir *= AH2_x(float16_t(dirR));
    len = len * AH1_x(float16_t(0.5));
    len *= len;
    float16_t stretch = (dir.x * dir.x + dir.y * dir.y) * APrxLoRcpH1(max(abs(dir.x), abs(dir.y)));
    f16vec2 len2 = f16vec2(AH1_x(float16_t(1.0)) + (stretch - AH1_x(float16_t(1.0))) * len, AH1_x(float16_t(1.0)) + AH1_x(float16_t(-0.5)) * len);
    float16_t lob = AH1_x(float16_t(0.5)) + AH1_x(float16_t((1.0 / 4.0 - 0.04) - 0.5)) * len;
    float16_t clp = APrxLoRcpH1(lob);

    f16vec2 bothR = max(max(f16vec2(-ijfeR.z, ijfeR.z), f16vec2(-klhgR.w, klhgR.w)), max(f16vec2(-ijfeR.y, ijfeR.y), f16vec2(-klhgR.x, klhgR.x)));
    f16vec2 bothG = max(max(f16vec2(-ijfeG.z, ijfeG.z), f16vec2(-klhgG.w, klhgG.w)), max(f16vec2(-ijfeG.y, ijfeG.y), f16vec2(-klhgG.x, klhgG.x)));
    f16vec2 bothB = max(max(f16vec2(-ijfeB.z, ijfeB.z), f16vec2(-klhgB.w, klhgB.w)), max(f16vec2(-ijfeB.y, ijfeB.y), f16vec2(-klhgB.x, klhgB.x)));

    f16vec2 pR = AH2_x(float16_t(0.0));
    f16vec2 pG = AH2_x(float16_t(0.0));
    f16vec2 pB = AH2_x(float16_t(0.0));
    f16vec2 pW = AH2_x(float16_t(0.0));
    FsrEasuTapH(pR, pG, pB, pW, f16vec2(0.0, 1.0) - ppp.xx, f16vec2(-1.0, -1.0) - ppp.yy, dir, len2, lob, clp, bczzR.xy, bczzG.xy, bczzB.xy);
    FsrEasuTapH(pR, pG, pB, pW, f16vec2(-1.0, 0.0) - ppp.xx, f16vec2(1.0, 1.0) - ppp.yy, dir, len2, lob, clp, ijfeR.xy, ijfeG.xy, ijfeB.xy);
    FsrEasuTapH(pR, pG, pB, pW, f16vec2(0.0, -1.0) - ppp.xx, f16vec2(0.0, 0.0) - ppp.yy, dir, len2, lob, clp, ijfeR.zw, ijfeG.zw, ijfeB.zw);
    FsrEasuTapH(pR, pG, pB, pW, f16vec2(1.0, 2.0) - ppp.xx, f16vec2(1.0, 1.0) - ppp.yy, dir, len2, lob, clp, klhgR.xy, klhgG.xy, klhgB.xy);
    FsrEasuTapH(pR, pG, pB, pW, f16vec2(2.0, 1.0) - ppp.xx, f16vec2(0.0, 0.0) - ppp.yy, dir, len2, lob, clp, klhgR.zw, klhgG.zw, klhgB.zw);
    FsrEasuTapH(pR, pG, pB, pW, f16vec2(1.0, 0.0) - ppp.xx, f16vec2(2.0, 2.0) - ppp.yy, dir, len2, lob, clp, zzonR.zw, zzonG.zw, zzonB.zw);
    f16vec3 aC = f16vec3(pR.x + pR.y, pG.x + pG.y, pB.x + pB.y);
    float16_t aW = pW.x + pW.y;

    pix = min(f16vec3(bothR.y, bothG.y, bothB.y), max(- f16vec3(bothR.x, bothG.x, bothB.x), aC * AH3_x(float16_t(ARcpH1(aW)))));
}
