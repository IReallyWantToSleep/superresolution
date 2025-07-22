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

#if FFX_HALF
#if FFX_HLSL_6_2
#define FFX_POSITIVE_INFINITY_HALF FFX_TO_FLOAT16((uint16_t)0x7c00u)

#define FFX_NEGATIVE_INFINITY_HALF FFX_TO_FLOAT16((uint16_t)0xfc00u)
#else
#define FFX_POSITIVE_INFINITY_HALF FFX_TO_FLOAT16(0x7c00u)

#define FFX_NEGATIVE_INFINITY_HALF FFX_TO_FLOAT16(0xfc00u)
#endif // FFX_HLSL_6_2

FfxFloat16 ffxMin(FfxFloat16 x, FfxFloat16 y)
{
    return min(x, y);
}

FfxFloat16x2 ffxMin(FfxFloat16x2 x, FfxFloat16x2 y)
{
    return min(x, y);
}

FfxFloat16x3 ffxMin(FfxFloat16x3 x, FfxFloat16x3 y)
{
    return min(x, y);
}

FfxFloat16x4 ffxMin(FfxFloat16x4 x, FfxFloat16x4 y)
{
    return min(x, y);
}

FfxInt16 ffxMin(FfxInt16 x, FfxInt16 y)
{
    return min(x, y);
}

FfxInt16x2 ffxMin(FfxInt16x2 x, FfxInt16x2 y)
{
    return min(x, y);
}

FfxInt16x3 ffxMin(FfxInt16x3 x, FfxInt16x3 y)
{
    return min(x, y);
}

FfxInt16x4 ffxMin(FfxInt16x4 x, FfxInt16x4 y)
{
    return min(x, y);
}

FfxUInt16 ffxMin(FfxUInt16 x, FfxUInt16 y)
{
    return min(x, y);
}

FfxUInt16x2 ffxMin(FfxUInt16x2 x, FfxUInt16x2 y)
{
    return min(x, y);
}

FfxUInt16x3 ffxMin(FfxUInt16x3 x, FfxUInt16x3 y)
{
    return min(x, y);
}

FfxUInt16x4 ffxMin(FfxUInt16x4 x, FfxUInt16x4 y)
{
    return min(x, y);
}

FfxFloat16 ffxMax(FfxFloat16 x, FfxFloat16 y)
{
    return max(x, y);
}

FfxFloat16x2 ffxMax(FfxFloat16x2 x, FfxFloat16x2 y)
{
    return max(x, y);
}

FfxFloat16x3 ffxMax(FfxFloat16x3 x, FfxFloat16x3 y)
{
    return max(x, y);
}

FfxFloat16x4 ffxMax(FfxFloat16x4 x, FfxFloat16x4 y)
{
    return max(x, y);
}

FfxInt16 ffxMax(FfxInt16 x, FfxInt16 y)
{
    return max(x, y);
}

FfxInt16x2 ffxMax(FfxInt16x2 x, FfxInt16x2 y)
{
    return max(x, y);
}

FfxInt16x3 ffxMax(FfxInt16x3 x, FfxInt16x3 y)
{
    return max(x, y);
}

FfxInt16x4 ffxMax(FfxInt16x4 x, FfxInt16x4 y)
{
    return max(x, y);
}

FfxUInt16 ffxMax(FfxUInt16 x, FfxUInt16 y)
{
    return max(x, y);
}

FfxUInt16x2 ffxMax(FfxUInt16x2 x, FfxUInt16x2 y)
{
    return max(x, y);
}

FfxUInt16x3 ffxMax(FfxUInt16x3 x, FfxUInt16x3 y)
{
    return max(x, y);
}

FfxUInt16x4 ffxMax(FfxUInt16x4 x, FfxUInt16x4 y)
{
    return max(x, y);
}

FfxFloat16 ffxPow(FfxFloat16 x, FfxFloat16 y)
{
    return pow(x, y);
}

FfxFloat16x2 ffxPow(FfxFloat16x2 x, FfxFloat16x2 y)
{
    return pow(x, y);
}

FfxFloat16x3 ffxPow(FfxFloat16x3 x, FfxFloat16x3 y)
{
    return pow(x, y);
}

FfxFloat16x4 ffxPow(FfxFloat16x4 x, FfxFloat16x4 y)
{
    return pow(x, y);
}

FfxFloat16 ffxSqrt(FfxFloat16 x)
{
    return sqrt(x);
}

FfxFloat16x2 ffxSqrt(FfxFloat16x2 x)
{
    return sqrt(x);
}

FfxFloat16x3 ffxSqrt(FfxFloat16x3 x)
{
    return sqrt(x);
}

FfxFloat16x4 ffxSqrt(FfxFloat16x4 x)
{
    return sqrt(x);
}

FfxFloat16 ffxCopySignBitHalf(FfxFloat16 d, FfxFloat16 s)
{
    return FFX_TO_FLOAT16(FFX_TO_UINT16(d) | (FFX_TO_UINT16(s) & FFX_BROADCAST_UINT16(0x8000u)));
}

FfxFloat16x2 ffxCopySignBitHalf(FfxFloat16x2 d, FfxFloat16x2 s)
{
    return FFX_TO_FLOAT16X2(FFX_TO_UINT16X2(d) | (FFX_TO_UINT16X2(s) & FFX_BROADCAST_UINT16X2(0x8000u)));
}

FfxFloat16x3 ffxCopySignBitHalf(FfxFloat16x3 d, FfxFloat16x3 s)
{
    return FFX_TO_FLOAT16X3(FFX_TO_UINT16X3(d) | (FFX_TO_UINT16X3(s) & FFX_BROADCAST_UINT16X3(0x8000u)));
}

FfxFloat16x4 ffxCopySignBitHalf(FfxFloat16x4 d, FfxFloat16x4 s)
{
    return FFX_TO_FLOAT16X4(FFX_TO_UINT16X4(d) | (FFX_TO_UINT16X4(s) & FFX_BROADCAST_UINT16X4(0x8000u)));
}

FfxFloat16 ffxIsSignedHalf(FfxFloat16 m)
{
    return ffxSaturate(m * FFX_BROADCAST_FLOAT16(FFX_NEGATIVE_INFINITY_HALF));
}

FfxFloat16x2 ffxIsSignedHalf(FfxFloat16x2 m)
{
    return ffxSaturate(m * FFX_BROADCAST_FLOAT16X2(FFX_NEGATIVE_INFINITY_HALF));
}

FfxFloat16x3 ffxIsSignedHalf(FfxFloat16x3 m)
{
    return ffxSaturate(m * FFX_BROADCAST_FLOAT16X3(FFX_NEGATIVE_INFINITY_HALF));
}

FfxFloat16x4 ffxIsSignedHalf(FfxFloat16x4 m)
{
    return ffxSaturate(m * FFX_BROADCAST_FLOAT16X4(FFX_NEGATIVE_INFINITY_HALF));
}

FfxFloat16 ffxIsGreaterThanZeroHalf(FfxFloat16 m)
{
    return ffxSaturate(m * FFX_BROADCAST_FLOAT16(FFX_POSITIVE_INFINITY_HALF));
}

FfxFloat16x2 ffxIsGreaterThanZeroHalf(FfxFloat16x2 m)
{
    return ffxSaturate(m * FFX_BROADCAST_FLOAT16X2(FFX_POSITIVE_INFINITY_HALF));
}

FfxFloat16x3 ffxIsGreaterThanZeroHalf(FfxFloat16x3 m)
{
    return ffxSaturate(m * FFX_BROADCAST_FLOAT16X3(FFX_POSITIVE_INFINITY_HALF));
}

FfxFloat16x4 ffxIsGreaterThanZeroHalf(FfxFloat16x4 m)
{
    return ffxSaturate(m * FFX_BROADCAST_FLOAT16X4(FFX_POSITIVE_INFINITY_HALF));
}

FfxUInt16 ffxFloatToSortableIntegerHalf(FfxUInt16 x)
{
    return x ^ ((ffxBitShiftRightHalf(x, FFX_BROADCAST_UINT16(15))) | FFX_BROADCAST_UINT16(0x8000));
}

FfxUInt16 ffxSortableIntegerToFloatHalf(FfxUInt16 x)
{
    return x ^ ((~ffxBitShiftRightHalf(x, FFX_BROADCAST_UINT16(15))) | FFX_BROADCAST_UINT16(0x8000));
}

FfxUInt16x2 ffxFloatToSortableIntegerHalf(FfxUInt16x2 x)
{
    return x ^ ((ffxBitShiftRightHalf(x, FFX_BROADCAST_UINT16X2(15))) | FFX_BROADCAST_UINT16X2(0x8000));
}

FfxUInt16x2 ffxSortableIntegerToFloatHalf(FfxUInt16x2 x)
{
    return x ^ ((~ffxBitShiftRightHalf(x, FFX_BROADCAST_UINT16X2(15))) | FFX_BROADCAST_UINT16X2(0x8000));
}

FfxUInt32 ffxPackBytesZeroY0ZeroX0(FfxUInt32x2 i)
{
    return ((i.x) & 0xffu) | ((i.y << 16) & 0xff0000u);
}

FfxUInt32 ffxPackBytesZeroY1ZeroX1(FfxUInt32x2 i)
{
    return ((i.x >> 8) & 0xffu) | ((i.y << 8) & 0xff0000u);
}

FfxUInt32 ffxPackBytesZeroY2ZeroX2(FfxUInt32x2 i)
{
    return ((i.x >> 16) & 0xffu) | ((i.y) & 0xff0000u);
}

FfxUInt32 ffxPackBytesZeroY3ZeroX3(FfxUInt32x2 i)
{
    return ((i.x >> 24) & 0xffu) | ((i.y >> 8) & 0xff0000u);
}

FfxUInt32 ffxPackBytesY3Y2Y1X0(FfxUInt32x2 i)
{
    return ((i.x) & 0x000000ffu) | (i.y & 0xffffff00u);
}

FfxUInt32 ffxPackBytesY3Y2Y1X2(FfxUInt32x2 i)
{
    return ((i.x >> 16) & 0x000000ffu) | (i.y & 0xffffff00u);
}

FfxUInt32 ffxPackBytesY3Y2X0Y0(FfxUInt32x2 i)
{
    return ((i.x << 8) & 0x0000ff00u) | (i.y & 0xffff00ffu);
}

FfxUInt32 ffxPackBytesY3Y2X2Y0(FfxUInt32x2 i)
{
    return ((i.x >> 8) & 0x0000ff00u) | (i.y & 0xffff00ffu);
}

FfxUInt32 ffxPackBytesY3X0Y1Y0(FfxUInt32x2 i)
{
    return ((i.x << 16) & 0x00ff0000u) | (i.y & 0xff00ffffu);
}

FfxUInt32 ffxPackBytesY3X2Y1Y0(FfxUInt32x2 i)
{
    return ((i.x) & 0x00ff0000u) | (i.y & 0xff00ffffu);
}

FfxUInt32 ffxPackBytesX0Y2Y1Y0(FfxUInt32x2 i)
{
    return ((i.x << 24) & 0xff000000u) | (i.y & 0x00ffffffu);
}

FfxUInt32 ffxPackBytesX2Y2Y1Y0(FfxUInt32x2 i)
{
    return ((i.x << 8) & 0xff000000u) | (i.y & 0x00ffffffu);
}

FfxUInt32 ffxPackBytesY2X2Y0X0(FfxUInt32x2 i)
{
    return ((i.x) & 0x00ff00ffu) | ((i.y << 8) & 0xff00ff00u);
}

FfxUInt32 ffxPackBytesY2Y0X2X0(FfxUInt32x2 i)
{
    return (((i.x) & 0xffu) | ((i.x >> 8) & 0xff00u) | ((i.y << 16) & 0xff0000u) | ((i.y << 8) & 0xff000000u));
}

FfxUInt16x2 ffxPackX0Y0X1Y1UnsignedToUint16x2(FfxFloat16x2 x, FfxFloat16x2 y)
{
    x *= FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0);
    y *= FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0);
    return FFX_UINT32_TO_UINT16X2(ffxPackBytesY2X2Y0X0(FfxUInt32x2(FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(x)), FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(y)))));
}

FfxUInt32x2 ffxPermuteUByte0Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0)));
    return FfxUInt32x2(ffxPackBytesY3Y2Y1X0(FfxUInt32x2(d.x, b)), ffxPackBytesY3Y2Y1X2(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteUByte1Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0)));
    return FfxUInt32x2(ffxPackBytesY3Y2X0Y0(FfxUInt32x2(d.x, b)), ffxPackBytesY3Y2X2Y0(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteUByte2Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0)));
    return FfxUInt32x2(ffxPackBytesY3X0Y1Y0(FfxUInt32x2(d.x, b)), ffxPackBytesY3X2Y1Y0(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteUByte3Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0)));
    return FfxUInt32x2(ffxPackBytesX0Y2Y1Y0(FfxUInt32x2(d.x, b)), ffxPackBytesX2Y2Y1Y0(FfxUInt32x2(d.y, b)));
}

FfxFloat16x2 ffxPermuteUByte0Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY0ZeroX0(i))) * FFX_BROADCAST_FLOAT16X2(32768.0);
}

FfxFloat16x2 ffxPermuteUByte1Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY1ZeroX1(i))) * FFX_BROADCAST_FLOAT16X2(32768.0);
}

FfxFloat16x2 ffxPermuteUByte2Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY2ZeroX2(i))) * FFX_BROADCAST_FLOAT16X2(32768.0);
}

FfxFloat16x2 ffxPermuteUByte3Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY3ZeroX3(i))) * FFX_BROADCAST_FLOAT16X2(32768.0);
}

FfxUInt16x2 ffxPackX0Y0X1Y1SignedToUint16x2(FfxFloat16x2 x, FfxFloat16x2 y)
{
    x = x * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0);
    y = y * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0);
    return FFX_UINT32_TO_UINT16X2(ffxPackBytesY2X2Y0X0(FfxUInt32x2(FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(x)), FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(y)))));
}

FfxUInt32x2 ffxPermuteSByte0Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0)));
    return FfxUInt32x2(ffxPackBytesY3Y2Y1X0(FfxUInt32x2(d.x, b)), ffxPackBytesY3Y2Y1X2(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteSByte1Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0)));
    return FfxUInt32x2(ffxPackBytesY3Y2X0Y0(FfxUInt32x2(d.x, b)), ffxPackBytesY3Y2X2Y0(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteSByte2Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0)));
    return FfxUInt32x2(ffxPackBytesY3X0Y1Y0(FfxUInt32x2(d.x, b)), ffxPackBytesY3X2Y1Y0(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteSByte3Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0)));
    return FfxUInt32x2(ffxPackBytesX0Y2Y1Y0(FfxUInt32x2(d.x, b)), ffxPackBytesX2Y2Y1Y0(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteZeroBasedSByte0Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0))) ^ 0x00800080u;
    return FfxUInt32x2(ffxPackBytesY3Y2Y1X0(FfxUInt32x2(d.x, b)), ffxPackBytesY3Y2Y1X2(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteZeroBasedSByte1Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0))) ^ 0x00800080u;
    return FfxUInt32x2(ffxPackBytesY3Y2X0Y0(FfxUInt32x2(d.x, b)), ffxPackBytesY3Y2X2Y0(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteZeroBasedSByte2Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0))) ^ 0x00800080u;
    return FfxUInt32x2(ffxPackBytesY3X0Y1Y0(FfxUInt32x2(d.x, b)), ffxPackBytesY3X2Y1Y0(FfxUInt32x2(d.y, b)));
}

FfxUInt32x2 ffxPermuteZeroBasedSByte3Float16x2ToUint2(FfxUInt32x2 d, FfxFloat16x2 i)
{
    FfxUInt32 b = FFX_UINT16X2_TO_UINT32(FFX_TO_UINT16X2(i * FFX_BROADCAST_FLOAT16X2(1.0 / 32768.0) + FFX_BROADCAST_FLOAT16X2(0.25 / 32768.0))) ^ 0x00800080u;
    return FfxUInt32x2(ffxPackBytesX0Y2Y1Y0(FfxUInt32x2(d.x, b)), ffxPackBytesX2Y2Y1Y0(FfxUInt32x2(d.y, b)));
}

FfxFloat16x2 ffxPermuteSByte0Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY0ZeroX0(i))) * FFX_BROADCAST_FLOAT16X2(32768.0) - FFX_BROADCAST_FLOAT16X2(0.25);
}

FfxFloat16x2 ffxPermuteSByte1Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY1ZeroX1(i))) * FFX_BROADCAST_FLOAT16X2(32768.0) - FFX_BROADCAST_FLOAT16X2(0.25);
}

FfxFloat16x2 ffxPermuteSByte2Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY2ZeroX2(i))) * FFX_BROADCAST_FLOAT16X2(32768.0) - FFX_BROADCAST_FLOAT16X2(0.25);
}

FfxFloat16x2 ffxPermuteSByte3Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY3ZeroX3(i))) * FFX_BROADCAST_FLOAT16X2(32768.0) - FFX_BROADCAST_FLOAT16X2(0.25);
}

FfxFloat16x2 ffxPermuteZeroBasedSByte0Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY0ZeroX0(i) ^ 0x00800080u)) * FFX_BROADCAST_FLOAT16X2(32768.0) - FFX_BROADCAST_FLOAT16X2(0.25);
}

FfxFloat16x2 ffxPermuteZeroBasedSByte1Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY1ZeroX1(i) ^ 0x00800080u)) * FFX_BROADCAST_FLOAT16X2(32768.0) - FFX_BROADCAST_FLOAT16X2(0.25);
}

FfxFloat16x2 ffxPermuteZeroBasedSByte2Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY2ZeroX2(i) ^ 0x00800080u)) * FFX_BROADCAST_FLOAT16X2(32768.0) - FFX_BROADCAST_FLOAT16X2(0.25);
}

FfxFloat16x2 ffxPermuteZeroBasedSByte3Uint2ToFloat16x2(FfxUInt32x2 i)
{
    return FFX_TO_FLOAT16X2(FFX_UINT32_TO_UINT16X2(ffxPackBytesZeroY3ZeroX3(i) ^ 0x00800080u)) * FFX_BROADCAST_FLOAT16X2(32768.0) - FFX_BROADCAST_FLOAT16X2(0.25);
}

FfxFloat16 ffxApproximateSqrtHalf(FfxFloat16 a)
{
    return FFX_TO_FLOAT16((FFX_TO_UINT16(a) >> FFX_BROADCAST_UINT16(1)) + FFX_BROADCAST_UINT16(0x1de2));
}

FfxFloat16x2 ffxApproximateSqrtHalf(FfxFloat16x2 a)
{
    return FFX_TO_FLOAT16X2((FFX_TO_UINT16X2(a) >> FFX_BROADCAST_UINT16X2(1)) + FFX_BROADCAST_UINT16X2(0x1de2));
}

FfxFloat16x3 ffxApproximateSqrtHalf(FfxFloat16x3 a)
{
    return FFX_TO_FLOAT16X3((FFX_TO_UINT16X3(a) >> FFX_BROADCAST_UINT16X3(1)) + FFX_BROADCAST_UINT16X3(0x1de2));
}

FfxFloat16 ffxApproximateReciprocalHalf(FfxFloat16 a)
{
    return FFX_TO_FLOAT16(FFX_BROADCAST_UINT16(0x7784) - FFX_TO_UINT16(a));
}

FfxFloat16x2 ffxApproximateReciprocalHalf(FfxFloat16x2 a)
{
    return FFX_TO_FLOAT16X2(FFX_BROADCAST_UINT16X2(0x7784) - FFX_TO_UINT16X2(a));
}

FfxFloat16x3 ffxApproximateReciprocalHalf(FfxFloat16x3 a)
{
    return FFX_TO_FLOAT16X3(FFX_BROADCAST_UINT16X3(0x7784) - FFX_TO_UINT16X3(a));
}

FfxFloat16x4 ffxApproximateReciprocalHalf(FfxFloat16x4 a)
{
    return FFX_TO_FLOAT16X4(FFX_BROADCAST_UINT16X4(0x7784) - FFX_TO_UINT16X4(a));
}

FfxFloat16 ffxApproximateReciprocalMediumHalf(FfxFloat16 a)
{
    FfxFloat16 b = FFX_TO_FLOAT16(FFX_BROADCAST_UINT16(0x778d) - FFX_TO_UINT16(a));
    return b * (-b * a + FFX_BROADCAST_FLOAT16(2.0));
}

FfxFloat16x2 ffxApproximateReciprocalMediumHalf(FfxFloat16x2 a)
{
    FfxFloat16x2 b = FFX_TO_FLOAT16X2(FFX_BROADCAST_UINT16X2(0x778d) - FFX_TO_UINT16X2(a));
    return b * (-b * a + FFX_BROADCAST_FLOAT16X2(2.0));
}

FfxFloat16x3 ffxApproximateReciprocalMediumHalf(FfxFloat16x3 a)
{
    FfxFloat16x3 b = FFX_TO_FLOAT16X3(FFX_BROADCAST_UINT16X3(0x778d) - FFX_TO_UINT16X3(a));
    return b * (-b * a + FFX_BROADCAST_FLOAT16X3(2.0));
}

FfxFloat16x4 ffxApproximateReciprocalMediumHalf(FfxFloat16x4 a)
{
    FfxFloat16x4 b = FFX_TO_FLOAT16X4(FFX_BROADCAST_UINT16X4(0x778d) - FFX_TO_UINT16X4(a));
    return b * (-b * a + FFX_BROADCAST_FLOAT16X4(2.0));
}

FfxFloat16 ffxApproximateReciprocalSquareRootHalf(FfxFloat16 a)
{
    return FFX_TO_FLOAT16(FFX_BROADCAST_UINT16(0x59a3) - (FFX_TO_UINT16(a) >> FFX_BROADCAST_UINT16(1)));
}

FfxFloat16x2 ffxApproximateReciprocalSquareRootHalf(FfxFloat16x2 a)
{
    return FFX_TO_FLOAT16X2(FFX_BROADCAST_UINT16X2(0x59a3) - (FFX_TO_UINT16X2(a) >> FFX_BROADCAST_UINT16X2(1)));
}

FfxFloat16x3 ffxApproximateReciprocalSquareRootHalf(FfxFloat16x3 a)
{
    return FFX_TO_FLOAT16X3(FFX_BROADCAST_UINT16X3(0x59a3) - (FFX_TO_UINT16X3(a) >> FFX_BROADCAST_UINT16X3(1)));
}

FfxFloat16x4 ffxApproximateReciprocalSquareRootHalf(FfxFloat16x4 a)
{
    return FFX_TO_FLOAT16X4(FFX_BROADCAST_UINT16X4(0x59a3) - (FFX_TO_UINT16X4(a) >> FFX_BROADCAST_UINT16X4(1)));
}

FfxFloat16 ffxParabolicSinHalf(FfxFloat16 x)
{
    return x * abs(x) - x;
}

FfxFloat16x2 ffxParabolicSinHalf(FfxFloat16x2 x)
{
    return x * abs(x) - x;
}

FfxFloat16 ffxParabolicCosHalf(FfxFloat16 x)
{
    x = ffxFract(x * FFX_BROADCAST_FLOAT16(0.5) + FFX_BROADCAST_FLOAT16(0.75));
    x = x * FFX_BROADCAST_FLOAT16(2.0) - FFX_BROADCAST_FLOAT16(1.0);
    return ffxParabolicSinHalf(x);
}

FfxFloat16x2 ffxParabolicCosHalf(FfxFloat16x2 x)
{
    x = ffxFract(x * FFX_BROADCAST_FLOAT16X2(0.5) + FFX_BROADCAST_FLOAT16X2(0.75));
    x = x * FFX_BROADCAST_FLOAT16X2(2.0) - FFX_BROADCAST_FLOAT16X2(1.0);
    return ffxParabolicSinHalf(x);
}

FfxFloat16x2 ffxParabolicSinCosHalf(FfxFloat16 x)
{
    FfxFloat16 y = ffxFract(x * FFX_BROADCAST_FLOAT16(0.5) + FFX_BROADCAST_FLOAT16(0.75));
    y     = y * FFX_BROADCAST_FLOAT16(2.0) - FFX_BROADCAST_FLOAT16(1.0);
    return ffxParabolicSinHalf(FfxFloat16x2(x, y));
}

FfxUInt16 ffxZeroOneAndHalf(FfxUInt16 x, FfxUInt16 y)
{
    return min(x, y);
}

FfxUInt16x2 ffxZeroOneAndHalf(FfxUInt16x2 x, FfxUInt16x2 y)
{
    return min(x, y);
}

FfxUInt16x3 ffxZeroOneAndHalf(FfxUInt16x3 x, FfxUInt16x3 y)
{
    return min(x, y);
}

FfxUInt16x4 ffxZeroOneAndHalf(FfxUInt16x4 x, FfxUInt16x4 y)
{
    return min(x, y);
}

FfxUInt16 ffxZeroOneNotHalf(FfxUInt16 x)
{
    return x ^ FFX_BROADCAST_UINT16(1);
}

FfxUInt16x2 ffxZeroOneNotHalf(FfxUInt16x2 x)
{
    return x ^ FFX_BROADCAST_UINT16X2(1);
}

FfxUInt16x3 ffxZeroOneNotHalf(FfxUInt16x3 x)
{
    return x ^ FFX_BROADCAST_UINT16X3(1);
}

FfxUInt16x4 ffxZeroOneNotHalf(FfxUInt16x4 x)
{
    return x ^ FFX_BROADCAST_UINT16X4(1);
}

FfxUInt16 ffxZeroOneOrHalf(FfxUInt16 x, FfxUInt16 y)
{
    return max(x, y);
}

FfxUInt16x2 ffxZeroOneOrHalf(FfxUInt16x2 x, FfxUInt16x2 y)
{
    return max(x, y);
}

FfxUInt16x3 ffxZeroOneOrHalf(FfxUInt16x3 x, FfxUInt16x3 y)
{
    return max(x, y);
}

FfxUInt16x4 ffxZeroOneOrHalf(FfxUInt16x4 x, FfxUInt16x4 y)
{
    return max(x, y);
}

FfxUInt16 ffxZeroOneFloat16ToUint16(FfxFloat16 x)
{
    return FFX_TO_UINT16(x * FFX_TO_FLOAT16(FFX_TO_UINT16(1)));
}

FfxUInt16x2 ffxZeroOneFloat16x2ToUint16x2(FfxFloat16x2 x)
{
    return FFX_TO_UINT16X2(x * FFX_TO_FLOAT16X2(FfxUInt16x2(1, 1)));
}

FfxUInt16x3 ffxZeroOneFloat16x3ToUint16x3(FfxFloat16x3 x)
{
    return FFX_TO_UINT16X3(x * FFX_TO_FLOAT16X3(FfxUInt16x3(1, 1, 1)));
}

FfxUInt16x4 ffxZeroOneFloat16x4ToUint16x4(FfxFloat16x4 x)
{
    return FFX_TO_UINT16X4(x * FFX_TO_FLOAT16X4(FfxUInt16x4(1, 1, 1, 1)));
}

FfxFloat16 ffxZeroOneUint16ToFloat16(FfxUInt16 x)
{
    return FFX_TO_FLOAT16(x * FFX_TO_UINT16(FFX_TO_FLOAT16(1.0)));
}

FfxFloat16x2 ffxZeroOneUint16x2ToFloat16x2(FfxUInt16x2 x)
{
    return FFX_TO_FLOAT16X2(x * FFX_TO_UINT16X2(FfxUInt16x2(FFX_TO_FLOAT16(1.0), FFX_TO_FLOAT16(1.0))));
}

FfxFloat16x3 ffxZeroOneUint16x3ToFloat16x3(FfxUInt16x3 x)
{
    return FFX_TO_FLOAT16X3(x * FFX_TO_UINT16X3(FfxUInt16x3(FFX_TO_FLOAT16(1.0), FFX_TO_FLOAT16(1.0), FFX_TO_FLOAT16(1.0))));
}

FfxFloat16x4 ffxZeroOneUint16x4ToFloat16x4(FfxUInt16x4 x)
{
    return FFX_TO_FLOAT16X4(x * FFX_TO_UINT16X4(FfxUInt16x4(FFX_TO_FLOAT16(1.0), FFX_TO_FLOAT16(1.0), FFX_TO_FLOAT16(1.0), FFX_TO_FLOAT16(1.0))));
}

FfxFloat16 ffxZeroOneAndHalf(FfxFloat16 x, FfxFloat16 y)
{
    return min(x, y);
}

FfxFloat16x2 ffxZeroOneAndHalf(FfxFloat16x2 x, FfxFloat16x2 y)
{
    return min(x, y);
}

FfxFloat16x3 ffxZeroOneAndHalf(FfxFloat16x3 x, FfxFloat16x3 y)
{
    return min(x, y);
}

FfxFloat16x4 ffxZeroOneAndHalf(FfxFloat16x4 x, FfxFloat16x4 y)
{
    return min(x, y);
}

FfxFloat16 ffxSignedZeroOneAndOrHalf(FfxFloat16 x, FfxFloat16 y)
{
    return (-x) * y + FFX_BROADCAST_FLOAT16(1.0);
}

FfxFloat16x2 ffxSignedZeroOneAndOrHalf(FfxFloat16x2 x, FfxFloat16x2 y)
{
    return (-x) * y + FFX_BROADCAST_FLOAT16X2(1.0);
}

FfxFloat16x3 ffxSignedZeroOneAndOrHalf(FfxFloat16x3 x, FfxFloat16x3 y)
{
    return (-x) * y + FFX_BROADCAST_FLOAT16X3(1.0);
}

FfxFloat16x4 ffxSignedZeroOneAndOrHalf(FfxFloat16x4 x, FfxFloat16x4 y)
{
    return (-x) * y + FFX_BROADCAST_FLOAT16X4(1.0);
}

FfxFloat16 ffxZeroOneAndOrHalf(FfxFloat16 x, FfxFloat16 y, FfxFloat16 z)
{
    return ffxSaturate(x * y + z);
}

FfxFloat16x2 ffxZeroOneAndOrHalf(FfxFloat16x2 x, FfxFloat16x2 y, FfxFloat16x2 z)
{
    return ffxSaturate(x * y + z);
}

FfxFloat16x3 ffxZeroOneAndOrHalf(FfxFloat16x3 x, FfxFloat16x3 y, FfxFloat16x3 z)
{
    return ffxSaturate(x * y + z);
}

FfxFloat16x4 ffxZeroOneAndOrHalf(FfxFloat16x4 x, FfxFloat16x4 y, FfxFloat16x4 z)
{
    return ffxSaturate(x * y + z);
}

FfxFloat16 ffxZeroOneIsGreaterThanZeroHalf(FfxFloat16 x)
{
    return ffxSaturate(x * FFX_BROADCAST_FLOAT16(FFX_POSITIVE_INFINITY_HALF));
}

FfxFloat16x2 ffxZeroOneIsGreaterThanZeroHalf(FfxFloat16x2 x)
{
    return ffxSaturate(x * FFX_BROADCAST_FLOAT16X2(FFX_POSITIVE_INFINITY_HALF));
}

FfxFloat16x3 ffxZeroOneIsGreaterThanZeroHalf(FfxFloat16x3 x)
{
    return ffxSaturate(x * FFX_BROADCAST_FLOAT16X3(FFX_POSITIVE_INFINITY_HALF));
}

FfxFloat16x4 ffxZeroOneIsGreaterThanZeroHalf(FfxFloat16x4 x)
{
    return ffxSaturate(x * FFX_BROADCAST_FLOAT16X4(FFX_POSITIVE_INFINITY_HALF));
}

FfxFloat16 ffxZeroOneNotHalf(FfxFloat16 x)
{
    return FFX_BROADCAST_FLOAT16(1.0) - x;
}

FfxFloat16x2 ffxZeroOneNotHalf(FfxFloat16x2 x)
{
    return FFX_BROADCAST_FLOAT16X2(1.0) - x;
}

FfxFloat16x3 ffxZeroOneNotHalf(FfxFloat16x3 x)
{
    return FFX_BROADCAST_FLOAT16X3(1.0) - x;
}

FfxFloat16x4 ffxZeroOneNotHalf(FfxFloat16x4 x)
{
    return FFX_BROADCAST_FLOAT16X4(1.0) - x;
}

FfxFloat16 ffxZeroOneOrHalf(FfxFloat16 x, FfxFloat16 y)
{
    return max(x, y);
}

FfxFloat16x2 ffxZeroOneOrHalf(FfxFloat16x2 x, FfxFloat16x2 y)
{
    return max(x, y);
}

FfxFloat16x3 ffxZeroOneOrHalf(FfxFloat16x3 x, FfxFloat16x3 y)
{
    return max(x, y);
}

FfxFloat16x4 ffxZeroOneOrHalf(FfxFloat16x4 x, FfxFloat16x4 y)
{
    return max(x, y);
}

FfxFloat16 ffxZeroOneSelectHalf(FfxFloat16 x, FfxFloat16 y, FfxFloat16 z)
{
    FfxFloat16 r = (-x) * z + z;
    return x * y + r;
}

FfxFloat16x2 ffxZeroOneSelectHalf(FfxFloat16x2 x, FfxFloat16x2 y, FfxFloat16x2 z)
{
    FfxFloat16x2 r = (-x) * z + z;
    return x * y + r;
}

FfxFloat16x3 ffxZeroOneSelectHalf(FfxFloat16x3 x, FfxFloat16x3 y, FfxFloat16x3 z)
{
    FfxFloat16x3 r = (-x) * z + z;
    return x * y + r;
}

FfxFloat16x4 ffxZeroOneSelectHalf(FfxFloat16x4 x, FfxFloat16x4 y, FfxFloat16x4 z)
{
    FfxFloat16x4 r = (-x) * z + z;
    return x * y + r;
}

FfxFloat16 ffxZeroOneIsSignedHalf(FfxFloat16 x)
{
    return ffxSaturate(x * FFX_BROADCAST_FLOAT16(FFX_NEGATIVE_INFINITY_HALF));
}

FfxFloat16x2 ffxZeroOneIsSignedHalf(FfxFloat16x2 x)
{
    return ffxSaturate(x * FFX_BROADCAST_FLOAT16X2(FFX_NEGATIVE_INFINITY_HALF));
}

FfxFloat16x3 ffxZeroOneIsSignedHalf(FfxFloat16x3 x)
{
    return ffxSaturate(x * FFX_BROADCAST_FLOAT16X3(FFX_NEGATIVE_INFINITY_HALF));
}

FfxFloat16x4 ffxZeroOneIsSignedHalf(FfxFloat16x4 x)
{
    return ffxSaturate(x * FFX_BROADCAST_FLOAT16X4(FFX_NEGATIVE_INFINITY_HALF));
}

FfxFloat16 ffxRec709FromLinearHalf(FfxFloat16 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.018 * 4.5, 4.5, 0.45);
    FfxFloat16x2 k = FfxFloat16x2(1.099, -0.099);
    return clamp(j.x, c * j.y, pow(c, j.z) * k.x + k.y);
}

FfxFloat16x2 ffxRec709FromLinearHalf(FfxFloat16x2 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.018 * 4.5, 4.5, 0.45);
    FfxFloat16x2 k = FfxFloat16x2(1.099, -0.099);
    return clamp(j.xx, c * j.yy, pow(c, j.zz) * k.xx + k.yy);
}

FfxFloat16x3 ffxRec709FromLinearHalf(FfxFloat16x3 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.018 * 4.5, 4.5, 0.45);
    FfxFloat16x2 k = FfxFloat16x2(1.099, -0.099);
    return clamp(j.xxx, c * j.yyy, pow(c, j.zzz) * k.xxx + k.yyy);
}

FfxFloat16 ffxGammaFromLinearHalf(FfxFloat16 c, FfxFloat16 rcpX)
{
    return pow(c, FFX_BROADCAST_FLOAT16(rcpX));
}

FfxFloat16x2 ffxGammaFromLinearHalf(FfxFloat16x2 c, FfxFloat16 rcpX)
{
    return pow(c, FFX_BROADCAST_FLOAT16X2(rcpX));
}

FfxFloat16x3 ffxGammaFromLinearHalf(FfxFloat16x3 c, FfxFloat16 rcpX)
{
    return pow(c, FFX_BROADCAST_FLOAT16X3(rcpX));
}

FfxFloat16 ffxSrgbFromLinearHalf(FfxFloat16 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.0031308 * 12.92, 12.92, 1.0 / 2.4);
    FfxFloat16x2 k = FfxFloat16x2(1.055, -0.055);
    return clamp(j.x, c * j.y, pow(c, j.z) * k.x + k.y);
}

FfxFloat16x2 ffxSrgbFromLinearHalf(FfxFloat16x2 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.0031308 * 12.92, 12.92, 1.0 / 2.4);
    FfxFloat16x2 k = FfxFloat16x2(1.055, -0.055);
    return clamp(j.xx, c * j.yy, pow(c, j.zz) * k.xx + k.yy);
}

FfxFloat16x3 ffxSrgbFromLinearHalf(FfxFloat16x3 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.0031308 * 12.92, 12.92, 1.0 / 2.4);
    FfxFloat16x2 k = FfxFloat16x2(1.055, -0.055);
    return clamp(j.xxx, c * j.yyy, pow(c, j.zzz) * k.xxx + k.yyy);
}

FfxFloat16 ffxSquareRootHalf(FfxFloat16 c)
{
    return sqrt(c);
}

FfxFloat16x2 ffxSquareRootHalf(FfxFloat16x2 c)
{
    return sqrt(c);
}

FfxFloat16x3 ffxSquareRootHalf(FfxFloat16x3 c)
{
    return sqrt(c);
}

FfxFloat16 ffxCubeRootHalf(FfxFloat16 c)
{
    return pow(c, FFX_BROADCAST_FLOAT16(1.0 / 3.0));
}

FfxFloat16x2 ffxCubeRootHalf(FfxFloat16x2 c)
{
    return pow(c, FFX_BROADCAST_FLOAT16X2(1.0 / 3.0));
}

FfxFloat16x3 ffxCubeRootHalf(FfxFloat16x3 c)
{
    return pow(c, FFX_BROADCAST_FLOAT16X3(1.0 / 3.0));
}

FfxFloat16 ffxLinearFromRec709Half(FfxFloat16 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.081 / 4.5, 1.0 / 4.5, 1.0 / 0.45);
    FfxFloat16x2 k = FfxFloat16x2(1.0 / 1.099, 0.099 / 1.099);
    return ffxZeroOneSelectHalf(ffxZeroOneIsSignedHalf(c - j.x), c * j.y, pow(c * k.x + k.y, j.z));
}

FfxFloat16x2 ffxLinearFromRec709Half(FfxFloat16x2 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.081 / 4.5, 1.0 / 4.5, 1.0 / 0.45);
    FfxFloat16x2 k = FfxFloat16x2(1.0 / 1.099, 0.099 / 1.099);
    return ffxZeroOneSelectHalf(ffxZeroOneIsSignedHalf(c - j.xx), c * j.yy, pow(c * k.xx + k.yy, j.zz));
}

FfxFloat16x3 ffxLinearFromRec709Half(FfxFloat16x3 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.081 / 4.5, 1.0 / 4.5, 1.0 / 0.45);
    FfxFloat16x2 k = FfxFloat16x2(1.0 / 1.099, 0.099 / 1.099);
    return ffxZeroOneSelectHalf(ffxZeroOneIsSignedHalf(c - j.xxx), c * j.yyy, pow(c * k.xxx + k.yyy, j.zzz));
}

FfxFloat16 ffxLinearFromGammaHalf(FfxFloat16 c, FfxFloat16 x)
{
    return pow(c, FFX_BROADCAST_FLOAT16(x));
}

FfxFloat16x2 ffxLinearFromGammaHalf(FfxFloat16x2 c, FfxFloat16 x)
{
    return pow(c, FFX_BROADCAST_FLOAT16X2(x));
}

FfxFloat16x3 ffxLinearFromGammaHalf(FfxFloat16x3 c, FfxFloat16 x)
{
    return pow(c, FFX_BROADCAST_FLOAT16X3(x));
}

FfxFloat16 ffxLinearFromSrgbHalf(FfxFloat16 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.04045 / 12.92, 1.0 / 12.92, 2.4);
    FfxFloat16x2 k = FfxFloat16x2(1.0 / 1.055, 0.055 / 1.055);
    return ffxZeroOneSelectHalf(ffxZeroOneIsSignedHalf(c - j.x), c * j.y, pow(c * k.x + k.y, j.z));
}

FfxFloat16x2 ffxLinearFromSrgbHalf(FfxFloat16x2 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.04045 / 12.92, 1.0 / 12.92, 2.4);
    FfxFloat16x2 k = FfxFloat16x2(1.0 / 1.055, 0.055 / 1.055);
    return ffxZeroOneSelectHalf(ffxZeroOneIsSignedHalf(c - j.xx), c * j.yy, pow(c * k.xx + k.yy, j.zz));
}

FfxFloat16x3 ffxLinearFromSrgbHalf(FfxFloat16x3 c)
{
    FfxFloat16x3 j = FfxFloat16x3(0.04045 / 12.92, 1.0 / 12.92, 2.4);
    FfxFloat16x2 k = FfxFloat16x2(1.0 / 1.055, 0.055 / 1.055);
    return ffxZeroOneSelectHalf(ffxZeroOneIsSignedHalf(c - j.xxx), c * j.yyy, pow(c * k.xxx + k.yyy, j.zzz));
}

FfxUInt16x2 ffxRemapForQuadHalf(FfxUInt32 a)
{
    return FfxUInt16x2(bitfieldExtract(a, 1u, 3u), bitfieldInsertMask(bitfieldExtract(a, 3u, 3u), a, 1u));
}

FfxUInt16x2 ffxRemapForWaveReductionHalf(FfxUInt32 a)
{
    return FfxUInt16x2(bitfieldInsertMask(bitfieldExtract(a, 2u, 3u), a, 1u), bitfieldInsertMask(bitfieldExtract(a, 3u, 3u), bitfieldExtract(a, 1u, 2u), 2u));
}

#endif  // FFX_HALF
