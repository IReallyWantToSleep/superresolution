#include <vulkan/vulkan.h>

#include "nvsdk_ngx_defs.h"
#include "nvsdk_ngx_defs_vk.h"

#include <mutex>
#include <string>
#include <windows.h>

namespace {
    HMODULE g_proxyModule = nullptr;
    HMODULE g_realModule = nullptr;
    std::once_flag g_loadOnce;
    LONG g_noTailCall = 0;

    std::wstring moduleDirectory() {
        wchar_t path[MAX_PATH] = {};
        DWORD length = GetModuleFileNameW(g_proxyModule, path, MAX_PATH);
        if (length == 0 || length == MAX_PATH) {
            return L".";
        }
        std::wstring fullPath(path, length);
        size_t sep = fullPath.find_last_of(L"\\/");
        return sep == std::wstring::npos ? L"." : fullPath.substr(0, sep);
    }

    HMODULE realModule() {
        std::call_once(g_loadOnce, [] {
            std::wstring path = moduleDirectory();
            path += L"\\nvngx_dlssnr.dll";
            g_realModule = LoadLibraryW(path.c_str());
        });
        return g_realModule;
    }

    template<typename T>
    T resolve(const char *name) {
        HMODULE module = realModule();
        return module ? reinterpret_cast<T>(GetProcAddress(module, name)) : nullptr;
    }

    void preventTailCall() {
        InterlockedExchangeAdd(&g_noTailCall, 0);
    }

    constexpr NVSDK_NGX_Result kMissingRealExport = NVSDK_NGX_Result_FAIL_NotImplemented;
}

extern "C" {
    BOOL WINAPI DllMain(HINSTANCE module, DWORD reason, LPVOID) {
        if (reason == DLL_PROCESS_ATTACH) {
            g_proxyModule = module;
            DisableThreadLibraryCalls(module);
        }
        return TRUE;
    }

    __declspec(dllexport) NVSDK_NGX_Result NVSDK_CONV NVSDK_NGX_GetAPIVersion(NVSDK_NGX_Version *version) {
        using Fn = NVSDK_NGX_Result(NVSDK_CONV *)(NVSDK_NGX_Version *);
        Fn fn = resolve<Fn>("NVSDK_NGX_GetAPIVersion");
        NVSDK_NGX_Result result = fn ? fn(version) : kMissingRealExport;
        preventTailCall();
        return result;
    }

    __declspec(dllexport) unsigned int NVSDK_CONV NVSDK_NGX_GetSnippetVersion() {
        using Fn = unsigned int(NVSDK_CONV *)();
        Fn fn = resolve<Fn>("NVSDK_NGX_GetSnippetVersion");
        unsigned int result = fn ? fn() : 0;
        preventTailCall();
        return result;
    }

    __declspec(dllexport) unsigned long long NVSDK_CONV NVSDK_NGX_GetApplicationId() {
        using Fn = unsigned long long(NVSDK_CONV *)();
        Fn fn = resolve<Fn>("NVSDK_NGX_GetApplicationId");
        unsigned long long result = fn ? fn() : 0;
        preventTailCall();
        return result;
    }

    __declspec(dllexport) NVSDK_NGX_Result NVSDK_CONV NVSDK_NGX_VULKAN_Init_Ext2(
        unsigned long long applicationId,
        const wchar_t *applicationDataPath,
        VkInstance instance,
        VkPhysicalDevice physicalDevice,
        VkDevice device,
        PFN_vkGetInstanceProcAddr getInstanceProcAddr,
        PFN_vkGetDeviceProcAddr getDeviceProcAddr,
        NVSDK_NGX_Version sdkVersion,
        const NVSDK_NGX_Parameter *parameters
    ) {
        using Fn = NVSDK_NGX_Result(NVSDK_CONV *)(
            unsigned long long, const wchar_t *, VkInstance, VkPhysicalDevice, VkDevice,
            PFN_vkGetInstanceProcAddr, PFN_vkGetDeviceProcAddr, NVSDK_NGX_Version,
            const NVSDK_NGX_Parameter *);
        Fn fn = resolve<Fn>("NVSDK_NGX_VULKAN_Init_Ext2");
        NVSDK_NGX_Result result = fn ? fn(
            applicationId,
            applicationDataPath,
            instance,
            physicalDevice,
            device,
            getInstanceProcAddr,
            getDeviceProcAddr,
            sdkVersion,
            parameters
        ) : kMissingRealExport;
        preventTailCall();
        return result;
    }

    __declspec(dllexport) NVSDK_NGX_Result NVSDK_CONV NVSDK_NGX_VULKAN_Shutdown1(VkDevice device) {
        using Fn = NVSDK_NGX_Result(NVSDK_CONV *)(VkDevice);
        Fn fn = resolve<Fn>("NVSDK_NGX_VULKAN_Shutdown1");
        NVSDK_NGX_Result result = fn ? fn(device) : kMissingRealExport;
        preventTailCall();
        return result;
    }

    __declspec(dllexport) NVSDK_NGX_Result NVSDK_CONV NVSDK_NGX_VULKAN_CreateFeature1(
        VkDevice device,
        VkCommandBuffer commandBuffer,
        NVSDK_NGX_Feature feature,
        NVSDK_NGX_Parameter *parameters,
        NVSDK_NGX_Handle **handle
    ) {
        using Fn = NVSDK_NGX_Result(NVSDK_CONV *)(
            VkDevice, VkCommandBuffer, NVSDK_NGX_Feature, NVSDK_NGX_Parameter *, NVSDK_NGX_Handle **);
        Fn fn = resolve<Fn>("NVSDK_NGX_VULKAN_CreateFeature1");
        NVSDK_NGX_Result result = fn ? fn(device, commandBuffer, feature, parameters, handle) : kMissingRealExport;
        preventTailCall();
        return result;
    }

    __declspec(dllexport) NVSDK_NGX_Result NVSDK_CONV NVSDK_NGX_VULKAN_EvaluateFeature(
        VkCommandBuffer commandBuffer,
        const NVSDK_NGX_Handle *handle,
        const NVSDK_NGX_Parameter *parameters,
        void *callback
    ) {
        using Fn = NVSDK_NGX_Result(NVSDK_CONV *)(
            VkCommandBuffer, const NVSDK_NGX_Handle *, const NVSDK_NGX_Parameter *, void *);
        Fn fn = resolve<Fn>("NVSDK_NGX_VULKAN_EvaluateFeature");
        NVSDK_NGX_Result result = fn ? fn(commandBuffer, handle, parameters, callback) : kMissingRealExport;
        preventTailCall();
        return result;
    }

    __declspec(dllexport) NVSDK_NGX_Result NVSDK_CONV NVSDK_NGX_VULKAN_ReleaseFeature(NVSDK_NGX_Handle *handle) {
        using Fn = NVSDK_NGX_Result(NVSDK_CONV *)(NVSDK_NGX_Handle *);
        Fn fn = resolve<Fn>("NVSDK_NGX_VULKAN_ReleaseFeature");
        NVSDK_NGX_Result result = fn ? fn(handle) : kMissingRealExport;
        preventTailCall();
        return result;
    }

    __declspec(dllexport) NVSDK_NGX_Result NVSDK_CONV NVSDK_NGX_VULKAN_GetFeatureRequirements(
        VkInstance instance,
        VkPhysicalDevice physicalDevice,
        const NVSDK_NGX_FeatureDiscoveryInfo *discoveryInfo,
        NVSDK_NGX_FeatureRequirement *requirements
    ) {
        using Fn = NVSDK_NGX_Result(NVSDK_CONV *)(
            VkInstance, VkPhysicalDevice, const NVSDK_NGX_FeatureDiscoveryInfo *, NVSDK_NGX_FeatureRequirement *);
        Fn fn = resolve<Fn>("NVSDK_NGX_VULKAN_GetFeatureRequirements");
        NVSDK_NGX_Result result = fn ? fn(instance, physicalDevice, discoveryInfo, requirements) : kMissingRealExport;
        preventTailCall();
        return result;
    }
}
