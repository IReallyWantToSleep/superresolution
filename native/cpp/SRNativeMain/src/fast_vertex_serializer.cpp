// Special thanks to @Argon4W @InitAuther97 @竹若泠ねこ
// Them help me 

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
#include <algorithm>
#include <immintrin.h>

namespace NormI8 {
    inline constexpr int X_COMPONENT_OFFSET = 0;
    inline constexpr int Y_COMPONENT_OFFSET = 8;
    inline constexpr int Z_COMPONENT_OFFSET = 16;
    inline constexpr int W_COMPONENT_OFFSET = 24;
    inline constexpr float COMPONENT_RANGE = 127.0f;
    inline constexpr float NORM = 0.007874016f; // 1/127

    [[nodiscard]] constexpr std::int32_t pack(float x, float y, float z, float w) noexcept {
        return (static_cast<std::int32_t>(x * 127.0f) & 255)
               | ((static_cast<std::int32_t>(y * 127.0f) & 255) << 8)
               | ((static_cast<std::int32_t>(z * 127.0f) & 255) << 16)
               | ((static_cast<std::int32_t>(w * 127.0f) & 255) << 24);
    }

    [[nodiscard]] constexpr std::int32_t pack(const float *normal) noexcept {
        return pack(normal[0], normal[1], normal[2], 0.0f);
    }

    [[nodiscard]] constexpr std::int32_t pack(const float *normal, float w) noexcept {
        return pack(normal[0], normal[1], normal[2], w);
    }

    [[nodiscard]] constexpr std::int8_t toByte(float v) noexcept {
        return static_cast<std::int8_t>(static_cast<std::int32_t>(v * 127.0f) & 255);
    }

    [[nodiscard]] constexpr std::int32_t packColor(float x, float y, float z, float w) noexcept {
        return (static_cast<std::int32_t>(x * 127.0f) & 255)
               | ((static_cast<std::int32_t>(y * 127.0f) & 255) << 8)
               | ((static_cast<std::int32_t>(z * 127.0f) & 255) << 16)
               | ((static_cast<std::int32_t>(w) & 255) << 24);
    }

    [[nodiscard]] constexpr std::int32_t encode(float comp) noexcept {
        return static_cast<std::int32_t>(std::clamp(comp, -1.0f, 1.0f) * 127.0f) & 255;
    }

    [[nodiscard]] constexpr int signedByte(std::int32_t bits) noexcept {
        const auto raw = static_cast<std::uint32_t>(bits) & 0xFFu;
        return raw < 0x80u ? static_cast<int>(raw) : static_cast<int>(raw) - 256;
    }

    [[nodiscard]] constexpr float unpackX(std::int32_t norm) noexcept {
        return static_cast<float>(signedByte(norm >> X_COMPONENT_OFFSET)) * NORM;
    }

    [[nodiscard]] constexpr float unpackY(std::int32_t norm) noexcept {
        return static_cast<float>(signedByte(norm >> Y_COMPONENT_OFFSET)) * NORM;
    }

    [[nodiscard]] constexpr float unpackZ(std::int32_t norm) noexcept {
        return static_cast<float>(signedByte(norm >> Z_COMPONENT_OFFSET)) * NORM;
    }

    [[nodiscard]] constexpr float unpackW(std::int32_t norm) noexcept {
        return static_cast<float>(signedByte(norm >> W_COMPONENT_OFFSET)) * NORM;
    }

    inline void unpack(std::int32_t norm, float *out) noexcept {
        out[0] = unpackX(norm);
        out[1] = unpackY(norm);
        out[2] = unpackZ(norm);
    }

    inline void unpack4(std::int32_t norm, float *out) noexcept {
        out[0] = unpackX(norm);
        out[1] = unpackY(norm);
        out[2] = unpackZ(norm);
        out[3] = unpackW(norm);
    }

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
    struct VelocityMatrix {
        __m128 c0;
        __m128 c1;
        __m128 c2;
        __m128 c3;
    };

    SR_FORCEINLINE VelocityMatrix loadVelocityMatrix(const float *matrix) noexcept {
        return {
                _mm_loadu_ps(matrix),
                _mm_loadu_ps(matrix + 4),
                _mm_loadu_ps(matrix + 8),
                _mm_loadu_ps(matrix + 12)
        };
    }

    SR_FORCEINLINE __m128 fma(__m128 a, __m128 b, __m128 c) noexcept {
        #if defined(__FMA__)
        return _mm_fmadd_ps(a, b, c);
        #else
        return _mm_add_ps(_mm_mul_ps(a, b), c);
        #endif
    }

    template<int Lane>
    SR_FORCEINLINE __m128 transformComponent(
            const VelocityMatrix &matrix,
            __m128 px,
            __m128 py,
            __m128 pz
    ) noexcept {
        constexpr int SHUFFLE = Lane == 0 ? 0x00 : (Lane == 1 ? 0x55 : 0xAA);
        const __m128 m0 = _mm_shuffle_ps(matrix.c0, matrix.c0, SHUFFLE);
        const __m128 m1 = _mm_shuffle_ps(matrix.c1, matrix.c1, SHUFFLE);
        const __m128 m2 = _mm_shuffle_ps(matrix.c2, matrix.c2, SHUFFLE);
        const __m128 m3 = _mm_shuffle_ps(matrix.c3, matrix.c3, SHUFFLE);
        return fma(m0, px, fma(m1, py, fma(m2, pz, m3)));
    }

    SR_FORCEINLINE void storeStrided(
            std::uint8_t *dst,
            std::int32_t offset,
            __m128 values
    ) noexcept {
        _mm_store_ss(reinterpret_cast<float *>(dst + offset), values);
        values = _mm_castsi128_ps(_mm_srli_si128(_mm_castps_si128(values), 4));
        _mm_store_ss(reinterpret_cast<float *>(dst + offset + 68), values);
        values = _mm_castsi128_ps(_mm_srli_si128(_mm_castps_si128(values), 4));
        _mm_store_ss(reinterpret_cast<float *>(dst + offset + 136), values);
        values = _mm_castsi128_ps(_mm_srli_si128(_mm_castps_si128(values), 4));
        _mm_store_ss(reinterpret_cast<float *>(dst + offset + 204), values);
    }

    SR_FORCEINLINE __m128 cross3(__m128 a, __m128 b) noexcept {
        const __m128 a_yzx = _mm_shuffle_ps(a, a, _MM_SHUFFLE(3, 0, 2, 1));
        const __m128 b_yzx = _mm_shuffle_ps(b, b, _MM_SHUFFLE(3, 0, 2, 1));
        const __m128 c = _mm_sub_ps(_mm_mul_ps(a, b_yzx), _mm_mul_ps(a_yzx, b));
        return _mm_shuffle_ps(c, c, _MM_SHUFFLE(3, 0, 2, 1));
    }

    SR_FORCEINLINE __m128 rsqrtAccurate(__m128 lengthSq) noexcept {
        const __m128 isZero = _mm_cmpeq_ps(lengthSq, _mm_setzero_ps());
        const __m128 safe = _mm_blendv_ps(lengthSq, _mm_set1_ps(1.0f), isZero);
        const __m128 r = _mm_div_ps(_mm_set1_ps(1.0f), _mm_sqrt_ps(safe));
        return _mm_blendv_ps(r, _mm_set1_ps(1.0f), isZero);
    }

    SR_FORCEINLINE __m128 loadVec3(const std::uint8_t *p) noexcept {
        const __m128 raw = _mm_loadu_ps(reinterpret_cast<const float *>(p));
        return _mm_insert_ps(raw, _mm_setzero_ps(), 0x38); // zero lane 3
    }

    SR_FORCEINLINE __m128 loadVec2(const std::uint8_t *p) noexcept {
        return _mm_castpd_ps(_mm_load_sd(reinterpret_cast<const double *>(p)));
    }
} // namespace detail

[[nodiscard]] static SR_FORCEINLINE std::int32_t computeTangentFast(
    float *output,
    __m128 normal,
    __m128 pos0, __m128 uv0,
    __m128 pos1, __m128 uv1,
    __m128 pos2, __m128 uv2
) noexcept {
    using namespace detail;

    const __m128 edge1 = _mm_sub_ps(pos1, pos0);
    const __m128 edge2 = _mm_sub_ps(pos2, pos0);
    const __m128 deltaUV1 = _mm_sub_ps(uv1, uv0); // (dU1, dV1, 0, 0)
    const __m128 deltaUV2 = _mm_sub_ps(uv2, uv0); // (dU2, dV2, 0, 0)

    const float deltaU1 = _mm_cvtss_f32(deltaUV1);
    const float deltaV1 = _mm_cvtss_f32(_mm_shuffle_ps(deltaUV1, deltaUV1, _MM_SHUFFLE(1, 1, 1, 1)));
    const float deltaU2 = _mm_cvtss_f32(deltaUV2);
    const float deltaV2 = _mm_cvtss_f32(_mm_shuffle_ps(deltaUV2, deltaUV2, _MM_SHUFFLE(1, 1, 1, 1)));

    const float fdenom = deltaU1 * deltaV2 - deltaU2 * deltaV1;
    const float f = (fdenom == 0.0f) ? 1.0f : 1.0f / fdenom;

    const __m128 fVec = _mm_set1_ps(f);
    const __m128 deltaV1V = _mm_set1_ps(deltaV1);
    const __m128 deltaV2V = _mm_set1_ps(deltaV2);
    const __m128 deltaU1V = _mm_set1_ps(deltaU1);
    const __m128 deltaU2V = _mm_set1_ps(deltaU2);

    __m128 tangent = _mm_mul_ps(fVec, _mm_sub_ps(_mm_mul_ps(deltaV2V, edge1), _mm_mul_ps(deltaV1V, edge2)));
    tangent = _mm_mul_ps(tangent, rsqrtAccurate(_mm_dp_ps(tangent, tangent, 0x7F)));

    if ((_mm_movemask_ps(_mm_cmpeq_ps(tangent, _mm_setzero_ps())) & 0x7) == 0x7) {
        return -1;
    }

    // Only the handedness is needed from the bitangent. Normalizing it does
    // not change the sign of the dot product below.
    const __m128 bitangent =
            _mm_mul_ps(fVec, _mm_sub_ps(_mm_mul_ps(deltaU1V, edge2), _mm_mul_ps(deltaU2V, edge1)));

    const __m128 pbitangent = cross3(tangent, normal);
    const float dot = _mm_cvtss_f32(_mm_dp_ps(bitangent, pbitangent, 0x7F));
    const float tangentW = (dot < 0.0f) ? -1.0f : 1.0f;

    const __m128 tangent4 = _mm_insert_ps(tangent, _mm_set_ss(tangentW), 0x30);

    if (output != nullptr) {
        _mm_storeu_ps(output, tangent4);
    }

    return NormI8::simd::pack(tangent4);
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
    constexpr int MIDCOORD = 44;
    constexpr int TANGENT = 52;
    constexpr int VELOCITY = 56;
    constexpr int DST_STRIDE = 68;
    constexpr int SRC_STRIDE = 36;

    const auto *src = reinterpret_cast<const std::uint8_t *>(srcBase);
    auto *dst = reinterpret_cast<std::uint8_t *>(dstBase);
    if (!src || !dst || vertexCount <= 0) return;

    const std::int32_t quadCount = vertexCount >> 2;
    const bool shouldCalculateVelocity = (deltaMatrix != nullptr);

    const std::uint64_t packedShorts =
            (static_cast<std::uint64_t>(static_cast<std::uint16_t>(entity)) & 0xFFFFull)
            | ((static_cast<std::uint64_t>(static_cast<std::uint16_t>(blockEntity)) & 0xFFFFull) << 16)
            | ((static_cast<std::uint64_t>(static_cast<std::uint16_t>(item)) & 0xFFFFull) << 32);

    detail::VelocityMatrix matrix;
    if (shouldCalculateVelocity) {
        matrix = detail::loadVelocityMatrix(deltaMatrix);
    }

    std::int64_t srcOff = 0;
    std::int64_t dstOff = 0;

    for (std::int32_t q = 0; q < quadCount; ++q) {
        const std::uint8_t *v0 = src + srcOff;
        const std::uint8_t *v1 = v0 + SRC_STRIDE;
        const std::uint8_t *v2 = v1 + SRC_STRIDE;
        const std::uint8_t *v3 = v2 + SRC_STRIDE;

        std::int32_t packedNormal;
        std::memcpy(&packedNormal, v0 + 32, 4);
        const __m128 normal = NormI8::simd::unpack(packedNormal);

        const __m128 pos0 = detail::loadVec3(v0);
        const __m128 pos1 = detail::loadVec3(v1);
        const __m128 pos2 = detail::loadVec3(v2);
        const __m128 pos3 = shouldCalculateVelocity ? detail::loadVec3(v3) : _mm_setzero_ps();
        const __m128 uv0 = detail::loadVec2(v0 + 16);
        const __m128 uv1 = detail::loadVec2(v1 + 16);
        const __m128 uv2 = detail::loadVec2(v2 + 16);

        const std::int32_t tangent = computeTangentFast(
            nullptr, normal, pos0, uv0, pos1, uv1, pos2, uv2);

        const __m128 uv3 = detail::loadVec2(v3 + 16);
        const float midU = (
                _mm_cvtss_f32(uv0)
                + _mm_cvtss_f32(uv1)
                + _mm_cvtss_f32(uv2)
                + _mm_cvtss_f32(uv3)
        ) * 0.25f;
        const float midV = (
                _mm_cvtss_f32(_mm_shuffle_ps(uv0, uv0, _MM_SHUFFLE(1, 1, 1, 1)))
                + _mm_cvtss_f32(_mm_shuffle_ps(uv1, uv1, _MM_SHUFFLE(1, 1, 1, 1)))
                + _mm_cvtss_f32(_mm_shuffle_ps(uv2, uv2, _MM_SHUFFLE(1, 1, 1, 1)))
                + _mm_cvtss_f32(_mm_shuffle_ps(uv3, uv3, _MM_SHUFFLE(1, 1, 1, 1)))
        ) * 0.25f;

        std::int64_t ws = srcOff;
        std::int64_t wd = dstOff;

        for (int i = 0; i < 4; ++i) {
            _mm_storeu_si128(reinterpret_cast<__m128i *>(dst + wd),
                             _mm_loadu_si128(reinterpret_cast<const __m128i *>(src + ws)));
            _mm_storeu_si128(reinterpret_cast<__m128i *>(dst + wd + 16),
                             _mm_loadu_si128(reinterpret_cast<const __m128i *>(src + ws + 16)));
            std::memcpy(dst + wd + 32, src + ws + 32, 4);

            std::memcpy(dst + wd + 36, &packedShorts, 8);
            std::memcpy(dst + wd + MIDCOORD, &midU, 4);
            std::memcpy(dst + wd + MIDCOORD + 4, &midV, 4);
            std::memcpy(dst + wd + TANGENT, &tangent, 4);

            if (!shouldCalculateVelocity) {
                const __m128 zero = _mm_setzero_ps();
                _mm_store_ss(reinterpret_cast<float *>(dst + wd + VELOCITY), zero);
                _mm_store_ss(reinterpret_cast<float *>(dst + wd + VELOCITY + 4), zero);
                _mm_store_ss(reinterpret_cast<float *>(dst + wd + VELOCITY + 8), zero);
            }

            ws += SRC_STRIDE;
            wd += DST_STRIDE;
        }

        if (shouldCalculateVelocity) {
            __m128 px = pos0;
            __m128 py = pos1;
            __m128 pz = pos2;
            __m128 unused = pos3;
            _MM_TRANSPOSE4_PS(px, py, pz, unused);
            detail::storeStrided(
                    dst + dstOff,
                    VELOCITY,
                    detail::transformComponent<0>(matrix, px, py, pz)
            );
            detail::storeStrided(
                    dst + dstOff,
                    VELOCITY + 4,
                    detail::transformComponent<1>(matrix, px, py, pz)
            );
            detail::storeStrided(
                    dst + dstOff,
                    VELOCITY + 8,
                    detail::transformComponent<2>(matrix, px, py, pz)
            );
        }

        srcOff += SRC_STRIDE * 4;
        dstOff += DST_STRIDE * 4;
    }
}
