#include "sr/sr_api.h"
#include <algorithm>
#include <condition_variable>
#include <exception>
#include <mutex>
#include <new>
#include <utility>
#include <vector>
#include <cstring>

#ifdef ON_WIN64
#include <windows.h>
#include <string>
#include <iostream>
#elif defined(ON_LINUX64)
#include <dlfcn.h>
#include <string>
#include <iostream>
#include <codecvt>
#include <locale>
#endif

#ifdef ON_WIN64
using SRProviderLibraryHandle = HMODULE;
#elif defined(ON_LINUX64)
using SRProviderLibraryHandle = void *;
#endif

struct SRLoadedProviderLibrary {
    std::string path;
    std::vector<SRProviderLibraryHandle> handles;
    std::vector<uint64_t> pendingCloseProviderIds;
    uint64_t id = 0;
    bool loading = false;
    bool unloading = false;
};

struct SRLoadedProviderEntry {
    SRUpscaleProvider provider{};
    uint64_t libraryId = 0;
    uint64_t id = 0;
};

static std::vector<SRLoadedProviderLibrary> g_loadedLibraries;
static std::vector<SRLoadedProviderEntry> g_srLoadedUpscaleProviders;
static std::mutex g_providerMutex;
static std::condition_variable g_providerCondition;
static uint64_t g_nextLibraryId = 1;
static uint64_t g_nextProviderEntryId = 1;
static bool g_shutdownInProgress = false;

static bool srCloseProviderLibrary(SRProviderLibraryHandle handle) {
    if (!handle) {
        return true;
    }
    #ifdef ON_WIN64
    return FreeLibrary(handle) != 0;
    #elif defined(ON_LINUX64)
    return dlclose(handle) == 0;
    #endif
}

static auto srFindLoadedLibrary(const std::string &path) {
    return std::find_if(g_loadedLibraries.begin(), g_loadedLibraries.end(),
                        [&path](const SRLoadedProviderLibrary &library) {
                            return library.path == path;
                        });
}

static auto srFindLoadedLibrary(uint64_t libraryId) {
    return std::find_if(g_loadedLibraries.begin(), g_loadedLibraries.end(),
                        [libraryId](const SRLoadedProviderLibrary &library) {
                            return library.id == libraryId;
                        });
}

static bool srLibraryHasProviders(uint64_t libraryId) {
    return std::any_of(
        g_srLoadedUpscaleProviders.begin(),
        g_srLoadedUpscaleProviders.end(),
        [libraryId](const SRLoadedProviderEntry &entry) {
            return entry.libraryId == libraryId;
        }
    );
}

static bool srLibraryHasPendingCloseForProvider(
    const SRLoadedProviderLibrary &library,
    uint64_t providerId) {
    return std::find(
        library.pendingCloseProviderIds.begin(),
        library.pendingCloseProviderIds.end(),
        providerId
    ) != library.pendingCloseProviderIds.end();
}

static void srCloseProviderLibraryHandles(
    const std::vector<SRProviderLibraryHandle> &handles,
    std::vector<SRProviderLibraryHandle> &remainingHandles) {
    remainingHandles.clear();
    remainingHandles.reserve(handles.size());
    for (SRProviderLibraryHandle handle: handles) {
        if (!srCloseProviderLibrary(handle)) {
            remainingHandles.push_back(handle);
        }
    }
}

static void srPublishProviderLibraryHandle(
    uint64_t libraryId,
    SRProviderLibraryHandle handle) noexcept {
    if (!handle) {
        std::terminate();
    }
    std::lock_guard<std::mutex> lock(g_providerMutex);
    auto library = srFindLoadedLibrary(libraryId);
    if (library == g_loadedLibraries.end() ||
        !library->loading ||
        library->handles.size() != 1 ||
        library->handles.front()) {
        std::terminate();
    }
    library->handles.front() = handle;
}

static bool srCloseRegisteredProviderLibraryHandle(
    uint64_t libraryId,
    SRProviderLibraryHandle handle) {
    if (!handle) {
        return true;
    }
    if (!srCloseProviderLibrary(handle)) {
        return false;
    }

    std::lock_guard<std::mutex> lock(g_providerMutex);
    auto library = srFindLoadedLibrary(libraryId);
    if (library == g_loadedLibraries.end()) {
        return false;
    }
    auto registeredHandle = std::find(
        library->handles.begin(),
        library->handles.end(),
        handle
    );
    if (registeredHandle == library->handles.end()) {
        return false;
    }
    *registeredHandle = nullptr;
    return true;
}

class SRProviderLibraryLoadGuard {
public:
    explicit SRProviderLibraryLoadGuard(uint64_t libraryId) noexcept
        : libraryId(libraryId) {
    }

    ~SRProviderLibraryLoadGuard() noexcept {
        finish();
    }

    SRProviderLibraryLoadGuard(const SRProviderLibraryLoadGuard &) = delete;
    SRProviderLibraryLoadGuard &operator=(const SRProviderLibraryLoadGuard &) = delete;

    void finish() noexcept {
        if (!active) {
            return;
        }
        try {
            std::lock_guard<std::mutex> lock(g_providerMutex);
            auto library = srFindLoadedLibrary(libraryId);
            if (library != g_loadedLibraries.end()) {
                library->loading = false;
                const bool hasHandle = std::any_of(
                    library->handles.begin(),
                    library->handles.end(),
                    [](SRProviderLibraryHandle handle) {
                        return handle != nullptr;
                    }
                );
                if (!hasHandle && !srLibraryHasProviders(libraryId)) {
                    g_loadedLibraries.erase(library);
                }
            }
        } catch (...) {
            return;
        }
        active = false;
        g_providerCondition.notify_all();
    }

private:
    uint64_t libraryId = 0;
    bool active = true;
};

class SRProviderLibraryHandleGuard {
public:
    SRProviderLibraryHandleGuard(
        uint64_t libraryId,
        SRProviderLibraryHandle handle) noexcept
        : libraryId(libraryId), handle(handle) {
    }

    ~SRProviderLibraryHandleGuard() {
        close();
    }

    SRProviderLibraryHandleGuard(const SRProviderLibraryHandleGuard &) = delete;
    SRProviderLibraryHandleGuard &operator=(const SRProviderLibraryHandleGuard &) = delete;

    bool close() noexcept {
        if (!active) {
            return true;
        }
        bool closed = false;
        try {
            closed = srCloseRegisteredProviderLibraryHandle(libraryId, handle);
        } catch (...) {
            closed = false;
        }
        active = false;
        return closed;
    }

    void release() noexcept {
        active = false;
    }

private:
    uint64_t libraryId = 0;
    SRProviderLibraryHandle handle = nullptr;
    bool active = true;
};

class SRProviderShutdownGuard {
public:
    SRProviderShutdownGuard() {
        std::unique_lock<std::mutex> lock(g_providerMutex);
        while (g_shutdownInProgress) {
            g_providerCondition.wait(lock);
        }
        g_shutdownInProgress = true;
        while (std::any_of(
            g_loadedLibraries.begin(),
            g_loadedLibraries.end(),
            [](const SRLoadedProviderLibrary &library) {
                return library.loading;
            })) {
            g_providerCondition.wait(lock);
        }
    }

    ~SRProviderShutdownGuard() noexcept {
        finish();
    }

    SRProviderShutdownGuard(const SRProviderShutdownGuard &) = delete;
    SRProviderShutdownGuard &operator=(const SRProviderShutdownGuard &) = delete;

    void finish() noexcept {
        if (!active) {
            return;
        }
        try {
            std::lock_guard<std::mutex> lock(g_providerMutex);
            g_shutdownInProgress = false;
        } catch (...) {
            return;
        }
        active = false;
        g_providerCondition.notify_all();
    }

private:
    bool active = true;
};

class SRProviderLibraryUnloadGuard {
public:
    explicit SRProviderLibraryUnloadGuard(
        const std::vector<uint64_t> &libraryIds) noexcept
        : libraryIds(libraryIds) {
    }

    ~SRProviderLibraryUnloadGuard() noexcept {
        finish();
    }

    SRProviderLibraryUnloadGuard(const SRProviderLibraryUnloadGuard &) = delete;
    SRProviderLibraryUnloadGuard &operator=(const SRProviderLibraryUnloadGuard &) = delete;

    void finish() noexcept {
        if (!active) {
            return;
        }
        try {
            std::lock_guard<std::mutex> lock(g_providerMutex);
            for (uint64_t libraryId: libraryIds) {
                auto library = srFindLoadedLibrary(libraryId);
                if (library != g_loadedLibraries.end()) {
                    library->unloading = false;
                }
            }
        } catch (...) {
            return;
        }
        active = false;
        g_providerCondition.notify_all();
    }

private:
    const std::vector<uint64_t> &libraryIds;
    bool active = true;
};

static bool srCloseOrphanedProviderLibraries() {
    struct CloseTarget {
        SRLoadedProviderLibrary library;
        std::vector<SRProviderLibraryHandle> remainingHandles;
    };

    std::vector<CloseTarget> closeTargets;
    {
        std::unique_lock<std::mutex> lock(g_providerMutex);
        while (true) {
            bool waitForClose = false;
            for (const SRLoadedProviderLibrary &library: g_loadedLibraries) {
                if (!srLibraryHasProviders(library.id) && library.unloading) {
                    waitForClose = true;
                    break;
                }
            }
            if (waitForClose) {
                g_providerCondition.wait(lock);
                continue;
            }
            const size_t closeCount = std::count_if(
                g_loadedLibraries.begin(),
                g_loadedLibraries.end(),
                [](const SRLoadedProviderLibrary &library) {
                    return !srLibraryHasProviders(library.id);
                }
            );
            closeTargets.reserve(closeCount);
            for (const SRLoadedProviderLibrary &library: g_loadedLibraries) {
                if (!srLibraryHasProviders(library.id)) {
                    CloseTarget target{.library = library};
                    target.remainingHandles.reserve(library.handles.size());
                    closeTargets.push_back(std::move(target));
                }
            }
            for (const CloseTarget &target: closeTargets) {
                auto library = srFindLoadedLibrary(target.library.id);
                if (library != g_loadedLibraries.end()) {
                    library->unloading = true;
                }
            }
            break;
        }
    }

    bool allSuccess = true;
    for (CloseTarget &target: closeTargets) {
        srCloseProviderLibraryHandles(
            target.library.handles,
            target.remainingHandles);
        if (!target.remainingHandles.empty()) {
            allSuccess = false;
        }
    }

    {
        std::lock_guard<std::mutex> lock(g_providerMutex);
        for (CloseTarget &target: closeTargets) {
            auto library = srFindLoadedLibrary(target.library.id);
            if (library == g_loadedLibraries.end()) {
                allSuccess = false;
                continue;
            }
            if (target.remainingHandles.empty()) {
                g_loadedLibraries.erase(library);
            } else {
                library->handles = std::move(target.remainingHandles);
                library->unloading = false;
            }
        }
    }
    g_providerCondition.notify_all();
    return allSuccess;
}

static SRReturnCode srCopyExtraParams(SRContextExtraParams *dst, const SRContextExtraParams *src) {
    if (!dst) {
        return SR_RETURN_CODE_NULL_POINTER;
    }
    memset(dst, 0, sizeof(SRContextExtraParams));
    if (!src) {
        return SR_RETURN_CODE_OK;
    }
    if (src->extraParamCount > SR_API_CONTEXT_MAX_PARAMS) {
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    for (uint32_t i = 0; i < src->extraParamCount; ++i) {
        const SRContextExtraParam *srcParam = &src->extraParams[i];
        if (!srcParam->exist) {
            continue;
        }
        if (!srcParam->name) {
            srDestroyExtraParams(dst);
            return SR_RETURN_CODE_INVALID_ARGUMENT;
        }

        SRContextExtraParam *dstParam = &dst->extraParams[dst->extraParamCount];
        dstParam->name = strdup(srcParam->name);
        if (!dstParam->name) {
            srDestroyExtraParams(dst);
            return SR_RETURN_CODE_ERROR;
        }

        dstParam->valueType = srcParam->valueType;
        dstParam->value = srcParam->value;
        if (srcParam->valueType == SR_PARAM_VALUE_TYPE_STRING && srcParam->value.stringValue) {
            dstParam->value.stringValue = strdup(srcParam->value.stringValue);
            if (!dstParam->value.stringValue) {
                free((void *) dstParam->name);
                memset(dstParam, 0, sizeof(SRContextExtraParam));
                srDestroyExtraParams(dst);
                return SR_RETURN_CODE_ERROR;
            }
        }

        dstParam->exist = true;
        dst->extraParamCount++;
    }

    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srGetUpscaleProvider(
    SRUpscaleProvider *outProvider,
    uint64_t providerId) {
    if (!outProvider) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    std::unique_lock<std::mutex> lock(g_providerMutex);
    while (true) {
        if (g_shutdownInProgress) {
            g_providerCondition.wait(lock);
            continue;
        }
        bool waitForUnload = false;
        for (const SRLoadedProviderEntry &entry: g_srLoadedUpscaleProviders) {
            if (entry.provider.providerId != providerId) {
                continue;
            }
            auto library = srFindLoadedLibrary(entry.libraryId);
            if (library == g_loadedLibraries.end()) {
                return SR_RETURN_CODE_UNEXPECTED_ERROR;
            }
            if (library->loading || library->unloading) {
                waitForUnload = true;
                break;
            }
            *outProvider = entry.provider;
            return SR_RETURN_CODE_OK;
        }
        if (!waitForUnload) {
            return SR_RETURN_CODE_CANNOT_FIND_PROVIDER;
        }
        g_providerCondition.wait(lock);
    }
}

SR_API SRReturnCode srShutdown() try {
    SRProviderShutdownGuard shutdownGuard;
    bool allSuccess = true;
    std::vector<uint64_t> providerIds;
    {
        std::lock_guard<std::mutex> lock(g_providerMutex);
        for (const SRLoadedProviderEntry &entry: g_srLoadedUpscaleProviders) {
            if (std::find(providerIds.begin(), providerIds.end(), entry.provider.providerId) == providerIds.end()) {
                providerIds.push_back(entry.provider.providerId);
            }
        }
    }

    for (uint64_t providerId: providerIds) {
        if (srUnloadUpscaleProviders(providerId) != SR_RETURN_CODE_OK) {
            allSuccess = false;
        }
    }
    if (!srCloseOrphanedProviderLibraries()) {
        allSuccess = false;
    }

    return allSuccess ? SR_RETURN_CODE_OK : SR_RETURN_CODE_UNEXPECTED_ERROR;
} catch (const std::bad_alloc &) {
    return SR_RETURN_CODE_ERROR;
} catch (...) {
    return SR_RETURN_CODE_UNEXPECTED_ERROR;
}

SR_API SRReturnCode srCreateUpscaleContext(
    SRUpscaleContext *outContext,
    SRUpscaleProvider *provider,
    const SRCreateUpscaleContextDesc *desc) {
    if (!outContext || !provider || !desc || !provider->callbacks.pCreate) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    SRCreateUpscaleContextDesc ownedDesc = *desc;
    SRReturnCode copyCode = srCopyExtraParams(&ownedDesc.extraParams, &desc->extraParams);
    if (copyCode != SR_RETURN_CODE_OK) {
        return copyCode;
    }

    outContext->callbacks = provider->callbacks;
    SRReturnCode code = provider->callbacks.pCreate(outContext, &ownedDesc);
    if (code != SR_RETURN_CODE_OK) {
        srDestroyExtraParams(&ownedDesc.extraParams);
        return code;
    }

    // Providers retain the descriptor in the context. Transfer the deep copy
    // explicitly so its string storage remains valid for the context lifetime.
    outContext->desc.extraParams = ownedDesc.extraParams;
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srInitUpscaleContext(
    SRUpscaleContext *context) {
    if (!context || !context->callbacks.pInit) {
        return SR_RETURN_CODE_NULL_POINTER;
    }
    SRReturnCode code = context->callbacks.pInit(context);
    return code;
}

SR_API SRReturnCode srDestroyUpscaleContext(SRUpscaleContext *context) {
    if (!context || !context->callbacks.pDestroy) {
        return SR_RETURN_CODE_NULL_POINTER;
    }
    SRReturnCode code = context->callbacks.pDestroy(context);
    if (code == SR_RETURN_CODE_OK) {
        srDestroyExtraParams(&context->desc.extraParams);
    }
    return code;
}

SR_API SRReturnCode srQueryUpscaleContext(
    SRUpscaleContext *context,
    SRUpscaleContextQueryResult *outResult,
    SRUpscaleContextQueryType queryType) {
    if (!context || !outResult || !context->callbacks.pQuery) {
        return SR_RETURN_CODE_NULL_POINTER;
    }
    outResult->type = queryType;
    return context->callbacks.pQuery(context, outResult, queryType);
}

SR_API SRReturnCode srDispatchUpscale(
    SRUpscaleContext *context,
    const SRDispatchUpscaleDesc *desc) {
    if (!context || !desc || !context->callbacks.pDispatchUpscale) {
        return SR_RETURN_CODE_NULL_POINTER;
    }
    SRReturnCode code = context->callbacks.pDispatchUpscale(context, desc);
    return code;
}

SR_API SRReturnCode srLoadUpscaleProvidersFromLibrary(
    const std::string &libPath,
    const std::string &getProvidersFuncName,
    const std::string &getProvidersCountFuncName,
    SRMessageCallback messageCallback) try {
    uint64_t libraryId = 0;
    {
        std::unique_lock<std::mutex> lock(g_providerMutex);
        while (true) {
            if (g_shutdownInProgress) {
                g_providerCondition.wait(lock);
                continue;
            }
            auto library = srFindLoadedLibrary(libPath);
            if (library != g_loadedLibraries.end() &&
                (library->loading || library->unloading)) {
                g_providerCondition.wait(lock);
                continue;
            }
            if (library != g_loadedLibraries.end()) {
                if (!srLibraryHasProviders(library->id)) {
                    lock.unlock();
                    if (messageCallback) {
                        messageCallback(
                            SR_MESSAGE_TYPE_ERROR,
                            L"Library unload previously failed; call srShutdown before reloading."
                        );
                    }
                    return SR_RETURN_CODE_UNEXPECTED_ERROR;
                }
                lock.unlock();
                if (messageCallback) {
                    messageCallback(SR_MESSAGE_TYPE_INFO, L"Library already loaded, skipping.");
                }
                return SR_RETURN_CODE_OK;
            }

            SRLoadedProviderLibrary tombstone;
            tombstone.path = libPath;
            tombstone.handles.resize(1, nullptr);
            tombstone.id = g_nextLibraryId;
            tombstone.loading = true;
            g_loadedLibraries.push_back(std::move(tombstone));
            libraryId = g_nextLibraryId++;
            break;
        }
    }
    SRProviderLibraryLoadGuard loadGuard(libraryId);
    SRProviderLibraryHandle libraryHandle = nullptr;

    #ifdef ON_WIN64
    // 首先将UTF-8字符串（jstring->GetStringUTFChars+reinterpet_cast->libPath(std::string)）转换为Windows Wide Char.
    // 计算目标缓冲区大小
    size_t wideLen = MultiByteToWideChar(CP_UTF8, 0, libPath.c_str(), -1, NULL, 0);
    // 为0则返回error
    if (wideLen == 0) {
        loadGuard.finish();
        if (messageCallback) {
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"Failed to load DLL,libPath is empty.");
        }
        return SR_RETURN_CODE_UNEXPECTED_ERROR;
    }
    // 否则分配内存并执行实际转换(在Windows上使用Windows API)
    std::wstring widePath(static_cast<size_t>(wideLen), L'\0');
    MultiByteToWideChar(CP_UTF8, 0, libPath.c_str(), -1, widePath.data(), static_cast<int>(wideLen));
    libraryHandle = LoadLibraryW(widePath.c_str());
    if (!libraryHandle) {
        std::wstring error;
        if (messageCallback) {
            error = L"Failed to load DLL: ";
            error += std::to_wstring(GetLastError());
            error += L" Path: ";
            error += widePath.c_str();
        }
        loadGuard.finish();
        if (messageCallback) {
            messageCallback(SR_MESSAGE_TYPE_ERROR, error.c_str());
        }
        return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
    }

    #elif defined(ON_LINUX64)
    // 路径不用转换，本来就是UTF-8
    // 这个converter用来转换messageCallback
    // FSR为什么要用wchar_t呢
    std::wstring_convert<std::codecvt_utf8<wchar_t> > converter;
    libraryHandle = dlopen(libPath.c_str(), RTLD_NOW);
    if (!libraryHandle) {
        std::wstring error;
        if (messageCallback) {
            // 这里必须使用wchar_t，以与FSR的CallBack兼容
            error = L"Failed to load .so: " + converter.from_bytes(dlerror());
        }
        loadGuard.finish();
        if (messageCallback) {
            messageCallback(SR_MESSAGE_TYPE_ERROR, error.c_str());
        }
        return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
    }

    #endif

    srPublishProviderLibraryHandle(libraryId, libraryHandle);
    SRProviderLibraryHandleGuard handleGuard(libraryId, libraryHandle);

    #ifdef ON_WIN64
    auto getProvidersCount = (SRUpscaleProviderSupplierCountFunc)
            GetProcAddress(libraryHandle, getProvidersCountFuncName.c_str());
    auto getProviders = (SRUpscaleProviderSupplierFunc)
            GetProcAddress(libraryHandle, getProvidersFuncName.c_str());
    #elif defined(ON_LINUX64)
    auto getProvidersCount = (SRUpscaleProviderSupplierCountFunc)
            dlsym(libraryHandle, getProvidersCountFuncName.c_str());
    auto getProviders = (SRUpscaleProviderSupplierFunc)
            dlsym(libraryHandle, getProvidersFuncName.c_str());
    #endif

    if (!getProviders || !getProvidersCount) {
        handleGuard.close();
        loadGuard.finish();
        if (messageCallback) {
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"Failed to resolve provider functions.");
        }
        return SR_RETURN_CODE_INVALID_PROVIDER_LIBRARY;
    }

    uint32_t count = 0;
    if (getProvidersCount(&count) != SR_RETURN_CODE_OK || count == 0) {
        handleGuard.close();
        loadGuard.finish();
        if (messageCallback) {
            messageCallback(SR_MESSAGE_TYPE_WARNING, L"No upscale providers found.");
        }
        return SR_RETURN_CODE_INVALID_PROVIDER_LIBRARY;
    }

    std::vector<SRUpscaleProvider> providers(count);
    if (getProviders(providers.data()) != SR_RETURN_CODE_OK) {
        handleGuard.close();
        loadGuard.finish();
        if (messageCallback) {
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"Failed to get providers.");
        }
        return SR_RETURN_CODE_UNEXPECTED_ERROR;
    }

    {
        std::lock_guard<std::mutex> lock(g_providerMutex);
        auto library = srFindLoadedLibrary(libraryId);
        if (library == g_loadedLibraries.end() ||
            !library->loading ||
            library->handles.size() != 1 ||
            library->handles.front() != libraryHandle) {
            return SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        if (providers.size() >
            g_srLoadedUpscaleProviders.max_size() -
            g_srLoadedUpscaleProviders.size()) {
            return SR_RETURN_CODE_ERROR;
        }
        g_srLoadedUpscaleProviders.reserve(
            g_srLoadedUpscaleProviders.size() + providers.size());
        for (const SRUpscaleProvider &provider: providers) {
            g_srLoadedUpscaleProviders.push_back({
                .provider = provider,
                .libraryId = libraryId,
                .id = g_nextProviderEntryId++,
            });
        }
    }
    handleGuard.release();
    loadGuard.finish();

    if (messageCallback) {
        messageCallback(SR_MESSAGE_TYPE_INFO, L"Successfully loaded upscale providers.");
    }

    return SR_RETURN_CODE_OK;
} catch (const std::bad_alloc &) {
    return SR_RETURN_CODE_ERROR;
} catch (...) {
    return SR_RETURN_CODE_UNEXPECTED_ERROR;
}

SR_API SRReturnCode srUnloadUpscaleProviders(uint64_t providerId) try {
    struct ShutdownTarget {
        SRUpscaleProvider provider{};
        uint64_t libraryId = 0;
        uint64_t entryId = 0;
        bool succeeded = false;
    };

    struct CloseTarget {
        SRLoadedProviderLibrary library;
        std::vector<SRProviderLibraryHandle> remainingHandles;
        bool shouldClose = false;
    };

    std::vector<ShutdownTarget> shutdownTargets;
    std::vector<uint64_t> affectedLibraryIds;
    std::vector<CloseTarget> librariesToClose;
    SRProviderLibraryUnloadGuard unloadGuard(affectedLibraryIds);
    {
        std::unique_lock<std::mutex> lock(g_providerMutex);
        while (true) {
            shutdownTargets.clear();
            affectedLibraryIds.clear();
            librariesToClose.clear();
            bool waitForUnload = false;
            for (const SRLoadedProviderEntry &entry: g_srLoadedUpscaleProviders) {
                if (entry.provider.providerId != providerId) {
                    continue;
                }
                auto library = srFindLoadedLibrary(entry.libraryId);
                if (library == g_loadedLibraries.end()) {
                    return SR_RETURN_CODE_UNEXPECTED_ERROR;
                }
                if (library->loading || library->unloading) {
                    waitForUnload = true;
                    break;
                }
                shutdownTargets.push_back({
                    .provider = entry.provider,
                    .libraryId = entry.libraryId,
                    .entryId = entry.id,
                });
                if (std::find(affectedLibraryIds.begin(), affectedLibraryIds.end(), entry.libraryId) ==
                    affectedLibraryIds.end()) {
                    affectedLibraryIds.push_back(entry.libraryId);
                }
            }
            if (!waitForUnload) {
                for (const SRLoadedProviderLibrary &library: g_loadedLibraries) {
                    if (!srLibraryHasPendingCloseForProvider(library, providerId)) {
                        continue;
                    }
                    if (library.loading || library.unloading) {
                        waitForUnload = true;
                        break;
                    }
                    if (std::find(
                            affectedLibraryIds.begin(),
                            affectedLibraryIds.end(),
                            library.id) == affectedLibraryIds.end()) {
                        affectedLibraryIds.push_back(library.id);
                    }
                }
            }
            if (waitForUnload) {
                g_providerCondition.wait(lock);
                continue;
            }
            if (shutdownTargets.empty() && affectedLibraryIds.empty()) {
                return SR_RETURN_CODE_OK;
            }
            librariesToClose.reserve(affectedLibraryIds.size());
            for (uint64_t libraryId: affectedLibraryIds) {
                auto library = srFindLoadedLibrary(libraryId);
                if (library == g_loadedLibraries.end()) {
                    return SR_RETURN_CODE_UNEXPECTED_ERROR;
                }
                if (!srLibraryHasPendingCloseForProvider(*library, providerId)) {
                    if (library->pendingCloseProviderIds.size() ==
                        library->pendingCloseProviderIds.max_size()) {
                        return SR_RETURN_CODE_ERROR;
                    }
                    library->pendingCloseProviderIds.reserve(
                        library->pendingCloseProviderIds.size() + 1);
                }
                CloseTarget target{.library = *library};
                target.remainingHandles.reserve(library->handles.size());
                librariesToClose.push_back(std::move(target));
            }
            for (uint64_t libraryId: affectedLibraryIds) {
                auto library = srFindLoadedLibrary(libraryId);
                library->unloading = true;
            }
            break;
        }
    }

    bool allSuccess = true;
    for (ShutdownTarget &target: shutdownTargets) {
        try {
            target.succeeded = !target.provider.callbacks.pShutdown ||
                               target.provider.callbacks.pShutdown() == SR_RETURN_CODE_OK;
        } catch (...) {
            target.succeeded = false;
        }
        if (!target.succeeded) {
            allSuccess = false;
        }
    }

    bool allLibrariesClosed = true;
    {
        std::lock_guard<std::mutex> lock(g_providerMutex);
        g_srLoadedUpscaleProviders.erase(
            std::remove_if(g_srLoadedUpscaleProviders.begin(), g_srLoadedUpscaleProviders.end(),
                           [&shutdownTargets](const SRLoadedProviderEntry &entry) {
                               return std::any_of(
                                   shutdownTargets.begin(),
                                   shutdownTargets.end(),
                                   [&entry](const ShutdownTarget &target) {
                                       return target.succeeded && target.entryId == entry.id;
                                   }
                               );
                           }),
            g_srLoadedUpscaleProviders.end()
        );
        for (CloseTarget &target: librariesToClose) {
            const uint64_t libraryId = target.library.id;
            const bool hasProviders = srLibraryHasProviders(libraryId);
            auto library = srFindLoadedLibrary(libraryId);
            if (library == g_loadedLibraries.end()) {
                allLibrariesClosed = false;
                continue;
            }
            if (hasProviders) {
                library->unloading = false;
                continue;
            }
            if (!srLibraryHasPendingCloseForProvider(*library, providerId)) {
                library->pendingCloseProviderIds.push_back(providerId);
            }
            target.shouldClose = true;
        }
    }

    for (CloseTarget &target: librariesToClose) {
        if (!target.shouldClose) {
            continue;
        }
        srCloseProviderLibraryHandles(
            target.library.handles,
            target.remainingHandles);
        if (!target.remainingHandles.empty()) {
            allLibrariesClosed = false;
        }
    }
    {
        std::lock_guard<std::mutex> lock(g_providerMutex);
        for (CloseTarget &target: librariesToClose) {
            if (!target.shouldClose) {
                continue;
            }
            auto library = srFindLoadedLibrary(target.library.id);
            if (library == g_loadedLibraries.end()) {
                allLibrariesClosed = false;
                continue;
            }
            if (target.remainingHandles.empty()) {
                g_loadedLibraries.erase(library);
            } else {
                library->handles = std::move(target.remainingHandles);
                library->unloading = false;
            }
        }
    }
    g_providerCondition.notify_all();

    return allSuccess && allLibrariesClosed
        ? SR_RETURN_CODE_OK
        : SR_RETURN_CODE_UNEXPECTED_ERROR;
} catch (const std::bad_alloc &) {
    return SR_RETURN_CODE_ERROR;
} catch (...) {
    return SR_RETURN_CODE_UNEXPECTED_ERROR;
}

SR_API GLenum srTextureFormatToGlFormat(SRTextureFormat fmt) {
    switch (fmt) {
        case SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS:
            return GL_RGBA32F;
        case SR_TEXTURE_FORMAT_R32G32B32A32_FLOAT:
            return GL_RGBA32F;
        case SR_TEXTURE_FORMAT_R16G16B16A16_FLOAT:
            return GL_RGBA16F;
        case SR_TEXTURE_FORMAT_R16G16B16A16_SNORM:
            return GL_RGBA16_SNORM;
        case SR_TEXTURE_FORMAT_R32G32_FLOAT:
            return GL_RG32F;
        case SR_TEXTURE_FORMAT_R32_UINT:
            return GL_R32UI;
        case SR_TEXTURE_FORMAT_R8G8B8A8_TYPELESS:
            return GL_RGBA8;
        case SR_TEXTURE_FORMAT_R8G8B8A8_UNORM:
            return GL_RGBA8;
        case SR_TEXTURE_FORMAT_R11G11B10_FLOAT:
            return GL_R11F_G11F_B10F;
        case SR_TEXTURE_FORMAT_R16G16_FLOAT:
            return GL_RG16F;
        case SR_TEXTURE_FORMAT_R16G16_UINT:
            return GL_RG16UI;
        case SR_TEXTURE_FORMAT_R16_FLOAT:
            return GL_R16F;
        case SR_TEXTURE_FORMAT_R16_UINT:
            return GL_R16UI;
        case SR_TEXTURE_FORMAT_R16_UNORM:
            return GL_R16;
        case SR_TEXTURE_FORMAT_R16_SNORM:
            return GL_R16_SNORM;
        case SR_TEXTURE_FORMAT_R8_UNORM:
            return GL_R8;
        case SR_TEXTURE_FORMAT_R8G8_UNORM:
            return GL_RG8;
        case SR_TEXTURE_FORMAT_R32_FLOAT:
            return GL_R32F;
        case SR_TEXTURE_FORMAT_R8_UINT:
            return GL_R8UI;
        case SR_TEXTURE_FORMAT_D32_SFLOAT:
            return GL_DEPTH_COMPONENT32F;
        default:
            return 0;
    }
}

SR_API VkFormat srTextureFormatToVkFormat(SRTextureFormat fmt) {
    switch (fmt) {
        case (SR_TEXTURE_FORMAT_UNKNOWN):
            return VK_FORMAT_UNDEFINED;
        case (SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS):
            return VK_FORMAT_R32G32B32A32_SFLOAT;
        case (SR_TEXTURE_FORMAT_R32G32B32A32_UINT):
            return VK_FORMAT_R32G32B32A32_UINT;
        case (SR_TEXTURE_FORMAT_R32G32B32A32_FLOAT):
            return VK_FORMAT_R32G32B32A32_SFLOAT;
        case (SR_TEXTURE_FORMAT_R16G16B16A16_FLOAT):
            return VK_FORMAT_R16G16B16A16_SFLOAT;
        case (SR_TEXTURE_FORMAT_R32G32B32_FLOAT):
            return VK_FORMAT_R32G32B32_SFLOAT;
        case (SR_TEXTURE_FORMAT_R32G32_FLOAT):
            return VK_FORMAT_R32G32_SFLOAT;
        case (SR_TEXTURE_FORMAT_R8_UINT):
            return VK_FORMAT_R8_UINT;
        case (SR_TEXTURE_FORMAT_R32_UINT):
            return VK_FORMAT_R32_UINT;
        case (SR_TEXTURE_FORMAT_R8G8B8A8_TYPELESS):
            return VK_FORMAT_R8G8B8A8_UNORM;
        case (SR_TEXTURE_FORMAT_R8G8B8A8_UNORM):
            return VK_FORMAT_R8G8B8A8_UNORM;
        case (SR_TEXTURE_FORMAT_R8G8B8A8_SNORM):
            return VK_FORMAT_R8G8B8A8_SNORM;
        case (SR_TEXTURE_FORMAT_R8G8B8A8_SRGB):
            return VK_FORMAT_R8G8B8A8_SRGB;
        case (SR_TEXTURE_FORMAT_B8G8R8A8_TYPELESS):
            return VK_FORMAT_B8G8R8A8_UNORM;
        case (SR_TEXTURE_FORMAT_B8G8R8A8_UNORM):
            return VK_FORMAT_B8G8R8A8_UNORM;
        case (SR_TEXTURE_FORMAT_B8G8R8A8_SRGB):
            return VK_FORMAT_B8G8R8A8_SRGB;
        case (SR_TEXTURE_FORMAT_R11G11B10_FLOAT):
            return VK_FORMAT_B10G11R11_UFLOAT_PACK32;
        case (SR_TEXTURE_FORMAT_R10G10B10A2_UNORM):
            return VK_FORMAT_A2B10G10R10_UNORM_PACK32;
        case (SR_TEXTURE_FORMAT_R16G16_FLOAT):
            return VK_FORMAT_R16G16_SFLOAT;
        case (SR_TEXTURE_FORMAT_R16G16_UINT):
            return VK_FORMAT_R16G16_UINT;
        case (SR_TEXTURE_FORMAT_R16G16_SINT):
            return VK_FORMAT_R16G16_SINT;
        case (SR_TEXTURE_FORMAT_R16_FLOAT):
            return VK_FORMAT_R16_SFLOAT;
        case (SR_TEXTURE_FORMAT_R16_UINT):
            return VK_FORMAT_R16_UINT;
        case (SR_TEXTURE_FORMAT_R16_UNORM):
            return VK_FORMAT_R16_UNORM;
        case (SR_TEXTURE_FORMAT_R16_SNORM):
            return VK_FORMAT_R16_SNORM;
        case (SR_TEXTURE_FORMAT_R8_UNORM):
            return VK_FORMAT_R8_UNORM;
        case (SR_TEXTURE_FORMAT_R8G8_UNORM):
            return VK_FORMAT_R8G8_UNORM;
        case (SR_TEXTURE_FORMAT_R8G8_UINT):
            return VK_FORMAT_R8G8_UINT;
        case (SR_TEXTURE_FORMAT_R32_FLOAT):
            return VK_FORMAT_R32_SFLOAT;
        case (SR_TEXTURE_FORMAT_R9G9B9E5_SHAREDEXP):
            return VK_FORMAT_E5B9G9R9_UFLOAT_PACK32;
        case (SR_TEXTURE_FORMAT_D32_SFLOAT):
            return VK_FORMAT_D32_SFLOAT;
        case (SR_TEXTURE_FORMAT_R16G16B16A16_SNORM):
            return VK_FORMAT_R16G16B16A16_SNORM;
        default:
            return VK_FORMAT_UNDEFINED;
    }
}

SR_API const SRContextExtraParam *srFindParam(
    const SRContextExtraParams *params,
    const char *name) {
    if (!params || !name) {
        return nullptr;
    }

    for (uint32_t i = 0; i < params->extraParamCount; ++i) {
        if (params->extraParams[i].name && strcmp(params->extraParams[i].name, name) == 0) {
            return &params->extraParams[i];
        }
    }

    return nullptr;
}

SR_API SRReturnCode srParamsSetBool(
    SRContextExtraParams *params,
    const char *name,
    bool value) {
    if (!params || !name) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    if (params->extraParamCount >= SR_API_CONTEXT_MAX_PARAMS) {
        return SR_RETURN_CODE_ERROR;
    }
    char *nameCopy = strdup(name);
    if (!nameCopy)
        return SR_RETURN_CODE_ERROR;
    SRContextExtraParam *param = &params->extraParams[params->extraParamCount];
    param->name = nameCopy;
    param->valueType = SR_PARAM_VALUE_TYPE_BOOL;
    param->value.boolValue = value;
    param->exist = true;
    params->extraParamCount++;

    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsSetInt32(
    SRContextExtraParams *params,
    const char *name,
    int32_t value) {
    if (!params || !name) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    if (params->extraParamCount >= SR_API_CONTEXT_MAX_PARAMS) {
        return SR_RETURN_CODE_ERROR;
    }
    char *nameCopy = strdup(name);
    if (!nameCopy)
        return SR_RETURN_CODE_ERROR;
    SRContextExtraParam *param = &params->extraParams[params->extraParamCount];
    param->name = nameCopy;
    param->valueType = SR_PARAM_VALUE_TYPE_INT32;
    param->value.int32Value = value;
    param->exist = true;
    params->extraParamCount++;

    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsSetUint32(
    SRContextExtraParams *params,
    const char *name,
    uint32_t value) {
    if (!params || !name) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    if (params->extraParamCount >= SR_API_CONTEXT_MAX_PARAMS) {
        return SR_RETURN_CODE_ERROR;
    }
    char *nameCopy = strdup(name);
    if (!nameCopy)
        return SR_RETURN_CODE_ERROR;
    SRContextExtraParam *param = &params->extraParams[params->extraParamCount];
    param->name = nameCopy;
    param->valueType = SR_PARAM_VALUE_TYPE_UINT32;
    param->value.uint32Value = value;
    param->exist = true;
    params->extraParamCount++;

    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsSetFloat(
    SRContextExtraParams *params,
    const char *name,
    float value) {
    if (!params || !name) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    if (params->extraParamCount >= SR_API_CONTEXT_MAX_PARAMS) {
        return SR_RETURN_CODE_ERROR;
    }
    char *nameCopy = strdup(name);
    if (!nameCopy)
        return SR_RETURN_CODE_ERROR;
    SRContextExtraParam *param = &params->extraParams[params->extraParamCount];
    param->name = nameCopy;
    param->valueType = SR_PARAM_VALUE_TYPE_FLOAT;
    param->value.floatValue = value;
    param->exist = true;
    params->extraParamCount++;

    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsSetDouble(
    SRContextExtraParams *params,
    const char *name,
    double value) {
    if (!params || !name) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    if (params->extraParamCount >= SR_API_CONTEXT_MAX_PARAMS) {
        return SR_RETURN_CODE_ERROR;
    }
    char *nameCopy = strdup(name);
    if (!nameCopy)
        return SR_RETURN_CODE_ERROR;
    SRContextExtraParam *param = &params->extraParams[params->extraParamCount];
    param->name = nameCopy;
    param->valueType = SR_PARAM_VALUE_TYPE_DOUBLE;
    param->value.doubleValue = value;
    param->exist = true;
    params->extraParamCount++;

    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsSetString(
    SRContextExtraParams *params,
    const char *name,
    const char *value) {
    if (!params || !name || !value) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    if (params->extraParamCount >= SR_API_CONTEXT_MAX_PARAMS) {
        return SR_RETURN_CODE_ERROR;
    }
    char *nameCopy = strdup(name);
    if (!nameCopy)
        return SR_RETURN_CODE_ERROR;

    char *valueCopy = strdup(value);
    if (!valueCopy) {
        free(nameCopy);
        return SR_RETURN_CODE_ERROR;
    }
    SRContextExtraParam *param = &params->extraParams[params->extraParamCount];
    param->name = nameCopy;
    param->valueType = SR_PARAM_VALUE_TYPE_STRING;
    param->value.stringValue = valueCopy;
    param->exist = true;
    params->extraParamCount++;

    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsSetPointer(
    SRContextExtraParams *params,
    const char *name,
    void *value) {
    if (!params || !name) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    if (params->extraParamCount >= SR_API_CONTEXT_MAX_PARAMS) {
        return SR_RETURN_CODE_ERROR;
    }
    char *nameCopy = strdup(name);
    if (!nameCopy)
        return SR_RETURN_CODE_ERROR;

    SRContextExtraParam *param = &params->extraParams[params->extraParamCount];
    param->name = nameCopy;
    param->valueType = SR_PARAM_VALUE_TYPE_POINTER;
    param->value.ptrValue = value;
    param->exist = true;
    params->extraParamCount++;

    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsGetBool(
    const SRContextExtraParams *params,
    const char *name,
    bool *outValue,
    bool defaultValue) {
    if (!params || !name || !outValue) {
        if (outValue) {
            *outValue = defaultValue;
        }
        return SR_RETURN_CODE_NULL_POINTER;
    }

    const SRContextExtraParam *param = srFindParam(params, name);
    if (!param) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_OK;
    }

    if (param->valueType != SR_PARAM_VALUE_TYPE_BOOL) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    *outValue = param->value.boolValue;
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsGetInt32(
    const SRContextExtraParams *params,
    const char *name,
    int32_t *outValue,
    int32_t defaultValue) {
    if (!params || !name || !outValue) {
        if (outValue) {
            *outValue = defaultValue;
        }
        return SR_RETURN_CODE_NULL_POINTER;
    }

    const SRContextExtraParam *param = srFindParam(params, name);
    if (!param) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_OK;
    }

    if (param->valueType != SR_PARAM_VALUE_TYPE_INT32) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    *outValue = param->value.int32Value;
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsGetUint32(
    const SRContextExtraParams *params,
    const char *name,
    uint32_t *outValue,
    uint32_t defaultValue) {
    if (!params || !name || !outValue) {
        if (outValue) {
            *outValue = defaultValue;
        }
        return SR_RETURN_CODE_NULL_POINTER;
    }

    const SRContextExtraParam *param = srFindParam(params, name);
    if (!param) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_OK;
    }

    if (param->valueType != SR_PARAM_VALUE_TYPE_UINT32) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    *outValue = param->value.uint32Value;
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsGetFloat(
    const SRContextExtraParams *params,
    const char *name,
    float *outValue,
    float defaultValue) {
    if (!params || !name || !outValue) {
        if (outValue) {
            *outValue = defaultValue;
        }
        return SR_RETURN_CODE_NULL_POINTER;
    }

    const SRContextExtraParam *param = srFindParam(params, name);
    if (!param) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_OK;
    }

    if (param->valueType != SR_PARAM_VALUE_TYPE_FLOAT) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    *outValue = param->value.floatValue;
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsGetDouble(
    const SRContextExtraParams *params,
    const char *name,
    double *outValue,
    double defaultValue) {
    if (!params || !name || !outValue) {
        if (outValue) {
            *outValue = defaultValue;
        }
        return SR_RETURN_CODE_NULL_POINTER;
    }

    const SRContextExtraParam *param = srFindParam(params, name);
    if (!param) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_OK;
    }

    if (param->valueType != SR_PARAM_VALUE_TYPE_DOUBLE) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    *outValue = param->value.doubleValue;
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsGetString(
    const SRContextExtraParams *params,
    const char *name,
    const char **outValue,
    const char *defaultValue) {
    if (!params || !name || !outValue) {
        if (outValue) {
            *outValue = defaultValue;
        }
        return SR_RETURN_CODE_NULL_POINTER;
    }

    const SRContextExtraParam *param = srFindParam(params, name);
    if (!param) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_OK;
    }

    if (param->valueType != SR_PARAM_VALUE_TYPE_STRING) {
        *outValue = defaultValue;
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    *outValue = param->value.stringValue;
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srParamsGetPointer(
    const SRContextExtraParams *params,
    const char *name,
    void **outValue) {
    if (!params || !name || !outValue) {
        if (outValue) {
            *outValue = nullptr;
        }
        return SR_RETURN_CODE_NULL_POINTER;
    }

    const SRContextExtraParam *param = srFindParam(params, name);
    if (!param) {
        *outValue = nullptr;
        return SR_RETURN_CODE_OK;
    }

    if (param->valueType != SR_PARAM_VALUE_TYPE_POINTER) {
        *outValue = nullptr;
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    *outValue = param->value.ptrValue;
    return SR_RETURN_CODE_OK;
}

SR_API void srDestroyExtraParams(SRContextExtraParams *params) {
    for (uint32_t i = 0; i < params->extraParamCount; ++i) {
        if (params->extraParams[i].exist) {
            free((void *) params->extraParams[i].name);
            if (params->extraParams[i].valueType == SR_PARAM_VALUE_TYPE_STRING) {
                if ((void *) params->extraParams[i].value.stringValue) {
                    free((void *) params->extraParams[i].value.stringValue);
                }
            }
        }
    }
    params->extraParamCount = 0;
}
