// Special thanks to @Argon4W @InitAuther97 @竹若泠ねこ @moyongxin
// They provided a lot of help while I was writing this code.

// Require AVX2+FMA

#ifdef _WIN32
#define SR_EXPORT extern "C" __declspec(dllexport)
#else
#define SR_EXPORT extern "C" __attribute__((visibility("default")))
#endif

#if defined(__GNUC__) || defined(__clang__)
#define SR_FORCEINLINE inline __attribute__((always_inline))
#elif defined(_MSC_VER)
#define SR_FORCEINLINE __forceinline
#else
#define SR_FORCEINLINE inline
#endif

#include <cstring>
#include <cstdint>
#include <bit>
#include <immintrin.h>

namespace NormI8 {
    inline constexpr float COMPONENT_RANGE = 127.0f;
    inline constexpr float NORM = 0.007874016f; // 1/127

    namespace simd {
        [[nodiscard]] SR_FORCEINLINE __m128 unpack(std::int32_t norm) noexcept {
            const __m128i packed = _mm_cvtsi32_si128(norm);
            const __m128i wide = _mm_cvtepi8_epi32(packed);
            return _mm_mul_ps(_mm_cvtepi32_ps(wide), _mm_set1_ps(NORM));
        }

        [[nodiscard]] SR_FORCEINLINE std::int32_t pack(__m128 v) noexcept {
            const __m128i i32 = _mm_cvttps_epi32(_mm_mul_ps(v, _mm_set1_ps(COMPONENT_RANGE)));
            const __m128i i16 = _mm_packs_epi32(i32, i32);
            const __m128i i8 = _mm_packs_epi16(i16, i16);
            return _mm_cvtsi128_si32(i8);
        }
    } // namespace simd
} // namespace NormI8

namespace detail {
    SR_FORCEINLINE __m128 fma(__m128 a, __m128 b, __m128 c) noexcept {
        #if defined(__FMA__)
        return _mm_fmadd_ps(a, b, c);
        #else
        return _mm_add_ps(_mm_mul_ps(a, b), c);
        #endif
    }

    SR_FORCEINLINE __m128 cross3(__m128 a, __m128 b) noexcept {
        const __m128 a_yzx = _mm_shuffle_ps(a, a, _MM_SHUFFLE(3, 0, 2, 1));
        const __m128 b_yzx = _mm_shuffle_ps(b, b, _MM_SHUFFLE(3, 0, 2, 1));
        const __m128 c = _mm_sub_ps(_mm_mul_ps(a, b_yzx), _mm_mul_ps(a_yzx, b));
        return _mm_shuffle_ps(c, c, _MM_SHUFFLE(3, 0, 2, 1));
    }

    SR_FORCEINLINE __m128 rsqrtNR(__m128 x) noexcept {
        __m128 r = _mm_rsqrt_ps(x);
        const __m128 halfX = _mm_mul_ps(x, _mm_set1_ps(0.5f));
        const __m128 threeHalf = _mm_set1_ps(1.5f);
        r = _mm_mul_ps(r, _mm_sub_ps(threeHalf, _mm_mul_ps(halfX, _mm_mul_ps(r, r))));
        const __m128 isZero = _mm_cmpeq_ps(x, _mm_setzero_ps());
        return _mm_blendv_ps(r, _mm_set1_ps(1.0f), isZero);
    }

    SR_FORCEINLINE __m256 rsqrtNR(__m256 x) noexcept {
        __m256 r = _mm256_rsqrt_ps(x);
        const __m256 halfX = _mm256_mul_ps(x, _mm256_set1_ps(0.5f));
        const __m256 threeHalf = _mm256_set1_ps(1.5f);
        r = _mm256_mul_ps(r, _mm256_sub_ps(threeHalf, _mm256_mul_ps(halfX, _mm256_mul_ps(r, r))));
        const __m256 isZero = _mm256_cmp_ps(x, _mm256_setzero_ps(), _CMP_EQ_OQ);
        return _mm256_blendv_ps(r, _mm256_set1_ps(1.0f), isZero);
    }

    SR_FORCEINLINE __m128 loadVec2(const std::uint8_t *p) noexcept {
        return _mm_castpd_ps(_mm_load_sd(reinterpret_cast<const double *>(p)));
    }
} // namespace detail

[[nodiscard]] static SR_FORCEINLINE std::int32_t computeTangent(
    __m128 normal,
    __m128 pos0, __m128 uv0,
    __m128 pos1, __m128 uv1,
    __m128 pos2, __m128 uv2) noexcept {
    const __m128 edge1 = _mm_sub_ps(pos1, pos0);
    const __m128 edge2 = _mm_sub_ps(pos2, pos0);
    const __m128 duv1 = _mm_sub_ps(uv1, uv0); // (dU1, dV1, ?, ?)
    const __m128 duv2 = _mm_sub_ps(uv2, uv0); // (dU2, dV2, ?, ?)

    const __m128 duv2s = _mm_shuffle_ps(duv2, duv2, _MM_SHUFFLE(3, 2, 0, 1)); // (dV2, dU2, ...)
    const __m128 duv1s = _mm_shuffle_ps(duv1, duv1, _MM_SHUFFLE(3, 2, 0, 1)); // (dV1, dU1, ...)
    const __m128 fden = _mm_sub_ps(_mm_mul_ps(duv1, duv2s), _mm_mul_ps(duv1s, duv2));
    __m128 f = _mm_div_ss(_mm_set_ss(1.0f), fden);
    f = _mm_blendv_ps(f, _mm_set_ss(1.0f), _mm_cmpeq_ss(fden, _mm_setzero_ps()));
    const __m128 fv = _mm_shuffle_ps(f, f, 0x00);

    const __m128 dV2 = _mm_shuffle_ps(duv2, duv2, _MM_SHUFFLE(1, 1, 1, 1));
    const __m128 dV1 = _mm_shuffle_ps(duv1, duv1, _MM_SHUFFLE(1, 1, 1, 1));
    const __m128 tgRaw = _mm_mul_ps(fv, _mm_sub_ps(_mm_mul_ps(dV2, edge1), _mm_mul_ps(dV1, edge2)));
    __m128 tg = _mm_mul_ps(tgRaw, detail::rsqrtNR(_mm_dp_ps(tgRaw, tgRaw, 0x7F)));

    if ((_mm_movemask_ps(_mm_cmpeq_ps(tg, _mm_setzero_ps())) & 0x7) == 0x7)
        return -1;

    const __m128 dU1 = _mm_shuffle_ps(duv1, duv1, _MM_SHUFFLE(0, 0, 0, 0));
    const __m128 dU2 = _mm_shuffle_ps(duv2, duv2, _MM_SHUFFLE(0, 0, 0, 0));
    const __m128 bt = _mm_mul_ps(fv, _mm_sub_ps(_mm_mul_ps(dU1, edge2), _mm_mul_ps(dU2, edge1)));

    const __m128 pb = detail::cross3(tgRaw, normal);
    const __m128 dt = _mm_dp_ps(bt, pb, 0x7F);
    const __m128 wv = _mm_or_ps(
        _mm_and_ps(_mm_cmplt_ps(dt, _mm_setzero_ps()),
                   _mm_castsi128_ps(_mm_set1_epi32((std::int32_t) 0x80000000))),
        _mm_set1_ps(1.0f));
    return NormI8::simd::pack(_mm_blend_ps(tg, wv, 0x8));
}

static SR_FORCEINLINE void computeTangent2(
    std::int32_t &tgOutA, std::int32_t &tgOutB,
    std::int32_t nrmA, std::int32_t nrmB,
    __m128 p0A, __m128 t0A, __m128 p1A, __m128 t1A, __m128 p2A, __m128 t2A,
    __m128 p0B, __m128 t0B, __m128 p1B, __m128 t1B, __m128 p2B, __m128 t2B) noexcept {
    const __m256 P0 = _mm256_insertf128_ps(_mm256_castps128_ps256(p0A), p0B, 1);
    const __m256 P1 = _mm256_insertf128_ps(_mm256_castps128_ps256(p1A), p1B, 1);
    const __m256 P2 = _mm256_insertf128_ps(_mm256_castps128_ps256(p2A), p2B, 1);
    const __m256 T0 = _mm256_insertf128_ps(_mm256_castps128_ps256(t0A), t0B, 1);
    const __m256 T1 = _mm256_insertf128_ps(_mm256_castps128_ps256(t1A), t1B, 1);
    const __m256 T2 = _mm256_insertf128_ps(_mm256_castps128_ps256(t2A), t2B, 1);

    const __m256 E1 = _mm256_sub_ps(P1, P0);
    const __m256 E2 = _mm256_sub_ps(P2, P0);
    const __m256 DU1 = _mm256_sub_ps(T1, T0);
    const __m256 DU2 = _mm256_sub_ps(T2, T0);

    const __m256 DU2s = _mm256_shuffle_ps(DU2, DU2, _MM_SHUFFLE(3, 2, 0, 1));
    const __m256 DU1s = _mm256_shuffle_ps(DU1, DU1, _MM_SHUFFLE(3, 2, 0, 1));
    const __m256 fden = _mm256_sub_ps(_mm256_mul_ps(DU1, DU2s), _mm256_mul_ps(DU1s, DU2));
    __m256 f = _mm256_rcp_ps(fden);
    f = _mm256_blendv_ps(f, _mm256_set1_ps(1.0f), _mm256_cmp_ps(fden, _mm256_setzero_ps(), _CMP_EQ_OQ));
    const __m256 fv = _mm256_permutevar8x32_ps(f, _mm256_setr_epi32(0, 0, 0, 0, 4, 4, 4, 4));

    const __m256i idxV = _mm256_setr_epi32(1, 1, 1, 1, 5, 5, 5, 5);
    const __m256i idxU = _mm256_setr_epi32(0, 0, 0, 0, 4, 4, 4, 4);
    const __m256 dV2 = _mm256_permutevar8x32_ps(DU2, idxV);
    const __m256 dV1 = _mm256_permutevar8x32_ps(DU1, idxV);
    const __m256 dU1 = _mm256_permutevar8x32_ps(DU1, idxU);
    const __m256 dU2 = _mm256_permutevar8x32_ps(DU2, idxU);

    const __m256 tgRaw = _mm256_mul_ps(fv, _mm256_sub_ps(_mm256_mul_ps(dV2, E1), _mm256_mul_ps(dV1, E2)));
    __m256 tg = _mm256_mul_ps(tgRaw, detail::rsqrtNR(_mm256_dp_ps(tgRaw, tgRaw, 0x7F)));

    const __m256 bt = _mm256_mul_ps(fv, _mm256_sub_ps(_mm256_mul_ps(dU1, E2), _mm256_mul_ps(dU2, E1)));

    const __m128i nb = _mm_insert_epi32(_mm_cvtsi32_si128(nrmA), nrmB, 1);
    const __m256 nrm = _mm256_mul_ps(
        _mm256_cvtepi32_ps(_mm256_cvtepi8_epi32(nb)), _mm256_set1_ps(NormI8::NORM));

    const __m256 a_yzx = _mm256_shuffle_ps(tgRaw, tgRaw, _MM_SHUFFLE(3, 0, 2, 1));
    const __m256 b_yzx = _mm256_shuffle_ps(nrm, nrm, _MM_SHUFFLE(3, 0, 2, 1));
    const __m256 cx = _mm256_sub_ps(_mm256_mul_ps(tgRaw, b_yzx), _mm256_mul_ps(a_yzx, nrm));
    const __m256 pb = _mm256_shuffle_ps(cx, cx, _MM_SHUFFLE(3, 0, 2, 1));

    const __m256 dt = _mm256_dp_ps(bt, pb, 0x7F);
    const __m256 wv = _mm256_or_ps(
        _mm256_and_ps(_mm256_cmp_ps(dt, _mm256_setzero_ps(), _CMP_LT_OQ),
                      _mm256_castsi256_ps(_mm256_set1_epi32((std::int32_t) 0x80000000))),
        _mm256_set1_ps(1.0f));
    const __m256 tg4 = _mm256_blend_ps(tg, wv, 0x88);

    const __m256i i32 = _mm256_cvttps_epi32(_mm256_mul_ps(tg4, _mm256_set1_ps(NormI8::COMPONENT_RANGE)));
    const __m256i i16 = _mm256_packs_epi32(i32, i32);
    const __m256i i8 = _mm256_packs_epi16(i16, i16);
    std::int32_t tgA = _mm256_cvtsi256_si32(i8);
    std::int32_t tgB = _mm256_extract_epi32(i8, 4);

    const std::int32_t zm = _mm256_movemask_ps(_mm256_cmp_ps(tg, _mm256_setzero_ps(), _CMP_EQ_OQ));
    if ((zm & 0x7) == 0x7) tgA = -1;
    if (((zm >> 4) & 0x7) == 0x7) tgB = -1;
    tgOutA = tgA;
    tgOutB = tgB;
}

static SR_FORCEINLINE void pairTgMeta(const std::uint8_t *qa, const std::uint8_t *qb,
                                      __m256 packedV2,
                                      std::int32_t &tgA, std::int32_t &tgB,
                                      __m128 &loMetaA, __m128 &loMetaB) noexcept {
    constexpr int SS = 36;
    const __m128 p0A = _mm_loadu_ps(reinterpret_cast<const float *>(qa));
    const __m128 p1A = _mm_loadu_ps(reinterpret_cast<const float *>(qa + SS));
    const __m128 p2A = _mm_loadu_ps(reinterpret_cast<const float *>(qa + 2 * SS));
    const __m128 p0B = _mm_loadu_ps(reinterpret_cast<const float *>(qb));
    const __m128 p1B = _mm_loadu_ps(reinterpret_cast<const float *>(qb + SS));
    const __m128 p2B = _mm_loadu_ps(reinterpret_cast<const float *>(qb + 2 * SS));
    const __m128 t0A = detail::loadVec2(qa + 16);
    const __m128 t1A = detail::loadVec2(qa + SS + 16);
    const __m128 t2A = detail::loadVec2(qa + 2 * SS + 16);
    const __m128 t3A = detail::loadVec2(qa + 3 * SS + 16);
    const __m128 t0B = detail::loadVec2(qb + 16);
    const __m128 t1B = detail::loadVec2(qb + SS + 16);
    const __m128 t2B = detail::loadVec2(qb + 2 * SS + 16);
    const __m128 t3B = detail::loadVec2(qb + 3 * SS + 16);

    std::int32_t nrmA, nrmB;
    std::memcpy(&nrmA, qa + 32, 4);
    std::memcpy(&nrmB, qb + 32, 4);
    computeTangent2(tgA, tgB, nrmA, nrmB,
                    p0A, t0A, p1A, t1A, p2A, t2A,
                    p0B, t0B, p1B, t1B, p2B, t2B);

    const __m256 T0 = _mm256_insertf128_ps(_mm256_castps128_ps256(t0A), t0B, 1);
    const __m256 T1 = _mm256_insertf128_ps(_mm256_castps128_ps256(t1A), t1B, 1);
    const __m256 T2 = _mm256_insertf128_ps(_mm256_castps128_ps256(t2A), t2B, 1);
    const __m256 T3 = _mm256_insertf128_ps(_mm256_castps128_ps256(t3A), t3B, 1);
    const __m256 midUV = _mm256_mul_ps(
        _mm256_add_ps(_mm256_add_ps(_mm256_add_ps(T0, T1), T2), T3), _mm256_set1_ps(0.25f));
    const __m256 loMeta2 = _mm256_shuffle_ps(packedV2, midUV, _MM_SHUFFLE(1, 0, 1, 0));
    loMetaA = _mm256_castps256_ps128(loMeta2);
    loMetaB = _mm256_extractf128_ps(loMeta2, 1);
}

template<bool VEL>
static SR_FORCEINLINE void storeQuad(const std::uint8_t *qs, std::uint8_t *qd,
                                     __m128 loMeta, std::int32_t tangent,
                                     __m128 c0, __m128 c1, __m128 c2, __m128 tr) noexcept {
    constexpr int SS = 36, DS = 68;
    const __m128 tgV = _mm_castsi128_ps(_mm_set1_epi32(tangent));
    const __m128 tailC = _mm_castsi128_ps(_mm_cvtsi32_si128(tangent)); // (tg, 0, 0, 0)

    const std::uint8_t *ws = qs;
    std::uint8_t *wd = qd;
    #pragma GCC unroll 4
    for (int i = 0; i < 4; ++i) {
        const __m256i a = _mm256_loadu_si256(reinterpret_cast<const __m256i *>(ws));
        _mm256_storeu_si256(reinterpret_cast<__m256i *>(wd), a);

        std::int32_t nd;
        std::memcpy(&nd, ws + 32, 4);
        std::memcpy(wd + 32, &nd, 4);

        __m128 tail;
        if constexpr (VEL) {
            const __m128 bx = _mm_broadcast_ss(reinterpret_cast<const float *>(ws + 0));
            const __m128 by = _mm_broadcast_ss(reinterpret_cast<const float *>(ws + 4));
            const __m128 bz = _mm_broadcast_ss(reinterpret_cast<const float *>(ws + 8));
            const __m128 vel = detail::fma(bx, c0, detail::fma(by, c1, detail::fma(bz, c2, tr)));
            tail = _mm_move_ss(_mm_shuffle_ps(vel, vel, _MM_SHUFFLE(2, 1, 0, 3)), tgV); // (tg, vx, vy, vz)
        } else {
            tail = tailC;
        }
        const __m256 mt = _mm256_insertf128_ps(_mm256_castps128_ps256(loMeta), tail, 1);
        _mm256_storeu_ps(reinterpret_cast<float *>(wd + 36), mt);

        ws += SS;
        wd += DS;
    }
}

template<bool VEL>
static void serImpl(const std::uint8_t *src, std::uint8_t *dst, std::int32_t quadCount,
                    std::uint64_t packedShorts, const float *deltaMatrix) noexcept {
    constexpr int SS = 36, DS = 68;

    const __m128 packedV = _mm_castsi128_ps(_mm_cvtsi64_si128((std::int64_t) packedShorts)); // [psLo, psHi, 0, 0]
    const __m256 packedV2 = _mm256_insertf128_ps(_mm256_castps128_ps256(packedV), packedV, 1);

    __m128 c0 = _mm_setzero_ps(), c1 = _mm_setzero_ps(), c2 = _mm_setzero_ps(), tr = _mm_setzero_ps();
    if constexpr (VEL) {
        const __m128 m3 = _mm_castsi128_ps(_mm_set_epi32(0, -1, -1, -1));
        c0 = _mm_and_ps(_mm_loadu_ps(deltaMatrix + 0), m3); // (m00, m01, m02, 0)
        c1 = _mm_and_ps(_mm_loadu_ps(deltaMatrix + 3), m3); // (m10, m11, m12, 0)
        c2 = _mm_and_ps(_mm_loadu_ps(deltaMatrix + 6), m3); // (m20, m21, m22, 0)
        tr = _mm_castpd_ps(_mm_load_sd(reinterpret_cast<const double *>(deltaMatrix + 9)));
        tr = _mm_insert_ps(tr, _mm_load_ss(deltaMatrix + 11), 0x20); // (m30, m31, m32, 0)
    }

    const std::uint8_t *sp = src;
    std::uint8_t *dp = dst;
    std::int32_t q = 0;

    for (; q + 2 <= quadCount; q += 2) {
        const std::uint8_t *qa = sp;
        const std::uint8_t *qb = sp + 4 * SS;

        std::int32_t tgA, tgB;
        __m128 loMetaA, loMetaB;
        pairTgMeta(qa, qb, packedV2, tgA, tgB, loMetaA, loMetaB);

        storeQuad<VEL>(qa, dp, loMetaA, tgA, c0, c1, c2, tr);
        storeQuad<VEL>(qb, dp + 4 * DS, loMetaB, tgB, c0, c1, c2, tr);

        sp += 2 * 4 * SS;
        dp += 2 * 4 * DS;
    }

    for (; q < quadCount; ++q) {
        const std::uint8_t *v0 = sp;
        const __m128 p0 = _mm_loadu_ps(reinterpret_cast<const float *>(v0));
        const __m128 p1 = _mm_loadu_ps(reinterpret_cast<const float *>(v0 + SS));
        const __m128 p2 = _mm_loadu_ps(reinterpret_cast<const float *>(v0 + 2 * SS));
        const __m128 t0 = detail::loadVec2(v0 + 16);
        const __m128 t1 = detail::loadVec2(v0 + SS + 16);
        const __m128 t2 = detail::loadVec2(v0 + 2 * SS + 16);
        const __m128 t3 = detail::loadVec2(v0 + 3 * SS + 16);

        std::int32_t nrm;
        std::memcpy(&nrm, v0 + 32, 4);

        const std::int32_t tangent = computeTangent(NormI8::simd::unpack(nrm), p0, t0, p1, t1, p2, t2);

        const __m128 midUV = _mm_mul_ps(
            _mm_add_ps(_mm_add_ps(_mm_add_ps(t0, t1), t2), t3), _mm_set1_ps(0.25f));
        const __m128 loMeta = _mm_shuffle_ps(packedV, midUV, _MM_SHUFFLE(1, 0, 1, 0));

        storeQuad<VEL>(sp, dp, loMeta, tangent, c0, c1, c2, tr);

        sp += 4 * SS;
        dp += 4 * DS;
    }
}

SR_EXPORT void _superFastModelToEntityVertexSerializer(
    std::int64_t srcBase,
    std::int64_t dstBase,
    std::int32_t vertexCount,
    std::int16_t entity,
    std::int16_t blockEntity,
    std::int16_t item,
    const float *deltaMatrix
) noexcept {
    const auto *src = reinterpret_cast<const std::uint8_t *>(srcBase);
    auto *dst = reinterpret_cast<std::uint8_t *>(dstBase);
    if (!src || !dst || vertexCount <= 0) return;

    const std::int32_t quadCount = vertexCount >> 2;
    const std::uint64_t packedShorts =
            (static_cast<std::uint64_t>(static_cast<std::uint16_t>(entity)) & 0xFFFFull)
            | ((static_cast<std::uint64_t>(static_cast<std::uint16_t>(blockEntity)) & 0xFFFFull) << 16)
            | ((static_cast<std::uint64_t>(static_cast<std::uint16_t>(item)) & 0xFFFFull) << 32);

    if (deltaMatrix) {
        serImpl<true>(src, dst, quadCount, packedShorts, deltaMatrix);
    } else {
        serImpl<false>(src, dst, quadCount, packedShorts, deltaMatrix);
    }
}
