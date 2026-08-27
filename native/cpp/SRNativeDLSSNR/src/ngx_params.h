#pragma once

#include <map>
#include <string>
#include <cstdint>

#include "nvsdk_ngx_defs.h"
#include "nvsdk_ngx_params.h"

// 直载 nvngx_dlssnr.dll 时没有 ngx core 提供 NVSDK_NGX_Parameter 实现,自行实现一个。
// 值存储为 (类型标签 + 8 字节负载),Get 时校验类型,缺失/类型不符返回 InvalidParameter。
class SRNgxParams : public NVSDK_NGX_Parameter
{
public:
    NVSDK_NGX_Result GetRaw(const char* name, int expectedType, uint64_t& outBits) const
    {
        auto it = m_values.find(name ? name : "");
        if (it == m_values.end() || it->second.type != expectedType)
            return NVSDK_NGX_Result_FAIL_InvalidParameter;
        outBits = it->second.bits;
        return NVSDK_NGX_Result_Success;
    }

    void Set(const char* name, unsigned long long value) override { store(name, 1, value); }
    void Set(const char* name, float value) override { union { float f; uint32_t u; } cvt{ value }; store(name, 2, cvt.u); }
    void Set(const char* name, double value) override { union { double d; uint64_t u; } cvt{ value }; store(name, 3, cvt.u); }
    void Set(const char* name, unsigned int value) override { store(name, 4, value); }
    void Set(const char* name, int value) override { store(name, 5, static_cast<uint64_t>(static_cast<int64_t>(value))); }
    void Set(const char* name, ID3D11Resource* value) override { store(name, 6, reinterpret_cast<uint64_t>(value)); }
    void Set(const char* name, ID3D12Resource* value) override { store(name, 7, reinterpret_cast<uint64_t>(value)); }
    void Set(const char* name, void* value) override { store(name, 8, reinterpret_cast<uint64_t>(value)); }

    NVSDK_NGX_Result Get(const char* name, unsigned long long* out) const override { return load(name, 1, out); }
    NVSDK_NGX_Result Get(const char* name, float* out) const override
    {
        uint64_t bits; auto r = GetRaw(name, 2, bits);
        if (NVSDK_NGX_SUCCEED(r)) { union { uint32_t u; float f; } cvt{ static_cast<uint32_t>(bits) }; *out = cvt.f; }
        return r;
    }
    NVSDK_NGX_Result Get(const char* name, double* out) const override
    {
        uint64_t bits; auto r = GetRaw(name, 3, bits);
        if (NVSDK_NGX_SUCCEED(r)) { union { uint64_t u; double d; } cvt{ bits }; *out = cvt.d; }
        return r;
    }
    NVSDK_NGX_Result Get(const char* name, unsigned int* out) const override
    {
        uint64_t bits; auto r = GetRaw(name, 4, bits);
        if (NVSDK_NGX_SUCCEED(r)) *out = static_cast<unsigned int>(bits);
        return r;
    }
    NVSDK_NGX_Result Get(const char* name, int* out) const override
    {
        uint64_t bits; auto r = GetRaw(name, 5, bits);
        if (NVSDK_NGX_SUCCEED(r)) *out = static_cast<int>(bits);
        return r;
    }
    NVSDK_NGX_Result Get(const char* name, ID3D11Resource** out) const override { return load(name, 6, out); }
    NVSDK_NGX_Result Get(const char* name, ID3D12Resource** out) const override { return load(name, 7, out); }
    NVSDK_NGX_Result Get(const char* name, void** out) const override { return load(name, 8, out); }

    void Reset() override { m_values.clear(); }

private:
    struct Entry { int type; uint64_t bits; };

    template<typename T>
    NVSDK_NGX_Result load(const char* name, int type, T** out) const
    {
        uint64_t bits; auto r = GetRaw(name, type, bits);
        if (NVSDK_NGX_SUCCEED(r)) *out = reinterpret_cast<T*>(static_cast<uintptr_t>(bits));
        return r;
    }
    NVSDK_NGX_Result load(const char* name, int type, unsigned long long* out) const
    {
        uint64_t bits; auto r = GetRaw(name, type, bits);
        if (NVSDK_NGX_SUCCEED(r)) *out = bits;
        return r;
    }
    void store(const char* name, int type, uint64_t bits)
    {
        m_values[name ? name : ""] = Entry{ type, bits };
    }

    std::map<std::string, Entry> m_values;
};
