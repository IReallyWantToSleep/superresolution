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

#define FFX_GROUPSHARED shared

#define FFX_GROUP_MEMORY_BARRIER() barrier()

#define FFX_STATIC

#define FFX_UNROLL

#define FFX_GREATER_THAN(x, y) greaterThan(x, y)

#define FFX_GREATER_THAN_EQUAL(x, y) greaterThanEqual(x, y)

#define FFX_LESS_THAN(x, y) lessThan(x, y)

#define FFX_LESS_THAN_EQUAL(x, y) lessThanEqual(x, y)

#define FFX_EQUAL(x, y) equal(x, y)

#define FFX_NOT_EQUAL(x, y) notEqual(x, y)

#define FFX_BROADCAST_FLOAT32(x)   FfxFloat32(x)

#define FFX_BROADCAST_FLOAT32X2(x) FfxFloat32x2(FfxFloat32(x))

#define FFX_BROADCAST_FLOAT32X3(x) FfxFloat32x3(FfxFloat32(x))

#define FFX_BROADCAST_FLOAT32X4(x) FfxFloat32x4(FfxFloat32(x))

#define FFX_BROADCAST_UINT32(x)   FfxUInt32(x)

#define FFX_BROADCAST_UINT32X2(x) FfxUInt32x2(FfxUInt32(x))

#define FFX_BROADCAST_UINT32X3(x) FfxUInt32x3(FfxUInt32(x))

#define FFX_BROADCAST_UINT32X4(x) FfxUInt32x4(FfxUInt32(x))

#define FFX_BROADCAST_INT32(x)   FfxInt32(x)

#define FFX_BROADCAST_INT32X2(x) FfxInt32x2(FfxInt32(x))

#define FFX_BROADCAST_INT32X3(x) FfxInt32x3(FfxInt32(x))

#define FFX_BROADCAST_INT32X4(x) FfxInt32x4(FfxInt32(x))

#define FFX_BROADCAST_MIN_FLOAT16(x)   FFX_MIN16_F(x)

#define FFX_BROADCAST_MIN_FLOAT16X2(x) FFX_MIN16_F2(FFX_MIN16_F(x))

#define FFX_BROADCAST_MIN_FLOAT16X3(x) FFX_MIN16_F3(FFX_MIN16_F(x))

#define FFX_BROADCAST_MIN_FLOAT16X4(x) FFX_MIN16_F4(FFX_MIN16_F(x))

#define FFX_BROADCAST_MIN_UINT16(x)   FFX_MIN16_U(x)

#define FFX_BROADCAST_MIN_UINT16X2(x) FFX_MIN16_U2(FFX_MIN16_U(x))

#define FFX_BROADCAST_MIN_UINT16X3(x) FFX_MIN16_U3(FFX_MIN16_U(x))

#define FFX_BROADCAST_MIN_UINT16X4(x) FFX_MIN16_U4(FFX_MIN16_U(x))

#define FFX_BROADCAST_MIN_INT16(x)   FFX_MIN16_I(x)

#define FFX_BROADCAST_MIN_INT16X2(x) FFX_MIN16_I2(FFX_MIN16_I(x))

#define FFX_BROADCAST_MIN_INT16X3(x) FFX_MIN16_I3(FFX_MIN16_I(x))

#define FFX_BROADCAST_MIN_INT16X4(x) FFX_MIN16_I4(FFX_MIN16_I(x))

#if !defined(FFX_SKIP_EXT)
#if FFX_HALF
    #extension GL_EXT_shader_16bit_storage : require
    #extension GL_EXT_shader_explicit_arithmetic_types : require
#endif // FFX_HALF

#if defined(FFX_LONG)
    #extension GL_ARB_gpu_shader_int64 : require
    #extension GL_NV_shader_atomic_int64 : require
#endif // #if defined(FFX_LONG)

#if defined(FFX_WAVE)
    #extension GL_KHR_shader_subgroup_arithmetic : require
    #extension GL_KHR_shader_subgroup_ballot : require
    #extension GL_KHR_shader_subgroup_quad : require
    #extension GL_KHR_shader_subgroup_shuffle : require
#endif // #if defined(FFX_WAVE)
#endif // #if !defined(FFX_SKIP_EXT)

// Forward declarations
FfxFloat32   ffxSqrt(FfxFloat32 x);
FfxFloat32x2 ffxSqrt(FfxFloat32x2 x);
FfxFloat32x3 ffxSqrt(FfxFloat32x3 x);
FfxFloat32x4 ffxSqrt(FfxFloat32x4 x);

FfxFloat32 ffxAsFloat(FfxUInt32 x)
{
    return uintBitsToFloat(x);
}

FfxFloat32x2 ffxAsFloat(FfxUInt32x2 x)
{
    return uintBitsToFloat(x);
}

FfxFloat32x3 ffxAsFloat(FfxUInt32x3 x)
{
    return uintBitsToFloat(x);
}

FfxFloat32x4 ffxAsFloat(FfxUInt32x4 x)
{
    return uintBitsToFloat(x);
}

FfxUInt32 ffxAsUInt32(FfxFloat32 x)
{
    return floatBitsToUint(x);
}

FfxUInt32x2 ffxAsUInt32(FfxFloat32x2 x)
{
    return floatBitsToUint(x);
}

FfxUInt32x3 ffxAsUInt32(FfxFloat32x3 x)
{
    return floatBitsToUint(x);
}

FfxUInt32x4 ffxAsUInt32(FfxFloat32x4 x)
{
    return floatBitsToUint(x);
}

FfxUInt32 f32tof16(FfxFloat32 value)
{
    return packHalf2x16(FfxFloat32x2(value, 0.0));
}

FfxFloat32x2 ffxBroadcast2(FfxFloat32 value)
{
    return FfxFloat32x2(value, value);
}

FfxFloat32x3 ffxBroadcast3(FfxFloat32 value)
{
    return FfxFloat32x3(value, value, value);
}

FfxFloat32x4 ffxBroadcast4(FfxFloat32 value)
{
    return FfxFloat32x4(value, value, value, value);
}

FfxInt32x2 ffxBroadcast2(FfxInt32 value)
{
    return FfxInt32x2(value, value);
}

FfxInt32x3 ffxBroadcast3(FfxInt32 value)
{
    return FfxInt32x3(value, value, value);
}

FfxInt32x4 ffxBroadcast4(FfxInt32 value)
{
    return FfxInt32x4(value, value, value, value);
}

FfxUInt32x2 ffxBroadcast2(FfxUInt32 value)
{
    return FfxUInt32x2(value, value);
}

FfxUInt32x3 ffxBroadcast3(FfxUInt32 value)
{
    return FfxUInt32x3(value, value, value);
}

FfxUInt32x4 ffxBroadcast4(FfxUInt32 value)
{
    return FfxUInt32x4(value, value, value, value);
}

FfxUInt32 bitfieldExtract(FfxUInt32 src, FfxUInt32 off, FfxUInt32 bits)
{
    return bitfieldExtract(src, FfxInt32(off), FfxInt32(bits));
}

FfxUInt32 bitfieldInsert(FfxUInt32 src, FfxUInt32 ins, FfxUInt32 mask)
{
    return (ins & mask) | (src & (~mask));
}

// Proxy for V_BFI_B32 where the 'mask' is set as 'bits', 'mask=(1<<bits)-1', and 'bits' needs to be an immediate.
FfxUInt32 bitfieldInsertMask(FfxUInt32 src, FfxUInt32 ins, FfxUInt32 bits)
{
    return bitfieldInsert(src, ins, 0, FfxInt32(bits));
}

FfxFloat32 ffxLerp(FfxFloat32 x, FfxFloat32 y, FfxFloat32 t)
{
    return mix(x, y, t);
}

FfxFloat32x2 ffxLerp(FfxFloat32x2 x, FfxFloat32x2 y, FfxFloat32 t)
{
    return mix(x, y, t);
}

FfxFloat32x2 ffxLerp(FfxFloat32x2 x, FfxFloat32x2 y, FfxFloat32x2 t)
{
    return mix(x, y, t);
}

FfxFloat32x3 ffxLerp(FfxFloat32x3 x, FfxFloat32x3 y, FfxFloat32 t)
{
    return mix(x, y, t);
}

FfxFloat32x3 ffxLerp(FfxFloat32x3 x, FfxFloat32x3 y, FfxFloat32x3 t)
{
    return mix(x, y, t);
}

FfxFloat32x4 ffxLerp(FfxFloat32x4 x, FfxFloat32x4 y, FfxFloat32 t)
{
    return mix(x, y, t);
}

FfxFloat32x4 ffxLerp(FfxFloat32x4 x, FfxFloat32x4 y, FfxFloat32x4 t)
{
    return mix(x, y, t);
}

FfxFloat32 ffxMax3(FfxFloat32 x, FfxFloat32 y, FfxFloat32 z)
{
    return max(x, max(y, z));
}

FfxFloat32x2 ffxMax3(FfxFloat32x2 x, FfxFloat32x2 y, FfxFloat32x2 z)
{
    return max(x, max(y, z));
}

FfxFloat32x3 ffxMax3(FfxFloat32x3 x, FfxFloat32x3 y, FfxFloat32x3 z)
{
    return max(x, max(y, z));
}

FfxFloat32x4 ffxMax3(FfxFloat32x4 x, FfxFloat32x4 y, FfxFloat32x4 z)
{
    return max(x, max(y, z));
}

FfxUInt32 ffxMax3(FfxUInt32 x, FfxUInt32 y, FfxUInt32 z)
{
    return max(x, max(y, z));
}

FfxUInt32x2 ffxMax3(FfxUInt32x2 x, FfxUInt32x2 y, FfxUInt32x2 z)
{
    return max(x, max(y, z));
}

FfxUInt32x3 ffxMax3(FfxUInt32x3 x, FfxUInt32x3 y, FfxUInt32x3 z)
{
    return max(x, max(y, z));
}

FfxUInt32x4 ffxMax3(FfxUInt32x4 x, FfxUInt32x4 y, FfxUInt32x4 z)
{
    return max(x, max(y, z));
}

FfxFloat32 ffxMed3(FfxFloat32 x, FfxFloat32 y, FfxFloat32 z)
{
    return max(min(x, y), min(max(x, y), z));
}

FfxFloat32x2 ffxMed3(FfxFloat32x2 x, FfxFloat32x2 y, FfxFloat32x2 z)
{
    return max(min(x, y), min(max(x, y), z));
}

FfxFloat32x3 ffxMed3(FfxFloat32x3 x, FfxFloat32x3 y, FfxFloat32x3 z)
{
    return max(min(x, y), min(max(x, y), z));
}

FfxFloat32x4 ffxMed3(FfxFloat32x4 x, FfxFloat32x4 y, FfxFloat32x4 z)
{
    return max(min(x, y), min(max(x, y), z));
}

FfxInt32 ffxMed3(FfxInt32 x, FfxInt32 y, FfxInt32 z)
{
    return max(min(x, y), min(max(x, y), z));
}

FfxInt32x2 ffxMed3(FfxInt32x2 x, FfxInt32x2 y, FfxInt32x2 z)
{
    return max(min(x, y), min(max(x, y), z));
}

FfxInt32x3 ffxMed3(FfxInt32x3 x, FfxInt32x3 y, FfxInt32x3 z)
{
    return max(min(x, y), min(max(x, y), z));
}

FfxInt32x4 ffxMed3(FfxInt32x4 x, FfxInt32x4 y, FfxInt32x4 z)
{
    return max(min(x, y), min(max(x, y), z));
}


FfxFloat32 ffxMin3(FfxFloat32 x, FfxFloat32 y, FfxFloat32 z)
{
    return min(x, min(y, z));
}

FfxFloat32x2 ffxMin3(FfxFloat32x2 x, FfxFloat32x2 y, FfxFloat32x2 z)
{
    return min(x, min(y, z));
}

FfxFloat32x3 ffxMin3(FfxFloat32x3 x, FfxFloat32x3 y, FfxFloat32x3 z)
{
    return min(x, min(y, z));
}

FfxFloat32x4 ffxMin3(FfxFloat32x4 x, FfxFloat32x4 y, FfxFloat32x4 z)
{
    return min(x, min(y, z));
}

FfxUInt32 ffxMin3(FfxUInt32 x, FfxUInt32 y, FfxUInt32 z)
{
    return min(x, min(y, z));
}

FfxUInt32x2 ffxMin3(FfxUInt32x2 x, FfxUInt32x2 y, FfxUInt32x2 z)
{
    return min(x, min(y, z));
}

FfxUInt32x3 ffxMin3(FfxUInt32x3 x, FfxUInt32x3 y, FfxUInt32x3 z)
{
    return min(x, min(y, z));
}

FfxUInt32x4 ffxMin3(FfxUInt32x4 x, FfxUInt32x4 y, FfxUInt32x4 z)
{
    return min(x, min(y, z));
}

FfxFloat32 rcp(FfxFloat32 x)
{
    return FfxFloat32(1.0) / x;
}

FfxFloat32x2 rcp(FfxFloat32x2 x)
{
    return ffxBroadcast2(1.0) / x;
}

FfxFloat32x3 rcp(FfxFloat32x3 x)
{
    return ffxBroadcast3(1.0) / x;
}

FfxFloat32x4 rcp(FfxFloat32x4 x)
{
    return ffxBroadcast4(1.0) / x;
}

FfxFloat32 rsqrt(FfxFloat32 x)
{
    return FfxFloat32(1.0) / ffxSqrt(x);
}

FfxFloat32x2 rsqrt(FfxFloat32x2 x)
{
    return ffxBroadcast2(1.0) / ffxSqrt(x);
}

FfxFloat32x3 rsqrt(FfxFloat32x3 x)
{
    return ffxBroadcast3(1.0) / ffxSqrt(x);
}

FfxFloat32x4 rsqrt(FfxFloat32x4 x)
{
    return ffxBroadcast4(1.0) / ffxSqrt(x);
}

FfxFloat32 ffxSaturate(FfxFloat32 x)
{
    return clamp(x, FfxFloat32(0.0), FfxFloat32(1.0));
}

FfxFloat32x2 ffxSaturate(FfxFloat32x2 x)
{
    return clamp(x, ffxBroadcast2(0.0), ffxBroadcast2(1.0));
}

FfxFloat32x3 ffxSaturate(FfxFloat32x3 x)
{
    return clamp(x, ffxBroadcast3(0.0), ffxBroadcast3(1.0));
}

FfxFloat32x4 ffxSaturate(FfxFloat32x4 x)
{
    return clamp(x, ffxBroadcast4(0.0), ffxBroadcast4(1.0));
}

FfxFloat32 ffxFract(FfxFloat32 x)
{
    return fract(x);
}

FfxFloat32x2 ffxFract(FfxFloat32x2 x)
{
    return fract(x);
}

FfxFloat32x3 ffxFract(FfxFloat32x3 x)
{
    return fract(x);
}

FfxFloat32x4 ffxFract(FfxFloat32x4 x)
{
    return fract(x);
}

FfxUInt32 AShrSU1(FfxUInt32 a, FfxUInt32 b)
{
    return FfxUInt32(FfxInt32(a) >> FfxInt32(b));
}

#if FFX_HALF

#define FFX_UINT32_TO_FLOAT16X2(x) unpackFloat2x16(FfxUInt32(x))

FfxFloat16x4 ffxUint32x2ToFloat16x4(FfxUInt32x2 x)
{
    return FfxFloat16x4(unpackFloat2x16(x.x), unpackFloat2x16(x.y));
}
#define FFX_UINT32X2_TO_FLOAT16X4(x) ffxUint32x2ToFloat16x4(FfxUInt32x2(x))
#define FFX_UINT32_TO_UINT16X2(x) unpackUint2x16(FfxUInt32(x))
#define FFX_UINT32X2_TO_UINT16X4(x) unpackUint4x16(pack64(FfxUInt32x2(x)))
//------------------------------------------------------------------------------------------------------------------------------
#define FFX_FLOAT16X2_TO_UINT32(x) packFloat2x16(FfxFloat16x2(x))
FfxUInt32x2 ffxFloat16x4ToUint32x2(FfxFloat16x4 x)
{
    return FfxUInt32x2(packFloat2x16(x.xy), packFloat2x16(x.zw));
}
#define FFX_FLOAT16X4_TO_UINT32X2(x) ffxFloat16x4ToUint32x2(FfxFloat16x4(x))
#define FFX_UINT16X2_TO_UINT32(x) packUint2x16(FfxUInt16x2(x))
#define FFX_UINT16X4_TO_UINT32X2(x) unpack32(packUint4x16(FfxUInt16x4(x)))
//==============================================================================================================================
#define FFX_TO_UINT16(x) halfBitsToUint16(FfxFloat16(x))
#define FFX_TO_UINT16X2(x) halfBitsToUint16(FfxFloat16x2(x))
#define FFX_TO_UINT16X3(x) halfBitsToUint16(FfxFloat16x3(x))
#define FFX_TO_UINT16X4(x) halfBitsToUint16(FfxFloat16x4(x))
//------------------------------------------------------------------------------------------------------------------------------
#define FFX_TO_FLOAT16(x) uint16BitsToHalf(FfxUInt16(x))
#define FFX_TO_FLOAT16X2(x) uint16BitsToHalf(FfxUInt16x2(x))
#define FFX_TO_FLOAT16X3(x) uint16BitsToHalf(FfxUInt16x3(x))
#define FFX_TO_FLOAT16X4(x) uint16BitsToHalf(FfxUInt16x4(x))
//==============================================================================================================================
FfxFloat16 ffxBroadcastFloat16(FfxFloat16 a)
{
    return FfxFloat16(a);
}
FfxFloat16x2 ffxBroadcastFloat16x2(FfxFloat16 a)
{
    return FfxFloat16x2(a, a);
}
FfxFloat16x3 ffxBroadcastFloat16x3(FfxFloat16 a)
{
    return FfxFloat16x3(a, a, a);
}
FfxFloat16x4 ffxBroadcastFloat16x4(FfxFloat16 a)
{
    return FfxFloat16x4(a, a, a, a);
}
#define FFX_BROADCAST_FLOAT16(a)   FfxFloat16(a)
#define FFX_BROADCAST_FLOAT16X2(a) FfxFloat16x2(FfxFloat16(a))
#define FFX_BROADCAST_FLOAT16X3(a) FfxFloat16x3(FfxFloat16(a))
#define FFX_BROADCAST_FLOAT16X4(a) FfxFloat16x4(FfxFloat16(a))
//------------------------------------------------------------------------------------------------------------------------------
FfxInt16 ffxBroadcastInt16(FfxInt16 a)
{
    return FfxInt16(a);
}
FfxInt16x2 ffxBroadcastInt16x2(FfxInt16 a)
{
    return FfxInt16x2(a, a);
}
FfxInt16x3 ffxBroadcastInt16x3(FfxInt16 a)
{
    return FfxInt16x3(a, a, a);
}
FfxInt16x4 ffxBroadcastInt16x4(FfxInt16 a)
{
    return FfxInt16x4(a, a, a, a);
}
#define FFX_BROADCAST_INT16(a)   FfxInt16(a)
#define FFX_BROADCAST_INT16X2(a) FfxInt16x2(FfxInt16(a))
#define FFX_BROADCAST_INT16X3(a) FfxInt16x3(FfxInt16(a))
#define FFX_BROADCAST_INT16X4(a) FfxInt16x4(FfxInt16(a))
//------------------------------------------------------------------------------------------------------------------------------
FfxUInt16 ffxBroadcastUInt16(FfxUInt16 a)
{
    return FfxUInt16(a);
}
FfxUInt16x2 ffxBroadcastUInt16x2(FfxUInt16 a)
{
    return FfxUInt16x2(a, a);
}
FfxUInt16x3 ffxBroadcastUInt16x3(FfxUInt16 a)
{
    return FfxUInt16x3(a, a, a);
}
FfxUInt16x4 ffxBroadcastUInt16x4(FfxUInt16 a)
{
    return FfxUInt16x4(a, a, a, a);
}
#define FFX_BROADCAST_UINT16(a)   FfxUInt16(a)
#define FFX_BROADCAST_UINT16X2(a) FfxUInt16x2(FfxUInt16(a))
#define FFX_BROADCAST_UINT16X3(a) FfxUInt16x3(FfxUInt16(a))
#define FFX_BROADCAST_UINT16X4(a) FfxUInt16x4(FfxUInt16(a))
//==============================================================================================================================
FfxUInt16 ffxAbsHalf(FfxUInt16 a)
{
    return FfxUInt16(abs(FfxInt16(a)));
}
FfxUInt16x2 ffxAbsHalf(FfxUInt16x2 a)
{
    return FfxUInt16x2(abs(FfxInt16x2(a)));
}
FfxUInt16x3 ffxAbsHalf(FfxUInt16x3 a)
{
    return FfxUInt16x3(abs(FfxInt16x3(a)));
}
FfxUInt16x4 ffxAbsHalf(FfxUInt16x4 a)
{
    return FfxUInt16x4(abs(FfxInt16x4(a)));
}
//------------------------------------------------------------------------------------------------------------------------------
FfxFloat16 ffxClampHalf(FfxFloat16 x, FfxFloat16 n, FfxFloat16 m)
{
    return clamp(x, n, m);
}
FfxFloat16x2 ffxClampHalf(FfxFloat16x2 x, FfxFloat16x2 n, FfxFloat16x2 m)
{
    return clamp(x, n, m);
}
FfxFloat16x3 ffxClampHalf(FfxFloat16x3 x, FfxFloat16x3 n, FfxFloat16x3 m)
{
    return clamp(x, n, m);
}
FfxFloat16x4 ffxClampHalf(FfxFloat16x4 x, FfxFloat16x4 n, FfxFloat16x4 m)
{
    return clamp(x, n, m);
}
//------------------------------------------------------------------------------------------------------------------------------
FfxFloat16 ffxFract(FfxFloat16 x)
{
    return fract(x);
}
FfxFloat16x2 ffxFract(FfxFloat16x2 x)
{
    return fract(x);
}
FfxFloat16x3 ffxFract(FfxFloat16x3 x)
{
    return fract(x);
}
FfxFloat16x4 ffxFract(FfxFloat16x4 x)
{
    return fract(x);
}
//------------------------------------------------------------------------------------------------------------------------------
FfxFloat16 ffxLerp(FfxFloat16 x, FfxFloat16 y, FfxFloat16 a)
{
    return mix(x, y, a);
}
FfxFloat16x2 ffxLerp(FfxFloat16x2 x, FfxFloat16x2 y, FfxFloat16 a)
{
    return mix(x, y, a);
}
FfxFloat16x2 ffxLerp(FfxFloat16x2 x, FfxFloat16x2 y, FfxFloat16x2 a)
{
    return mix(x, y, a);
}
FfxFloat16x3 ffxLerp(FfxFloat16x3 x, FfxFloat16x3 y, FfxFloat16x3 a)
{
    return mix(x, y, a);
}
FfxFloat16x3 ffxLerp(FfxFloat16x3 x, FfxFloat16x3 y, FfxFloat16 a)
{
    return mix(x, y, a);
}
FfxFloat16x4 ffxLerp(FfxFloat16x4 x, FfxFloat16x4 y, FfxFloat16 a)
{
    return mix(x, y, a);
}
FfxFloat16x4 ffxLerp(FfxFloat16x4 x, FfxFloat16x4 y, FfxFloat16x4 a)
{
    return mix(x, y, a);
}
//------------------------------------------------------------------------------------------------------------------------------
// No packed version of ffxMid3.
FfxFloat16 ffxMed3Half(FfxFloat16 x, FfxFloat16 y, FfxFloat16 z)
{
    return max(min(x, y), min(max(x, y), z));
}
FfxFloat16x2 ffxMed3Half(FfxFloat16x2 x, FfxFloat16x2 y, FfxFloat16x2 z)
{
    return max(min(x, y), min(max(x, y), z));
}
FfxFloat16x3 ffxMed3Half(FfxFloat16x3 x, FfxFloat16x3 y, FfxFloat16x3 z)
{
    return max(min(x, y), min(max(x, y), z));
}
FfxFloat16x4 ffxMed3Half(FfxFloat16x4 x, FfxFloat16x4 y, FfxFloat16x4 z)
{
    return max(min(x, y), min(max(x, y), z));
}
FfxInt16 ffxMed3Half(FfxInt16 x, FfxInt16 y, FfxInt16 z)
{
    return max(min(x, y), min(max(x, y), z));
}
FfxInt16x2 ffxMed3Half(FfxInt16x2 x, FfxInt16x2 y, FfxInt16x2 z)
{
    return max(min(x, y), min(max(x, y), z));
}
FfxInt16x3 ffxMed3Half(FfxInt16x3 x, FfxInt16x3 y, FfxInt16x3 z)
{
    return max(min(x, y), min(max(x, y), z));
}
FfxInt16x4 ffxMed3Half(FfxInt16x4 x, FfxInt16x4 y, FfxInt16x4 z)
{
    return max(min(x, y), min(max(x, y), z));
}
//------------------------------------------------------------------------------------------------------------------------------
// No packed version of ffxMax3.
FfxFloat16 ffxMax3Half(FfxFloat16 x, FfxFloat16 y, FfxFloat16 z)
{
    return max(x, max(y, z));
}
FfxFloat16x2 ffxMax3Half(FfxFloat16x2 x, FfxFloat16x2 y, FfxFloat16x2 z)
{
    return max(x, max(y, z));
}
FfxFloat16x3 ffxMax3Half(FfxFloat16x3 x, FfxFloat16x3 y, FfxFloat16x3 z)
{
    return max(x, max(y, z));
}
FfxFloat16x4 ffxMax3Half(FfxFloat16x4 x, FfxFloat16x4 y, FfxFloat16x4 z)
{
    return max(x, max(y, z));
}
//------------------------------------------------------------------------------------------------------------------------------
// No packed version of ffxMin3.
FfxFloat16 ffxMin3Half(FfxFloat16 x, FfxFloat16 y, FfxFloat16 z)
{
    return min(x, min(y, z));
}
FfxFloat16x2 ffxMin3Half(FfxFloat16x2 x, FfxFloat16x2 y, FfxFloat16x2 z)
{
    return min(x, min(y, z));
}
FfxFloat16x3 ffxMin3Half(FfxFloat16x3 x, FfxFloat16x3 y, FfxFloat16x3 z)
{
    return min(x, min(y, z));
}
FfxFloat16x4 ffxMin3Half(FfxFloat16x4 x, FfxFloat16x4 y, FfxFloat16x4 z)
{
    return min(x, min(y, z));
}
//------------------------------------------------------------------------------------------------------------------------------
FfxFloat16 ffxReciprocalHalf(FfxFloat16 x)
{
    return FFX_BROADCAST_FLOAT16(1.0) / x;
}
FfxFloat16x2 ffxReciprocalHalf(FfxFloat16x2 x)
{
    return FFX_BROADCAST_FLOAT16X2(1.0) / x;
}
FfxFloat16x3 ffxReciprocalHalf(FfxFloat16x3 x)
{
    return FFX_BROADCAST_FLOAT16X3(1.0) / x;
}
FfxFloat16x4 ffxReciprocalHalf(FfxFloat16x4 x)
{
    return FFX_BROADCAST_FLOAT16X4(1.0) / x;
}
//------------------------------------------------------------------------------------------------------------------------------
FfxFloat16 ffxReciprocalSquareRootHalf(FfxFloat16 x)
{
    return FFX_BROADCAST_FLOAT16(1.0) / sqrt(x);
}
FfxFloat16x2 ffxReciprocalSquareRootHalf(FfxFloat16x2 x)
{
    return FFX_BROADCAST_FLOAT16X2(1.0) / sqrt(x);
}
FfxFloat16x3 ffxReciprocalSquareRootHalf(FfxFloat16x3 x)
{
    return FFX_BROADCAST_FLOAT16X3(1.0) / sqrt(x);
}
FfxFloat16x4 ffxReciprocalSquareRootHalf(FfxFloat16x4 x)
{
    return FFX_BROADCAST_FLOAT16X4(1.0) / sqrt(x);
}
//------------------------------------------------------------------------------------------------------------------------------
FfxFloat16 ffxSaturate(FfxFloat16 x)
{
    return clamp(x, FFX_BROADCAST_FLOAT16(0.0), FFX_BROADCAST_FLOAT16(1.0));
}
FfxFloat16x2 ffxSaturate(FfxFloat16x2 x)
{
    return clamp(x, FFX_BROADCAST_FLOAT16X2(0.0), FFX_BROADCAST_FLOAT16X2(1.0));
}
FfxFloat16x3 ffxSaturate(FfxFloat16x3 x)
{
    return clamp(x, FFX_BROADCAST_FLOAT16X3(0.0), FFX_BROADCAST_FLOAT16X3(1.0));
}
FfxFloat16x4 ffxSaturate(FfxFloat16x4 x)
{
    return clamp(x, FFX_BROADCAST_FLOAT16X4(0.0), FFX_BROADCAST_FLOAT16X4(1.0));
}
//------------------------------------------------------------------------------------------------------------------------------
FfxUInt16 ffxBitShiftRightHalf(FfxUInt16 a, FfxUInt16 b)
{
    return FfxUInt16(FfxInt16(a) >> FfxInt16(b));
}
FfxUInt16x2 ffxBitShiftRightHalf(FfxUInt16x2 a, FfxUInt16x2 b)
{
    return FfxUInt16x2(FfxInt16x2(a) >> FfxInt16x2(b));
}
FfxUInt16x3 ffxBitShiftRightHalf(FfxUInt16x3 a, FfxUInt16x3 b)
{
    return FfxUInt16x3(FfxInt16x3(a) >> FfxInt16x3(b));
}
FfxUInt16x4 ffxBitShiftRightHalf(FfxUInt16x4 a, FfxUInt16x4 b)
{
    return FfxUInt16x4(FfxInt16x4(a) >> FfxInt16x4(b));
}
#endif // FFX_HALF

#if defined(FFX_WAVE)
// Where 'x' must be a compile time literal.
FfxFloat32 AWaveXorF1(FfxFloat32 v, FfxUInt32 x)
{
    return subgroupShuffleXor(v, x);
}
FfxFloat32x2 AWaveXorF2(FfxFloat32x2 v, FfxUInt32 x)
{
    return subgroupShuffleXor(v, x);
}
FfxFloat32x3 AWaveXorF3(FfxFloat32x3 v, FfxUInt32 x)
{
    return subgroupShuffleXor(v, x);
}
FfxFloat32x4 AWaveXorF4(FfxFloat32x4 v, FfxUInt32 x)
{
    return subgroupShuffleXor(v, x);
}
FfxUInt32 AWaveXorU1(FfxUInt32 v, FfxUInt32 x)
{
    return subgroupShuffleXor(v, x);
}
FfxUInt32x2 AWaveXorU2(FfxUInt32x2 v, FfxUInt32 x)
{
    return subgroupShuffleXor(v, x);
}
FfxUInt32x3 AWaveXorU3(FfxUInt32x3 v, FfxUInt32 x)
{
    return subgroupShuffleXor(v, x);
}
FfxUInt32x4 AWaveXorU4(FfxUInt32x4 v, FfxUInt32 x)
{
    return subgroupShuffleXor(v, x);
}

//------------------------------------------------------------------------------------------------------------------------------
#if FFX_HALF
FfxFloat16x2 ffxWaveXorFloat16x2(FfxFloat16x2 v, FfxUInt32 x)
{
    return FFX_UINT32_TO_FLOAT16X2(subgroupShuffleXor(FFX_FLOAT16X2_TO_UINT32(v), x));
}
FfxFloat16x4 ffxWaveXorFloat16x4(FfxFloat16x4 v, FfxUInt32 x)
{
    return FFX_UINT32X2_TO_FLOAT16X4(subgroupShuffleXor(FFX_FLOAT16X4_TO_UINT32X2(v), x));
}
FfxUInt16x2 ffxWaveXorUint16x2(FfxUInt16x2 v, FfxUInt32 x)
{
    return FFX_UINT32_TO_UINT16X2(subgroupShuffleXor(FFX_UINT16X2_TO_UINT32(v), x));
}
FfxUInt16x4 ffxWaveXorUint16x4(FfxUInt16x4 v, FfxUInt32 x)
{
    return FFX_UINT32X2_TO_UINT16X4(subgroupShuffleXor(FFX_UINT16X4_TO_UINT32X2(v), x));
}
#endif // FFX_HALF
#endif // #if defined(FFX_WAVE)
