#include "sr/streamline/streamline.h"

#include <algorithm>
#include <cstring>
#include <filesystem>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <utility>
#include <vector>
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <windows.h>
#include <vulkan/vulkan.h>
#include "jni/jni.h"

#include "sl.h"
#include "sl_consts.h"
#include "sl_core_api.h"
#include "sl_dlss.h"
#include "sl_dlss_g.h"
#include "sl_helpers.h"
#include "sl_helpers_vk.h"
#include "sl_pcl.h"
#include "sl_reflex.h"

namespace {
    constexpr const char *kProjectId = "3a799712-b54a-407c-82b0-eb3366f0f1e3";
    constexpr uint32_t kViewportId = 0;

    std::mutex g_streamlineMutex;
    bool g_streamlineInitialized = false;
    bool g_dlssOptionsInitialized = false;
    std::wstring g_pluginPath;
    std::wstring g_logPath;
    SRMessageCallback g_messageCallback = nullptr;
    int g_lastVkResult = VK_SUCCESS;

    struct StreamlineDLSSPrivateData {
        SRMessageCallback messageCallback = nullptr;
        uint64_t estimatedVram = 0;
    };

    struct JavaStreamlineSession {
        JavaVM *javaVm = nullptr;
        jobject logListener = nullptr;
        jmethodID logMethod = nullptr;
        jobject apiErrorListener = nullptr;
        jmethodID apiErrorMethod = nullptr;
        std::vector<std::wstring> pluginPaths;
    };

    std::unique_ptr<JavaStreamlineSession> g_javaSession;

    using PFN_vkGetInstanceProcAddrSl = PFN_vkVoidFunction(VKAPI_PTR *)(VkInstance instance, const char *pName);

    HMODULE loadStreamlineInterposer() {
        HMODULE module = GetModuleHandleW(L"sl.interposer.dll");
        if (module) {
            return module;
        }
        if (!g_pluginPath.empty()) {
            std::filesystem::path dllPath(g_pluginPath);
            dllPath /= L"sl.interposer.dll";
            module = LoadLibraryW(dllPath.c_str());
            if (module) {
                return module;
            }
        }
        return LoadLibraryW(L"sl.interposer.dll");
    }

    // sl.interposer.dll is distributed separately from this bridge, so its exports are
    // resolved at runtime instead of linked through the import library. Every core entry
    // point goes through the callXxx wrappers below and degrades to eErrorNoPlugins
    // (SR_RETURN_CODE_CANNOT_FIND_LIBRARY) when the DLL is absent.
    struct StreamlineCore {
        PFun_slInit *init;
        PFun_slShutdown *shutdown;
        PFun_slIsFeatureSupported *isFeatureSupported;
        PFun_slIsFeatureLoaded *isFeatureLoaded;
        PFun_slSetFeatureLoaded *setFeatureLoaded;
        PFun_slEvaluateFeature *evaluateFeature;
        PFun_slAllocateResources *allocateResources;
        PFun_slFreeResources *freeResources;
        PFun_slSetTag *setTag;
        PFun_slSetTagForFrame *setTagForFrame;
        PFun_slGetFeatureRequirements *getFeatureRequirements;
        PFun_slGetFeatureVersion *getFeatureVersion;
        PFun_slSetConstants *setConstants;
        PFun_slGetFeatureFunction *getFeatureFunction;
        PFun_slGetNewFrameToken *getNewFrameToken;
        PFun_slSetVulkanInfo *setVulkanInfo;
    };

    std::mutex g_coreFunctionMutex;
    StreamlineCore g_coreFunctions{};
    bool g_coreFunctionsResolved = false;

    template<typename T>
    bool resolveCoreFunction(HMODULE module, const char *name, T *&out) {
        out = reinterpret_cast<T *>(GetProcAddress(module, name));
        return out != nullptr;
    }

    const StreamlineCore *coreFunctions() {
        std::lock_guard<std::mutex> lock(g_coreFunctionMutex);
        if (g_coreFunctionsResolved) {
            return &g_coreFunctions;
        }
        HMODULE module = loadStreamlineInterposer();
        if (!module) {
            return nullptr;
        }
        StreamlineCore core{};
        bool resolved = resolveCoreFunction(module, "slInit", core.init)
            && resolveCoreFunction(module, "slShutdown", core.shutdown)
            && resolveCoreFunction(module, "slIsFeatureSupported", core.isFeatureSupported)
            && resolveCoreFunction(module, "slIsFeatureLoaded", core.isFeatureLoaded)
            && resolveCoreFunction(module, "slSetFeatureLoaded", core.setFeatureLoaded)
            && resolveCoreFunction(module, "slEvaluateFeature", core.evaluateFeature)
            && resolveCoreFunction(module, "slAllocateResources", core.allocateResources)
            && resolveCoreFunction(module, "slFreeResources", core.freeResources)
            && resolveCoreFunction(module, "slSetTag", core.setTag)
            && resolveCoreFunction(module, "slSetTagForFrame", core.setTagForFrame)
            && resolveCoreFunction(module, "slGetFeatureRequirements", core.getFeatureRequirements)
            && resolveCoreFunction(module, "slGetFeatureVersion", core.getFeatureVersion)
            && resolveCoreFunction(module, "slSetConstants", core.setConstants)
            && resolveCoreFunction(module, "slGetFeatureFunction", core.getFeatureFunction)
            && resolveCoreFunction(module, "slGetNewFrameToken", core.getNewFrameToken)
            && resolveCoreFunction(module, "slSetVulkanInfo", core.setVulkanInfo);
        if (!resolved) {
            return nullptr;
        }
        g_coreFunctions = core;
        g_coreFunctionsResolved = true;
        return &g_coreFunctions;
    }

    template<typename Fn, typename... Args>
    sl::Result callCoreFunction(Fn *StreamlineCore::*member, Args &&... args) {
        const StreamlineCore *core = coreFunctions();
        return core
                   ? (core->*member)(std::forward<Args>(args)...)
                   : sl::Result::eErrorNoPlugins;
    }

    sl::Result callInit(const sl::Preferences &preferences) {
        return callCoreFunction(&StreamlineCore::init, preferences, sl::kSDKVersion);
    }

    sl::Result callShutdown() {
        return callCoreFunction(&StreamlineCore::shutdown);
    }

    sl::Result callIsFeatureSupported(sl::Feature feature, const sl::AdapterInfo &adapterInfo) {
        return callCoreFunction(&StreamlineCore::isFeatureSupported, feature, adapterInfo);
    }

    sl::Result callIsFeatureLoaded(sl::Feature feature, bool &loaded) {
        return callCoreFunction(&StreamlineCore::isFeatureLoaded, feature, loaded);
    }

    sl::Result callSetFeatureLoaded(sl::Feature feature, bool loaded) {
        return callCoreFunction(&StreamlineCore::setFeatureLoaded, feature, loaded);
    }

    sl::Result callEvaluateFeature(
        sl::Feature feature,
        const sl::FrameToken &frame,
        const sl::BaseStructure **inputs,
        uint32_t numInputs,
        sl::CommandBuffer *commandBuffer
    ) {
        return callCoreFunction(&StreamlineCore::evaluateFeature, feature, frame, inputs, numInputs, commandBuffer);
    }

    sl::Result callAllocateResources(
        sl::CommandBuffer *commandBuffer,
        sl::Feature feature,
        const sl::ViewportHandle &viewport
    ) {
        return callCoreFunction(&StreamlineCore::allocateResources, commandBuffer, feature, viewport);
    }

    sl::Result callFreeResources(sl::Feature feature, const sl::ViewportHandle &viewport) {
        return callCoreFunction(&StreamlineCore::freeResources, feature, viewport);
    }

    sl::Result callSetTag(
        const sl::ViewportHandle &viewport,
        const sl::ResourceTag *tags,
        uint32_t numTags,
        sl::CommandBuffer *commandBuffer
    ) {
        return callCoreFunction(&StreamlineCore::setTag, viewport, tags, numTags, commandBuffer);
    }

    sl::Result callSetTagForFrame(
        const sl::FrameToken &frame,
        const sl::ViewportHandle &viewport,
        const sl::ResourceTag *tags,
        uint32_t numTags,
        sl::CommandBuffer *commandBuffer
    ) {
        return callCoreFunction(&StreamlineCore::setTagForFrame, frame, viewport, tags, numTags, commandBuffer);
    }

    sl::Result callGetFeatureRequirements(sl::Feature feature, sl::FeatureRequirements &requirements) {
        return callCoreFunction(&StreamlineCore::getFeatureRequirements, feature, requirements);
    }

    sl::Result callGetFeatureVersion(sl::Feature feature, sl::FeatureVersion &version) {
        return callCoreFunction(&StreamlineCore::getFeatureVersion, feature, version);
    }

    sl::Result callSetConstants(
        const sl::Constants &values,
        const sl::FrameToken &frame,
        const sl::ViewportHandle &viewport
    ) {
        return callCoreFunction(&StreamlineCore::setConstants, values, frame, viewport);
    }

    sl::Result callGetFeatureFunction(sl::Feature feature, const char *functionName, void *&function) {
        return callCoreFunction(&StreamlineCore::getFeatureFunction, feature, functionName, function);
    }

    sl::Result callGetNewFrameToken(sl::FrameToken *&token, const uint32_t *frameIndex) {
        return callCoreFunction(&StreamlineCore::getNewFrameToken, token, frameIndex);
    }

    sl::Result callSetVulkanInfo(const sl::VulkanInfo &info) {
        return callCoreFunction(&StreamlineCore::setVulkanInfo, info);
    }

    void sendMessage(SRMessageType type, const wchar_t *message) {
        if (g_messageCallback && message) {
            g_messageCallback(type, message);
        }
    }

    void sendContextMessage(StreamlineDLSSPrivateData *privateData, SRMessageType type, const wchar_t *message) {
        if (privateData && privateData->messageCallback && message) {
            privateData->messageCallback(type, message);
            return;
        }
        sendMessage(type, message);
    }

    std::wstring utf8ToWide(const char *value) {
        if (!value || value[0] == '\0') {
            return {};
        }
        int wideLength = MultiByteToWideChar(CP_UTF8, 0, value, -1, nullptr, 0);
        if (wideLength <= 0) {
            return {};
        }
        std::wstring out(static_cast<size_t>(wideLength), L'\0');
        MultiByteToWideChar(CP_UTF8, 0, value, -1, out.data(), wideLength);
        if (!out.empty() && out.back() == L'\0') {
            out.pop_back();
        }
        return out;
    }

    std::string javaStringToUtf8(JNIEnv *env, jstring value) {
        if (!env || !value) {
            return {};
        }
        const char *chars = env->GetStringUTFChars(value, nullptr);
        if (!chars) {
            return {};
        }
        std::string out(chars);
        env->ReleaseStringUTFChars(value, chars);
        return out;
    }

    bool getJavaEnv(JavaVM *javaVm, JNIEnv *&env, bool &attached) {
        attached = false;
        env = nullptr;
        if (!javaVm) {
            return false;
        }
        jint result = javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8);
        if (result == JNI_OK) {
            return true;
        }
        if (result != JNI_EDETACHED || javaVm->AttachCurrentThread(reinterpret_cast<void **>(&env), nullptr) != JNI_OK) {
            env = nullptr;
            return false;
        }
        attached = true;
        return true;
    }

    void clearJavaException(JNIEnv *env) {
        if (env && env->ExceptionCheck()) {
            env->ExceptionClear();
        }
    }

    void sendJavaLogMessage(sl::LogType type, const char *message) {
        JavaStreamlineSession *session = g_javaSession.get();
        if (!session || !session->logListener || !session->logMethod || !message) {
            return;
        }
        JNIEnv *env = nullptr;
        bool attached = false;
        if (!getJavaEnv(session->javaVm, env, attached)) {
            return;
        }
        jstring javaMessage = env->NewStringUTF(message);
        if (javaMessage) {
            env->CallVoidMethod(session->logListener, session->logMethod, static_cast<jint>(type), javaMessage);
            env->DeleteLocalRef(javaMessage);
        }
        clearJavaException(env);
        if (attached) {
            session->javaVm->DetachCurrentThread();
        }
    }

    void sendJavaApiError(const sl::APIError &error) {
        JavaStreamlineSession *session = g_javaSession.get();
        if (!session || !session->apiErrorListener || !session->apiErrorMethod) {
            return;
        }
        JNIEnv *env = nullptr;
        bool attached = false;
        if (!getJavaEnv(session->javaVm, env, attached)) {
            return;
        }
        env->CallVoidMethod(
            session->apiErrorListener,
            session->apiErrorMethod,
            static_cast<jint>(error.vkRes)
        );
        clearJavaException(env);
        if (attached) {
            session->javaVm->DetachCurrentThread();
        }
    }

    void releaseJavaSessionRefs(JNIEnv *env, JavaStreamlineSession *session) {
        if (!env || !session) {
            return;
        }
        if (session->apiErrorListener) {
            env->DeleteGlobalRef(session->apiErrorListener);
            session->apiErrorListener = nullptr;
        }
        if (session->logListener) {
            env->DeleteGlobalRef(session->logListener);
            session->logListener = nullptr;
        }
    }

    bool isActiveJavaSession(jlong handle) {
        return g_streamlineInitialized
            && g_javaSession
            && reinterpret_cast<jlong>(g_javaSession.get()) == handle;
    }

    template<typename T>
    sl::Result resolveFeatureFunction(sl::Feature feature, const char *name, T *&outFunction) {
        void *address = nullptr;
        sl::Result result = callGetFeatureFunction(feature, name, address);
        if (result != sl::Result::eOk) {
            outFunction = nullptr;
            return result;
        }
        outFunction = reinterpret_cast<T *>(address);
        return outFunction ? sl::Result::eOk : sl::Result::eErrorMissingInputParameter;
    }

    sl::Result callDLSSGetOptimalSettings(const sl::DLSSOptions &options, sl::DLSSOptimalSettings &settings) {
        PFun_slDLSSGetOptimalSettings *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureDLSS, "slDLSSGetOptimalSettings", function);
        return result == sl::Result::eOk ? function(options, settings) : result;
    }

    sl::Result callDLSSGetState(const sl::ViewportHandle &viewport, sl::DLSSState &state) {
        PFun_slDLSSGetState *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureDLSS, "slDLSSGetState", function);
        return result == sl::Result::eOk ? function(viewport, state) : result;
    }

    sl::Result callDLSSSetOptions(const sl::ViewportHandle &viewport, const sl::DLSSOptions &options) {
        PFun_slDLSSSetOptions *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureDLSS, "slDLSSSetOptions", function);
        return result == sl::Result::eOk ? function(viewport, options) : result;
    }

    sl::Result callDLSSGGetState(const sl::ViewportHandle &viewport, sl::DLSSGState &state, const sl::DLSSGOptions *options) {
        PFun_slDLSSGGetState *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureDLSS_G, "slDLSSGGetState", function);
        return result == sl::Result::eOk ? function(viewport, state, options) : result;
    }

    sl::Result callDLSSGSetOptions(const sl::ViewportHandle &viewport, const sl::DLSSGOptions &options) {
        PFun_slDLSSGSetOptions *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureDLSS_G, "slDLSSGSetOptions", function);
        return result == sl::Result::eOk ? function(viewport, options) : result;
    }

    sl::Result callPCLGetState(sl::PCLState &state) {
        PFun_slPCLGetState *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeaturePCL, "slPCLGetState", function);
        return result == sl::Result::eOk ? function(state) : result;
    }

    sl::Result callPCLSetMarker(sl::PCLMarker marker, const sl::FrameToken &frame) {
        PFun_slPCLSetMarker *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeaturePCL, "slPCLSetMarker", function);
        return result == sl::Result::eOk ? function(marker, frame) : result;
    }

    sl::Result callPCLSetOptions(const sl::PCLOptions &options) {
        PFun_slPCLSetOptions *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeaturePCL, "slPCLSetOptions", function);
        return result == sl::Result::eOk ? function(options) : result;
    }

    sl::Result callReflexGetState(sl::ReflexState &state) {
        PFun_slReflexGetState *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureReflex, "slReflexGetState", function);
        return result == sl::Result::eOk ? function(state) : result;
    }

    sl::Result callReflexSleep(const sl::FrameToken &frame) {
        PFun_slReflexSleep *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureReflex, "slReflexSleep", function);
        return result == sl::Result::eOk ? function(frame) : result;
    }

    sl::Result callReflexSetOptions(const sl::ReflexOptions &options) {
        PFun_slReflexSetOptions *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureReflex, "slReflexSetOptions", function);
        return result == sl::Result::eOk ? function(options) : result;
    }

    sl::Result callReflexSetCameraData(
        const sl::ViewportHandle &viewport,
        const sl::FrameToken &frame,
        const sl::ReflexCameraData &data
    ) {
        PFun_slReflexSetCameraData *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureReflex, "slReflexSetCameraData", function);
        return result == sl::Result::eOk ? function(viewport, frame, data) : result;
    }

    sl::Result callReflexGetPredictedCameraData(
        const sl::ViewportHandle &viewport,
        const sl::FrameToken &frame,
        sl::ReflexPredictedCameraData &data
    ) {
        PFun_slReflexGetPredictedCameraData *function = nullptr;
        sl::Result result = resolveFeatureFunction(sl::kFeatureReflex, "slReflexGetPredictedCameraData", function);
        return result == sl::Result::eOk ? function(viewport, frame, data) : result;
    }

    PFN_vkGetInstanceProcAddrSl getStreamlineVkGetInstanceProcAddr() {
        HMODULE interposer = loadStreamlineInterposer();
        if (!interposer) {
            g_lastVkResult = VK_ERROR_INITIALIZATION_FAILED;
            return nullptr;
        }
        auto proc = reinterpret_cast<PFN_vkGetInstanceProcAddrSl>(
            GetProcAddress(interposer, "vkGetInstanceProcAddr")
        );
        if (!proc) {
            g_lastVkResult = VK_ERROR_INITIALIZATION_FAILED;
        }
        return proc;
    }

    void slLogCallback(sl::LogType type, const char *message) {
        if (!message) {
            return;
        }
        sendJavaLogMessage(type, message);
        std::wstring wideMessage = utf8ToWide(message);
        SRMessageType srType = SR_MESSAGE_TYPE_INFO;
        if (type == sl::LogType::eWarn) {
            srType = SR_MESSAGE_TYPE_WARNING;
        } else if (type == sl::LogType::eError) {
            srType = SR_MESSAGE_TYPE_ERROR;
        }
        sendMessage(srType, wideMessage.c_str());
    }

    SRReturnCode resultToReturnCode(sl::Result result) {
        switch (result) {
            case sl::Result::eOk:
                return SR_RETURN_CODE_OK;
            case sl::Result::eErrorInvalidParameter:
            case sl::Result::eErrorMissingInputParameter:
                return SR_RETURN_CODE_INVALID_ARGUMENT;
            case sl::Result::eErrorFeatureNotSupported:
            case sl::Result::eErrorNoSupportedAdapterFound:
            case sl::Result::eErrorAdapterNotSupported:
                return SR_RETURN_CODE_UNSUPPORTED;
            case sl::Result::eErrorNoPlugins:
            case sl::Result::eErrorFeatureMissing:
            case sl::Result::eErrorFeatureFailedToLoad:
                return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
            default:
                return SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
    }

    void reportResult(StreamlineDLSSPrivateData *privateData, const wchar_t *operation, sl::Result result) {
        if (result == sl::Result::eOk) {
            return;
        }
        std::wstring message = operation;
        message += L" failed. Streamline result: ";
        message += utf8ToWide(sl::getResultAsStr(result));
        message += L" (";
        message += std::to_wstring(static_cast<int>(result));
        message += L")";
        sendContextMessage(privateData, SR_MESSAGE_TYPE_ERROR, message.c_str());
    }

    bool hasFlag(uint32_t flags, SRUpscaleContextCreateFlags flag) {
        return (flags & static_cast<uint32_t>(flag)) != 0;
    }

    sl::DLSSPreset mapDLSSPreset(int32_t preset) {
        if (preset <= 0) {
            return sl::DLSSPreset::eDefault;
        }
        if (preset >= static_cast<int32_t>(sl::DLSSPreset::eCount)) {
            return sl::DLSSPreset::eDefault;
        }
        return static_cast<sl::DLSSPreset>(preset);
    }

    sl::DLSSMode inferDLSSMode(const SRCreateUpscaleContextDesc *desc) {
        if (!desc || desc->renderSize.x == 0 || desc->renderSize.y == 0) {
            return sl::DLSSMode::eBalanced;
        }
        float ratioX = static_cast<float>(desc->upscaledSize.x) / static_cast<float>(desc->renderSize.x);
        float ratioY = static_cast<float>(desc->upscaledSize.y) / static_cast<float>(desc->renderSize.y);
        float ratio = (std::max)(ratioX, ratioY);
        if (ratio < 1.05f) {
            return sl::DLSSMode::eDLAA;
        }
        if (ratio < 1.35f) {
            return sl::DLSSMode::eMaxQuality;
        }
        if (ratio < 1.75f) {
            return sl::DLSSMode::eBalanced;
        }
        if (ratio < 2.5f) {
            return sl::DLSSMode::eMaxPerformance;
        }
        return sl::DLSSMode::eUltraPerformance;
    }

    VkImageUsageFlags srUsageToVkUsage(SRResourceUsage usage) {
        VkImageUsageFlags result = VK_IMAGE_USAGE_SAMPLED_BIT;
        if ((usage & SR_RESOURCE_USAGE_RENDERTARGET) != 0) {
            result |= VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT;
        }
        if ((usage & SR_RESOURCE_USAGE_UAV) != 0) {
            result |= VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
        }
        if ((usage & SR_RESOURCE_USAGE_DEPTHTARGET) != 0) {
            result |= VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
        }
        if ((usage & SR_RESOURCE_USAGE_INDIRECT) != 0) {
            result |= VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
        }
        return result;
    }

    sl::Resource makeResource(const SRTextureResource &src, VkImageLayout layout) {
        sl::Resource resource{};
        resource.type = sl::ResourceType::eTex2d;
        resource.native = src.handle;
        resource.view = src.imageView;
        resource.state = static_cast<uint32_t>(layout);
        resource.width = src.desc.width;
        resource.height = src.desc.height;
        resource.nativeFormat = static_cast<uint32_t>(srTextureFormatToVkFormat(src.desc.format));
        resource.mipLevels = (std::max)(1u, src.desc.mipmapCount);
        resource.arrayLayers = 1;
        resource.usage = srUsageToVkUsage(src.desc.usage);
        return resource;
    }

    sl::Extent makeExtent(uint32_t width, uint32_t height) {
        sl::Extent extent{};
        extent.left = 0;
        extent.top = 0;
        extent.width = width;
        extent.height = height;
        return extent;
    }

    jfieldID getField(JNIEnv *env, jobject object, const char *name, const char *signature) {
        if (!env || !object) {
            return nullptr;
        }
        jclass type = env->GetObjectClass(object);
        if (!type) {
            return nullptr;
        }
        jfieldID field = env->GetFieldID(type, name, signature);
        env->DeleteLocalRef(type);
        return field;
    }

    jint getInt(JNIEnv *env, jobject object, const char *name) {
        jfieldID field = getField(env, object, name, "I");
        return field ? env->GetIntField(object, field) : 0;
    }

    jlong getLong(JNIEnv *env, jobject object, const char *name) {
        jfieldID field = getField(env, object, name, "J");
        return field ? env->GetLongField(object, field) : 0;
    }

    jfloat getFloat(JNIEnv *env, jobject object, const char *name) {
        jfieldID field = getField(env, object, name, "F");
        return field ? env->GetFloatField(object, field) : 0.0f;
    }

    jbyte getByte(JNIEnv *env, jobject object, const char *name) {
        jfieldID field = getField(env, object, name, "B");
        return field ? env->GetByteField(object, field) : 0;
    }

    jboolean getBoolean(JNIEnv *env, jobject object, const char *name) {
        jfieldID field = getField(env, object, name, "Z");
        return field ? env->GetBooleanField(object, field) : JNI_FALSE;
    }

    jobject getObject(JNIEnv *env, jobject object, const char *name, const char *signature) {
        jfieldID field = getField(env, object, name, signature);
        return field ? env->GetObjectField(object, field) : nullptr;
    }

    void setInt(JNIEnv *env, jobject object, const char *name, jint value) {
        if (jfieldID field = getField(env, object, name, "I")) {
            env->SetIntField(object, field, value);
        }
    }

    void setLong(JNIEnv *env, jobject object, const char *name, jlong value) {
        if (jfieldID field = getField(env, object, name, "J")) {
            env->SetLongField(object, field, value);
        }
    }

    void setFloat(JNIEnv *env, jobject object, const char *name, jfloat value) {
        if (jfieldID field = getField(env, object, name, "F")) {
            env->SetFloatField(object, field, value);
        }
    }

    void setByte(JNIEnv *env, jobject object, const char *name, jbyte value) {
        if (jfieldID field = getField(env, object, name, "B")) {
            env->SetByteField(object, field, value);
        }
    }

    void setBoolean(JNIEnv *env, jobject object, const char *name, jboolean value) {
        if (jfieldID field = getField(env, object, name, "Z")) {
            env->SetBooleanField(object, field, value);
        }
    }

    void setObject(JNIEnv *env, jobject object, const char *name, const char *signature, jobject value) {
        if (jfieldID field = getField(env, object, name, signature)) {
            env->SetObjectField(object, field, value);
        }
    }

    bool readFloatArray(JNIEnv *env, jobject object, const char *name, float *destination, jsize count) {
        jobject rawArray = getObject(env, object, name, "[F");
        auto array = reinterpret_cast<jfloatArray>(rawArray);
        if (!array || env->GetArrayLength(array) != count) {
            if (array) {
                env->DeleteLocalRef(array);
            }
            return false;
        }
        env->GetFloatArrayRegion(array, 0, count, destination);
        env->DeleteLocalRef(array);
        return !env->ExceptionCheck();
    }

    void writeFloatArray(JNIEnv *env, jobject object, const char *name, const float *source, jsize count) {
        jfloatArray array = env->NewFloatArray(count);
        if (!array) {
            return;
        }
        env->SetFloatArrayRegion(array, 0, count, source);
        setObject(env, object, name, "[F", array);
        env->DeleteLocalRef(array);
    }

    bool readMatrix(JNIEnv *env, jobject object, const char *name, sl::float4x4 &matrix) {
        float values[16]{};
        if (!readFloatArray(env, object, name, values, 16)) {
            return false;
        }
        std::memcpy(&matrix, values, sizeof(values));
        return true;
    }

    void writeMatrix(JNIEnv *env, jobject object, const char *name, const sl::float4x4 &matrix) {
        writeFloatArray(env, object, name, reinterpret_cast<const float *>(&matrix), 16);
    }

    void writeVersion(JNIEnv *env, jobject object, const sl::Version &version) {
        if (!object) {
            return;
        }
        setInt(env, object, "major", static_cast<jint>(version.major));
        setInt(env, object, "minor", static_cast<jint>(version.minor));
        setInt(env, object, "build", static_cast<jint>(version.build));
    }

    void setBooleanOut(JNIEnv *env, jbooleanArray out, bool value) {
        if (!out || env->GetArrayLength(out) == 0) {
            return;
        }
        jboolean result = value ? JNI_TRUE : JNI_FALSE;
        env->SetBooleanArrayRegion(out, 0, 1, &result);
    }

    void setLongOut(JNIEnv *env, jlongArray out, jlong value) {
        if (!out || env->GetArrayLength(out) == 0) {
            return;
        }
        env->SetLongArrayRegion(out, 0, 1, &value);
    }

    std::vector<std::wstring> readWideStringArray(JNIEnv *env, jobjectArray values) {
        std::vector<std::wstring> result;
        if (!values) {
            return result;
        }
        jsize count = env->GetArrayLength(values);
        result.reserve(static_cast<size_t>(count));
        for (jsize index = 0; index < count; ++index) {
            auto value = reinterpret_cast<jstring>(env->GetObjectArrayElement(values, index));
            std::string utf8 = javaStringToUtf8(env, value);
            if (value) {
                env->DeleteLocalRef(value);
            }
            if (!utf8.empty()) {
                result.push_back(utf8ToWide(utf8.c_str()));
            }
        }
        return result;
    }

    std::vector<sl::Feature> readFeatures(JNIEnv *env, jintArray values) {
        std::vector<sl::Feature> result;
        if (!values) {
            return result;
        }
        jsize count = env->GetArrayLength(values);
        if (count <= 0) {
            return result;
        }
        std::vector<jint> raw(static_cast<size_t>(count));
        env->GetIntArrayRegion(values, 0, count, raw.data());
        result.reserve(static_cast<size_t>(count));
        for (jint value : raw) {
            result.push_back(static_cast<sl::Feature>(value));
        }
        return result;
    }

    void writeIntArrayField(JNIEnv *env, jobject object, const char *name, const uint32_t *values, uint32_t count) {
        jintArray array = env->NewIntArray(static_cast<jsize>(count));
        if (!array) {
            return;
        }
        if (count > 0) {
            std::vector<jint> converted(count);
            for (uint32_t index = 0; index < count; ++index) {
                converted[index] = static_cast<jint>(values[index]);
            }
            env->SetIntArrayRegion(array, 0, static_cast<jsize>(count), converted.data());
        }
        setObject(env, object, name, "[I", array);
        env->DeleteLocalRef(array);
    }

    void writeStringArrayField(JNIEnv *env, jobject object, const char *name, const char *const *values, uint32_t count) {
        jclass stringClass = env->FindClass("java/lang/String");
        if (!stringClass) {
            return;
        }
        jobjectArray array = env->NewObjectArray(static_cast<jsize>(count), stringClass, nullptr);
        env->DeleteLocalRef(stringClass);
        if (!array) {
            return;
        }
        for (uint32_t index = 0; index < count; ++index) {
            jstring value = env->NewStringUTF(values && values[index] ? values[index] : "");
            if (value) {
                env->SetObjectArrayElement(array, static_cast<jsize>(index), value);
                env->DeleteLocalRef(value);
            }
        }
        setObject(env, object, name, "[Ljava/lang/String;", array);
        env->DeleteLocalRef(array);
    }

    bool readResource(JNIEnv *env, jobject object, sl::Resource &resource) {
        if (!object) {
            return false;
        }
        resource.type = static_cast<sl::ResourceType>(getInt(env, object, "type"));
        resource.native = reinterpret_cast<void *>(getLong(env, object, "nativeHandle"));
        resource.memory = reinterpret_cast<void *>(getLong(env, object, "memory"));
        resource.view = reinterpret_cast<void *>(getLong(env, object, "view"));
        resource.state = static_cast<uint32_t>(getInt(env, object, "state"));
        resource.width = static_cast<uint32_t>(getInt(env, object, "width"));
        resource.height = static_cast<uint32_t>(getInt(env, object, "height"));
        resource.nativeFormat = static_cast<uint32_t>(getInt(env, object, "nativeFormat"));
        resource.mipLevels = static_cast<uint32_t>(getInt(env, object, "mipLevels"));
        resource.arrayLayers = static_cast<uint32_t>(getInt(env, object, "arrayLayers"));
        resource.gpuVirtualAddress = static_cast<uint64_t>(getLong(env, object, "gpuVirtualAddress"));
        resource.flags = static_cast<uint32_t>(getInt(env, object, "flags"));
        resource.usage = static_cast<uint32_t>(getInt(env, object, "usage"));
        resource.reserved = static_cast<uint32_t>(getInt(env, object, "reserved"));
        return resource.native != nullptr;
    }

    struct OwnedResourceTag {
        sl::Resource resource{};
        sl::Extent extent{};
        std::unique_ptr<sl::ResourceTag> tag;
    };

    bool readResourceTag(JNIEnv *env, jobject object, OwnedResourceTag &out) {
        if (!object) {
            return false;
        }
        constexpr const char *kResourceSignature =
            "Lio/homo/superresolution/core/streamline/StreamlineTypes$Resource;";
        constexpr const char *kExtentSignature =
            "Lio/homo/superresolution/core/streamline/StreamlineTypes$Extent;";
        jobject resourceObject = getObject(env, object, "resource", kResourceSignature);
        if (!readResource(env, resourceObject, out.resource)) {
            if (resourceObject) {
                env->DeleteLocalRef(resourceObject);
            }
            return false;
        }
        env->DeleteLocalRef(resourceObject);
        jobject extentObject = getObject(env, object, "extent", kExtentSignature);
        const sl::Extent *extent = nullptr;
        if (extentObject) {
            out.extent.top = static_cast<uint32_t>(getInt(env, extentObject, "top"));
            out.extent.left = static_cast<uint32_t>(getInt(env, extentObject, "left"));
            out.extent.width = static_cast<uint32_t>(getInt(env, extentObject, "width"));
            out.extent.height = static_cast<uint32_t>(getInt(env, extentObject, "height"));
            extent = &out.extent;
            env->DeleteLocalRef(extentObject);
        }
        out.tag = std::make_unique<sl::ResourceTag>(
            &out.resource,
            static_cast<sl::BufferType>(getInt(env, object, "type")),
            static_cast<sl::ResourceLifecycle>(getInt(env, object, "lifecycle")),
            extent
        );
        return true;
    }

    bool readResourceTags(JNIEnv *env, jobjectArray array, std::vector<OwnedResourceTag> &owned) {
        if (!array) {
            return false;
        }
        jsize count = env->GetArrayLength(array);
        if (count <= 0) {
            return false;
        }
        owned.resize(static_cast<size_t>(count));
        for (jsize index = 0; index < count; ++index) {
            jobject tagObject = env->GetObjectArrayElement(array, index);
            bool valid = readResourceTag(env, tagObject, owned[static_cast<size_t>(index)]);
            if (tagObject) {
                env->DeleteLocalRef(tagObject);
            }
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    bool readConstants(JNIEnv *env, jobject object, sl::Constants &constants) {
        if (!object
            || !readMatrix(env, object, "cameraViewToClip", constants.cameraViewToClip)
            || !readMatrix(env, object, "clipToCameraView", constants.clipToCameraView)
            || !readMatrix(env, object, "clipToLensClip", constants.clipToLensClip)
            || !readMatrix(env, object, "clipToPrevClip", constants.clipToPrevClip)
            || !readMatrix(env, object, "prevClipToClip", constants.prevClipToClip)) {
            return false;
        }
        constants.jitterOffset = {getFloat(env, object, "jitterOffsetX"), getFloat(env, object, "jitterOffsetY")};
        constants.mvecScale = {getFloat(env, object, "motionVectorScaleX"), getFloat(env, object, "motionVectorScaleY")};
        constants.cameraPinholeOffset = {getFloat(env, object, "cameraPinholeOffsetX"), getFloat(env, object, "cameraPinholeOffsetY")};
        constants.cameraPos = {getFloat(env, object, "cameraPosX"), getFloat(env, object, "cameraPosY"), getFloat(env, object, "cameraPosZ")};
        constants.cameraUp = {getFloat(env, object, "cameraUpX"), getFloat(env, object, "cameraUpY"), getFloat(env, object, "cameraUpZ")};
        constants.cameraRight = {getFloat(env, object, "cameraRightX"), getFloat(env, object, "cameraRightY"), getFloat(env, object, "cameraRightZ")};
        constants.cameraFwd = {getFloat(env, object, "cameraFwdX"), getFloat(env, object, "cameraFwdY"), getFloat(env, object, "cameraFwdZ")};
        constants.cameraNear = getFloat(env, object, "cameraNear");
        constants.cameraFar = getFloat(env, object, "cameraFar");
        constants.cameraFOV = getFloat(env, object, "cameraFov");
        constants.cameraAspectRatio = getFloat(env, object, "cameraAspectRatio");
        constants.motionVectorsInvalidValue = getFloat(env, object, "motionVectorsInvalidValue");
        constants.depthInverted = static_cast<sl::Boolean>(getByte(env, object, "depthInverted"));
        constants.cameraMotionIncluded = static_cast<sl::Boolean>(getByte(env, object, "cameraMotionIncluded"));
        constants.motionVectors3D = static_cast<sl::Boolean>(getByte(env, object, "motionVectors3D"));
        constants.reset = static_cast<sl::Boolean>(getByte(env, object, "reset"));
        constants.orthographicProjection = static_cast<sl::Boolean>(getByte(env, object, "orthographicProjection"));
        constants.motionVectorsDilated = static_cast<sl::Boolean>(getByte(env, object, "motionVectorsDilated"));
        constants.motionVectorsJittered = static_cast<sl::Boolean>(getByte(env, object, "motionVectorsJittered"));
        constants.minRelativeLinearDepthObjectSeparation = getFloat(env, object, "minRelativeLinearDepthObjectSeparation");
        return true;
    }

    void readDLSSOptions(JNIEnv *env, jobject object, sl::DLSSOptions &options) {
        options.mode = static_cast<sl::DLSSMode>(getInt(env, object, "mode"));
        options.outputWidth = static_cast<uint32_t>(getInt(env, object, "outputWidth"));
        options.outputHeight = static_cast<uint32_t>(getInt(env, object, "outputHeight"));
        options.sharpness = getFloat(env, object, "sharpness");
        options.preExposure = getFloat(env, object, "preExposure");
        options.exposureScale = getFloat(env, object, "exposureScale");
        options.colorBuffersHDR = static_cast<sl::Boolean>(getByte(env, object, "colorBuffersHdr"));
        options.indicatorInvertAxisX = static_cast<sl::Boolean>(getByte(env, object, "indicatorInvertAxisX"));
        options.indicatorInvertAxisY = static_cast<sl::Boolean>(getByte(env, object, "indicatorInvertAxisY"));
        options.dlaaPreset = static_cast<sl::DLSSPreset>(getInt(env, object, "dlaaPreset"));
        options.qualityPreset = static_cast<sl::DLSSPreset>(getInt(env, object, "qualityPreset"));
        options.balancedPreset = static_cast<sl::DLSSPreset>(getInt(env, object, "balancedPreset"));
        options.performancePreset = static_cast<sl::DLSSPreset>(getInt(env, object, "performancePreset"));
        options.ultraPerformancePreset = static_cast<sl::DLSSPreset>(getInt(env, object, "ultraPerformancePreset"));
        options.ultraQualityPreset = static_cast<sl::DLSSPreset>(getInt(env, object, "ultraQualityPreset"));
        options.useAutoExposure = static_cast<sl::Boolean>(getByte(env, object, "useAutoExposure"));
        options.alphaUpscalingEnabled = static_cast<sl::Boolean>(getByte(env, object, "alphaUpscalingEnabled"));
    }

    void readDLSSGOptions(JNIEnv *env, jobject object, sl::DLSSGOptions &options) {
        options.mode = static_cast<sl::DLSSGMode>(getInt(env, object, "mode"));
        options.numFramesToGenerate = static_cast<uint32_t>(getInt(env, object, "numFramesToGenerate"));
        options.flags = static_cast<sl::DLSSGFlags>(getInt(env, object, "flags"));
        options.dynamicResWidth = static_cast<uint32_t>(getInt(env, object, "dynamicResWidth"));
        options.dynamicResHeight = static_cast<uint32_t>(getInt(env, object, "dynamicResHeight"));
        options.numBackBuffers = static_cast<uint32_t>(getInt(env, object, "numBackBuffers"));
        options.mvecDepthWidth = static_cast<uint32_t>(getInt(env, object, "motionVectorDepthWidth"));
        options.mvecDepthHeight = static_cast<uint32_t>(getInt(env, object, "motionVectorDepthHeight"));
        options.colorWidth = static_cast<uint32_t>(getInt(env, object, "colorWidth"));
        options.colorHeight = static_cast<uint32_t>(getInt(env, object, "colorHeight"));
        options.colorBufferFormat = static_cast<uint32_t>(getInt(env, object, "colorBufferFormat"));
        options.mvecBufferFormat = static_cast<uint32_t>(getInt(env, object, "motionVectorBufferFormat"));
        options.depthBufferFormat = static_cast<uint32_t>(getInt(env, object, "depthBufferFormat"));
        options.hudLessBufferFormat = static_cast<uint32_t>(getInt(env, object, "hudLessBufferFormat"));
        options.uiBufferFormat = static_cast<uint32_t>(getInt(env, object, "uiBufferFormat"));
        options.bReserved15 = static_cast<sl::Boolean>(getByte(env, object, "reserved15"));
        options.queueParallelismMode = static_cast<sl::DLSSGQueueParallelismMode>(getInt(env, object, "queueParallelismMode"));
        options.enableUserInterfaceRecomposition = static_cast<sl::Boolean>(getByte(env, object, "enableUserInterfaceRecomposition"));
        options.dynamicTargetFrameRate = getFloat(env, object, "dynamicTargetFrameRate");
    }

    void readReflexCameraData(JNIEnv *env, jobject object, sl::ReflexCameraData &data) {
        readMatrix(env, object, "worldToViewMatrix", data.worldToViewMatrix);
        readMatrix(env, object, "viewToClipMatrix", data.viewToClipMatrix);
        readMatrix(env, object, "previousRenderedWorldToViewMatrix", data.prevRenderedWorldToViewMatrix);
        readMatrix(env, object, "previousRenderedViewToClipMatrix", data.prevRenderedViewToClipMatrix);
    }

    void writeReflexPredictedCameraData(JNIEnv *env, jobject object, const sl::ReflexPredictedCameraData &data) {
        writeMatrix(env, object, "predictedWorldToViewMatrix", data.predictedWorldToViewMatrix);
        writeMatrix(env, object, "predictedViewToClipMatrix", data.predictedViewToClipMatrix);
    }

    sl::Result initStreamline(const char *pluginPath, const char *logPath, SRMessageCallback messageCallback) {
        std::lock_guard<std::mutex> lock(g_streamlineMutex);
        if (messageCallback) {
            g_messageCallback = messageCallback;
        }
        if (g_streamlineInitialized) {
            return sl::Result::eOk;
        }

        g_pluginPath = utf8ToWide(pluginPath);
        g_logPath = utf8ToWide(logPath);
        const wchar_t *pathsToPlugins[] = {g_pluginPath.c_str()};
        std::vector<sl::Feature> features = {sl::kFeatureDLSS};

        sl::Preferences preferences{};
        preferences.logLevel = sl::LogLevel::eDefault;
        preferences.logMessageCallback = slLogCallback;
        preferences.pathsToPlugins = g_pluginPath.empty() ? nullptr : pathsToPlugins;
        preferences.numPathsToPlugins = g_pluginPath.empty() ? 0 : 1;
        preferences.pathToLogsAndData = g_logPath.empty() ? nullptr : g_logPath.c_str();
        preferences.flags = sl::PreferenceFlags::eDisableCLStateTracking |
                            sl::PreferenceFlags::eAllowOTA |
                            sl::PreferenceFlags::eLoadDownloadedPlugins |
                            sl::PreferenceFlags::eUseFrameBasedResourceTagging;
        preferences.featuresToLoad = features.data();
        preferences.numFeaturesToLoad = static_cast<uint32_t>(features.size());
        preferences.engine = sl::EngineType::eCustom;
        preferences.engineVersion = "SuperResolution";
        preferences.projectId = kProjectId;
        preferences.renderAPI = sl::RenderAPI::eVulkan;

        sl::Result result = callInit(preferences);
        if (result != sl::Result::eOk) {
            std::wstring message = L"slInit failed. Streamline result: ";
            message += utf8ToWide(sl::getResultAsStr(result));
            message += L" (";
            message += std::to_wstring(static_cast<int>(result));
            message += L")";
            sendMessage(SR_MESSAGE_TYPE_ERROR, message.c_str());
            return result;
        }

        g_streamlineInitialized = true;
        g_dlssOptionsInitialized = false;
        sendMessage(SR_MESSAGE_TYPE_INFO, L"Streamline initialized.");
        return sl::Result::eOk;
    }

    SRReturnCode requireStreamlineInitializedFromDesc(const SRCreateUpscaleContextDesc *desc) {
        if (!desc) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        std::lock_guard<std::mutex> lock(g_streamlineMutex);
        if (desc->messageCallback) {
            g_messageCallback = desc->messageCallback;
        }
        if (g_streamlineInitialized) {
            return SR_RETURN_CODE_OK;
        }
        const wchar_t *message = L"Streamline is not initialized. Call Streamline.initEarly/prepareEarly before creating the SR Vulkan context.";
        if (desc->messageCallback) {
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, message);
        } else {
            sendMessage(SR_MESSAGE_TYPE_ERROR, message);
        }
        return SR_RETURN_CODE_UNEXPECTED_ERROR;
    }
}

extern "C" {
    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nOpen(
        JNIEnv *env,
        jclass,
        jboolean showConsole,
        jint logLevel,
        jlong preferenceFlags,
        jobjectArray pluginPaths,
        jstring logPath,
        jintArray features,
        jint applicationId,
        jint engine,
        jstring engineVersion,
        jstring projectId,
        jobject logListener,
        jlongArray outHandle
    ) {
        if (!env || !outHandle || env->GetArrayLength(outHandle) == 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }

        std::lock_guard<std::mutex> lock(g_streamlineMutex);
        if (g_streamlineInitialized) {
            return static_cast<jint>(sl::Result::eErrorInvalidState);
        }
        if (g_javaSession) {
            releaseJavaSessionRefs(env, g_javaSession.get());
            g_javaSession.reset();
            g_pluginPath.clear();
            g_logPath.clear();
        }

        auto session = std::make_unique<JavaStreamlineSession>();
        if (env->GetJavaVM(&session->javaVm) != JNI_OK) {
            return static_cast<jint>(sl::Result::eErrorExceptionHandler);
        }
        if (logListener) {
            session->logListener = env->NewGlobalRef(logListener);
            jclass listenerClass = env->GetObjectClass(logListener);
            session->logMethod = listenerClass
                ? env->GetMethodID(listenerClass, "onLog", "(ILjava/lang/String;)V")
                : nullptr;
            if (listenerClass) {
                env->DeleteLocalRef(listenerClass);
            }
            if (!session->logListener || !session->logMethod) {
                releaseJavaSessionRefs(env, session.get());
                return static_cast<jint>(sl::Result::eErrorInvalidParameter);
            }
        }

        session->pluginPaths = readWideStringArray(env, pluginPaths);
        std::vector<const wchar_t *> pluginPointers;
        pluginPointers.reserve(session->pluginPaths.size());
        for (const std::wstring &path : session->pluginPaths) {
            pluginPointers.push_back(path.c_str());
        }
        std::vector<sl::Feature> requestedFeatures = readFeatures(env, features);
        if (requestedFeatures.empty()) {
            releaseJavaSessionRefs(env, session.get());
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        std::string engineVersionUtf8 = javaStringToUtf8(env, engineVersion);
        std::string projectIdUtf8 = javaStringToUtf8(env, projectId);
        std::string logPathUtf8 = javaStringToUtf8(env, logPath);
        std::wstring logPathWide = utf8ToWide(logPathUtf8.c_str());

        sl::Preferences preferences{};
        preferences.showConsole = showConsole == JNI_TRUE;
        preferences.logLevel = static_cast<sl::LogLevel>(logLevel);
        preferences.logMessageCallback = slLogCallback;
        preferences.pathsToPlugins = pluginPointers.empty() ? nullptr : pluginPointers.data();
        preferences.numPathsToPlugins = static_cast<uint32_t>(pluginPointers.size());
        preferences.pathToLogsAndData = logPathWide.empty() ? nullptr : logPathWide.c_str();
        preferences.flags = static_cast<sl::PreferenceFlags>(preferenceFlags);
        preferences.featuresToLoad = requestedFeatures.data();
        preferences.numFeaturesToLoad = static_cast<uint32_t>(requestedFeatures.size());
        preferences.applicationId = static_cast<uint32_t>(applicationId);
        preferences.engine = static_cast<sl::EngineType>(engine);
        preferences.engineVersion = engineVersionUtf8.empty() ? nullptr : engineVersionUtf8.c_str();
        preferences.projectId = projectIdUtf8.empty() ? nullptr : projectIdUtf8.c_str();
        preferences.renderAPI = sl::RenderAPI::eVulkan;

        g_javaSession = std::move(session);
        sl::Result result = callInit(preferences);
        if (result != sl::Result::eOk) {
            releaseJavaSessionRefs(env, g_javaSession.get());
            g_javaSession.reset();
            return static_cast<jint>(result);
        }

        g_streamlineInitialized = true;
        g_dlssOptionsInitialized = false;
        g_pluginPath = g_javaSession->pluginPaths.empty() ? L"" : g_javaSession->pluginPaths.front();
        g_logPath = logPathWide;
        setLongOut(env, outHandle, reinterpret_cast<jlong>(g_javaSession.get()));
        return static_cast<jint>(sl::Result::eOk);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nClose(
        JNIEnv *env,
        jclass,
        jlong session
    ) {
        std::lock_guard<std::mutex> lock(g_streamlineMutex);
        if (!g_javaSession || reinterpret_cast<jlong>(g_javaSession.get()) != session) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::Result result = g_streamlineInitialized ? callShutdown() : sl::Result::eOk;
        g_streamlineInitialized = false;
        g_dlssOptionsInitialized = false;
        releaseJavaSessionRefs(env, g_javaSession.get());
        g_javaSession.reset();
        g_pluginPath.clear();
        g_logPath.clear();
        return static_cast<jint>(result);
    }

    JNIEXPORT jboolean JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nIsSessionActive(
        JNIEnv *,
        jclass,
        jlong session
    ) {
        std::lock_guard<std::mutex> lock(g_streamlineMutex);
        return isActiveJavaSession(session) ? JNI_TRUE : JNI_FALSE;
    }

    SR_API SRReturnCode srStreamlineInit(const char *pluginPath, const char *logPath, SRMessageCallback messageCallback) {
        return resultToReturnCode(initStreamline(pluginPath, logPath, messageCallback));
    }

    SR_API SRReturnCode srStreamlineShutdown() {
        std::lock_guard<std::mutex> lock(g_streamlineMutex);
        if (!g_streamlineInitialized) {
            return SR_RETURN_CODE_OK;
        }
        sl::Result result = callShutdown();
        g_streamlineInitialized = false;
        g_dlssOptionsInitialized = false;
        if (result != sl::Result::eOk) {
            return resultToReturnCode(result);
        }
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srStreamlineIsDLSSGSupported(bool *outSupported) {
        if (!outSupported) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        *outSupported = false;
        if (!g_streamlineInitialized) {
            return SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        sl::FeatureRequirements requirements{};
        sl::Result result = callGetFeatureRequirements(sl::kFeatureDLSS_G, requirements);
        if (result == sl::Result::eOk) {
            *outSupported = true;
            return SR_RETURN_CODE_OK;
        }
        if (result == sl::Result::eErrorFeatureMissing ||
            result == sl::Result::eErrorFeatureFailedToLoad ||
            result == sl::Result::eErrorFeatureNotSupported) {
            return SR_RETURN_CODE_OK;
        }
        return resultToReturnCode(result);
    }

    SR_API SRReturnCode srStreamlineDLSSGSetOptions(bool enabled, uint32_t framesToGenerate) {
        if (!g_streamlineInitialized) {
            return SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        sl::ViewportHandle viewport(kViewportId);
        sl::DLSSGOptions options{};
        options.mode = enabled ? sl::DLSSGMode::eOn : sl::DLSSGMode::eOff;
        options.numFramesToGenerate = (std::max)(1u, framesToGenerate);
        sl::Result result = callDLSSGSetOptions(viewport, options);
        return resultToReturnCode(result);
    }

    SR_API SRReturnCode srStreamlineDLSSGGetState(uint64_t *outEstimatedVram, uint32_t *outStatus) {
        if (!g_streamlineInitialized) {
            return SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        sl::ViewportHandle viewport(kViewportId);
        sl::DLSSGState state{};
        sl::Result result = callDLSSGGetState(viewport, state, nullptr);
        if (result != sl::Result::eOk) {
            return resultToReturnCode(result);
        }
        if (outEstimatedVram) {
            *outEstimatedVram = state.estimatedVRAMUsageInBytes;
        }
        if (outStatus) {
            *outStatus = static_cast<uint32_t>(state.status);
        }
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srStreamlineDLSSCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc) {
        if (!context || !desc) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        if (desc->renderApiType != SR_RENDER_API_TYPE_VULKAN) {
            if (desc->messageCallback) {
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"Streamline DLSS only supports Vulkan.");
            }
            return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
        }

        SRReturnCode initCode = requireStreamlineInitializedFromDesc(desc);
        if (initCode != SR_RETURN_CODE_OK) {
            return initCode;
        }

        context->desc = *const_cast<SRCreateUpscaleContextDesc *>(desc);
        auto *privateData = new StreamlineDLSSPrivateData();
        privateData->messageCallback = desc->messageCallback;
        context->userContext = privateData;
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srStreamlineDLSSInitUpscaleContext(SRUpscaleContext *context) {
        if (!context || !context->userContext) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        auto *privateData = reinterpret_cast<StreamlineDLSSPrivateData *>(context->userContext);
        sl::ViewportHandle viewport(kViewportId);
        sl::DLSSOptions options{};
        options.mode = inferDLSSMode(&context->desc);
        options.outputWidth = context->desc.upscaledSize.x;
        options.outputHeight = context->desc.upscaledSize.y;
        options.colorBuffersHDR = hasFlag(context->desc.flags, SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_HDR)
                                      ? sl::Boolean::eTrue
                                      : sl::Boolean::eFalse;
        options.useAutoExposure = hasFlag(context->desc.flags, SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_AUTO_EXPOSURE)
                                      ? sl::Boolean::eTrue
                                      : sl::Boolean::eFalse;
        options.alphaUpscalingEnabled = sl::Boolean::eFalse;

        const SRContextExtraParam *presetParam = srFindParam(&context->desc.extraParams, "DLSS_RENDER_PRESET");
        sl::DLSSPreset preset = sl::DLSSPreset::eDefault;
        if (presetParam && presetParam->valueType == SR_PARAM_VALUE_TYPE_INT32) {
            preset = mapDLSSPreset(presetParam->value.int32Value);
        }
        options.dlaaPreset = preset;
        options.qualityPreset = preset;
        options.balancedPreset = preset;
        options.performancePreset = preset;
        options.ultraPerformancePreset = preset;
        options.ultraQualityPreset = preset;

        sl::Result result = callDLSSSetOptions(viewport, options);
        reportResult(privateData, L"slDLSSSetOptions", result);
        g_dlssOptionsInitialized = result == sl::Result::eOk;
        return resultToReturnCode(result);
    }

    SR_API SRReturnCode srStreamlineDLSSDestroyUpscaleContext(SRUpscaleContext *context) {
        if (!context) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        if (g_streamlineInitialized) {
            sl::ViewportHandle viewport(kViewportId);
            callFreeResources(sl::kFeatureDLSS, viewport);
        }
        delete reinterpret_cast<StreamlineDLSSPrivateData *>(context->userContext);
        context->userContext = nullptr;
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srStreamlineDLSSQueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result, SRUpscaleContextQueryType queryType) {
        if (!context || !result) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        switch (queryType) {
            case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO:
                reinterpret_cast<SRQueryVersionResult *>(result)->versionId = SR_MAKE_VERSION(2, 12, 0);
                reinterpret_cast<SRQueryVersionResult *>(result)->versionNumber = SR_MAKE_VERSION(2, 12, 0);
                return SR_RETURN_CODE_OK;
            case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO: {
                sl::ViewportHandle viewport(kViewportId);
            sl::DLSSState state{};
            sl::Result slResult = callDLSSGetState(viewport, state);
                if (slResult != sl::Result::eOk) {
                    return resultToReturnCode(slResult);
                }
                reinterpret_cast<SRQueryGpuMemoryResult *>(result)->gpuMemory = state.estimatedVRAMUsageInBytes;
                return SR_RETURN_CODE_OK;
            }
            case SR_UPSCALE_CONTEXT_QUERY_AVAILABLE:
                reinterpret_cast<SRQueryAvailabilityResult *>(result)->isAvailable = g_streamlineInitialized && g_dlssOptionsInitialized;
                return SR_RETURN_CODE_OK;
            default:
                return SR_RETURN_CODE_INVALID_ARGUMENT;
        }
    }

    SR_API SRReturnCode srStreamlineDLSSDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc) {
        if (!context || !context->userContext || !desc) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        auto *privateData = reinterpret_cast<StreamlineDLSSPrivateData *>(context->userContext);
        if (!g_streamlineInitialized) {
            sendContextMessage(privateData, SR_MESSAGE_TYPE_ERROR, L"Streamline is not initialized.");
            return SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        if (!desc->color.exist || !desc->output.exist || !desc->depth.exist || !desc->motionVectors.exist) {
            sendContextMessage(privateData, SR_MESSAGE_TYPE_ERROR, L"Streamline DLSS requires color, output, depth and motion vector resources.");
            return SR_RETURN_CODE_INVALID_ARGUMENT;
        }

        const SRContextExtraParam *frameTokenParam = srFindParam(&desc->extraParams, "STREAMLINE_FRAME_TOKEN");
        if (!frameTokenParam
            || frameTokenParam->valueType != SR_PARAM_VALUE_TYPE_POINTER
            || !frameTokenParam->value.ptrValue) {
            sendContextMessage(privateData, SR_MESSAGE_TYPE_ERROR, L"Streamline DLSS requires a frame token from the Java integration.");
            return SR_RETURN_CODE_INVALID_ARGUMENT;
        }
        auto *frameToken = reinterpret_cast<sl::FrameToken *>(frameTokenParam->value.ptrValue);

        sl::ViewportHandle viewport(kViewportId);
        sl::Extent renderExtent = makeExtent(desc->renderSize.x, desc->renderSize.y);
        sl::Extent outputExtent = makeExtent(desc->upscaleSize.x, desc->upscaleSize.y);
        sl::Resource color = makeResource(desc->color, VK_IMAGE_LAYOUT_GENERAL);
        sl::Resource output = makeResource(desc->output, VK_IMAGE_LAYOUT_GENERAL);
        sl::Resource depth = makeResource(desc->depth, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        sl::Resource mvec = makeResource(desc->motionVectors, VK_IMAGE_LAYOUT_GENERAL);
        sl::Resource exposure = makeResource(desc->exposure, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        constexpr sl::ResourceLifecycle tagLifecycle = sl::ResourceLifecycle::eValidUntilEvaluate;

        std::vector<sl::ResourceTag> tags;
        tags.emplace_back(&color, sl::kBufferTypeScalingInputColor, tagLifecycle, &renderExtent);
        tags.emplace_back(&output, sl::kBufferTypeScalingOutputColor, tagLifecycle, &outputExtent);
        tags.emplace_back(&depth, sl::kBufferTypeDepth, tagLifecycle, &renderExtent);
        tags.emplace_back(&mvec, sl::kBufferTypeMotionVectors, tagLifecycle, &renderExtent);
        if (desc->exposure.exist) {
            sl::Extent exposureExtent = makeExtent(desc->exposure.desc.width, desc->exposure.desc.height);
            tags.emplace_back(&exposure, sl::kBufferTypeExposure, tagLifecycle, &exposureExtent);
        }

        auto *commandBuffer = reinterpret_cast<sl::CommandBuffer *>(desc->commandList.apiCommandBuffer.vulkan.commandBuffer);
        sl::Result result = callSetTagForFrame(*frameToken, viewport, tags.data(), static_cast<uint32_t>(tags.size()), commandBuffer);
        if (result != sl::Result::eOk) {
            reportResult(privateData, L"slSetTagForFrame", result);
            return resultToReturnCode(result);
        }

        sl::DLSSOptions options{};
        options.mode = inferDLSSMode(&context->desc);
        options.outputWidth = desc->upscaleSize.x;
        options.outputHeight = desc->upscaleSize.y;
        options.preExposure = desc->preExposure;
        options.exposureScale = 1.0f;
        options.colorBuffersHDR = hasFlag(context->desc.flags, SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_HDR) ? sl::Boolean::eTrue : sl::Boolean::eFalse;
        options.useAutoExposure = hasFlag(context->desc.flags, SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_AUTO_EXPOSURE) ? sl::Boolean::eTrue : sl::Boolean::eFalse;
        options.alphaUpscalingEnabled = sl::Boolean::eFalse;
        const SRContextExtraParam *presetParam = srFindParam(&context->desc.extraParams, "DLSS_RENDER_PRESET");
        sl::DLSSPreset preset = sl::DLSSPreset::eDefault;
        if (presetParam && presetParam->valueType == SR_PARAM_VALUE_TYPE_INT32) {
            preset = mapDLSSPreset(presetParam->value.int32Value);
        }
        options.dlaaPreset = preset;
        options.qualityPreset = preset;
        options.balancedPreset = preset;
        options.performancePreset = preset;
        options.ultraPerformancePreset = preset;
        options.ultraQualityPreset = preset;

        result = callDLSSSetOptions(viewport, options);
        if (result != sl::Result::eOk) {
            reportResult(privateData, L"slDLSSSetOptions", result);
            return resultToReturnCode(result);
        }

        const sl::BaseStructure *inputs[] = {&viewport};
        result = callEvaluateFeature(sl::kFeatureDLSS, *frameToken, inputs, 1, commandBuffer);
        if (result != sl::Result::eOk) {
            reportResult(privateData, L"slEvaluateFeature(DLSS)", result);
            return resultToReturnCode(result);
        }
        return SR_RETURN_CODE_OK;
    }

    SR_API SRUpscaleContextCallbacks srGetStreamlineDLSSUpscaleCallbacks() {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = (SRCreateFunc) srStreamlineDLSSCreateUpscaleContext,
            .pInit = (SRInitFunc) srStreamlineDLSSInitUpscaleContext,
            .pDestroy = (SRDestroyFunc) srStreamlineDLSSDestroyUpscaleContext,
            .pQuery = (SRQueryFunc) srStreamlineDLSSQueryUpscale,
            .pDispatchUpscale = (SRDispatchUpscaleFunc) srStreamlineDLSSDispatchUpscale,
            .pShutdown = (SRShutdownFunc) srStreamlineShutdown,
        };
        return callbacks;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nInit(
        JNIEnv *env,
        jclass,
        jstring pluginPath,
        jstring logPath
    ) {
        std::string pluginPathUtf8 = javaStringToUtf8(env, pluginPath);
        std::string logPathUtf8 = javaStringToUtf8(env, logPath);
        return static_cast<jint>(initStreamline(pluginPathUtf8.c_str(), logPathUtf8.c_str(), nullptr));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nShutdown(JNIEnv *, jclass) {
        return static_cast<jint>(srStreamlineShutdown());
    }

    JNIEXPORT jlong JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nCreateVkInstance(
        JNIEnv *,
        jclass,
        jlong createInfoAddress
    ) {
        g_lastVkResult = VK_ERROR_INITIALIZATION_FAILED;
        if (!g_streamlineInitialized || createInfoAddress == 0) {
            return 0;
        }
        PFN_vkGetInstanceProcAddrSl getInstanceProcAddr = getStreamlineVkGetInstanceProcAddr();
        if (!getInstanceProcAddr) {
            return 0;
        }
        auto createInstance = reinterpret_cast<PFN_vkCreateInstance>(
            getInstanceProcAddr(nullptr, "vkCreateInstance")
        );
        if (!createInstance) {
            g_lastVkResult = VK_ERROR_INITIALIZATION_FAILED;
            return 0;
        }
        VkInstance instance = VK_NULL_HANDLE;
        g_lastVkResult = createInstance(
            reinterpret_cast<const VkInstanceCreateInfo *>(createInfoAddress),
            nullptr,
            &instance
        );
        return g_lastVkResult == VK_SUCCESS ? reinterpret_cast<jlong>(instance) : 0;
    }

    JNIEXPORT jlong JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nCreateVkDevice(
        JNIEnv *,
        jclass,
        jlong instanceAddress,
        jlong physicalDeviceAddress,
        jlong createInfoAddress
    ) {
        g_lastVkResult = VK_ERROR_INITIALIZATION_FAILED;
        if (!g_streamlineInitialized || instanceAddress == 0 || physicalDeviceAddress == 0 || createInfoAddress == 0) {
            return 0;
        }
        PFN_vkGetInstanceProcAddrSl getInstanceProcAddr = getStreamlineVkGetInstanceProcAddr();
        if (!getInstanceProcAddr) {
            return 0;
        }
        auto createDevice = reinterpret_cast<PFN_vkCreateDevice>(
            getInstanceProcAddr(reinterpret_cast<VkInstance>(instanceAddress), "vkCreateDevice")
        );
        if (!createDevice) {
            g_lastVkResult = VK_ERROR_INITIALIZATION_FAILED;
            return 0;
        }
        VkDevice device = VK_NULL_HANDLE;
        g_lastVkResult = createDevice(
            reinterpret_cast<VkPhysicalDevice>(physicalDeviceAddress),
            reinterpret_cast<const VkDeviceCreateInfo *>(createInfoAddress),
            nullptr,
            &device
        );
        return g_lastVkResult == VK_SUCCESS ? reinterpret_cast<jlong>(device) : 0;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nSetVulkanInfoLegacy(
        JNIEnv *,
        jclass,
        jlong instanceAddress,
        jlong physicalDeviceAddress,
        jlong deviceAddress,
        jint graphicsQueueFamilyIndex
    ) {
        if (!g_streamlineInitialized || instanceAddress == 0 || physicalDeviceAddress == 0 || deviceAddress == 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::VulkanInfo info{};
        info.instance = reinterpret_cast<VkInstance>(instanceAddress);
        info.physicalDevice = reinterpret_cast<VkPhysicalDevice>(physicalDeviceAddress);
        info.device = reinterpret_cast<VkDevice>(deviceAddress);
        info.graphicsQueueFamily = static_cast<uint32_t>(graphicsQueueFamilyIndex);
        info.graphicsQueueIndex = 0;
        info.computeQueueFamily = static_cast<uint32_t>(graphicsQueueFamilyIndex);
        info.computeQueueIndex = 0;
        info.opticalFlowQueueFamily = static_cast<uint32_t>(graphicsQueueFamilyIndex);
        info.opticalFlowQueueIndex = 0;
        sl::Result result = callSetVulkanInfo(info);
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nGetLastVkResult(JNIEnv *, jclass) {
        return static_cast<jint>(g_lastVkResult);
    }

    JNIEXPORT jboolean JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nIsFeatureSupportedLegacy(
        JNIEnv *,
        jclass,
        jint feature,
        jlong vkPhysicalDeviceAddress
    ) {
        if (!g_streamlineInitialized || vkPhysicalDeviceAddress == 0) {
            return JNI_FALSE;
        }
        sl::AdapterInfo adapterInfo{};
        adapterInfo.vkPhysicalDevice = reinterpret_cast<void *>(vkPhysicalDeviceAddress);
        sl::Result result = callIsFeatureSupported(static_cast<sl::Feature>(feature), adapterInfo);
        return result == sl::Result::eOk ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nIsDLSSGSupported(
        JNIEnv *,
        jclass
    ) {
        bool supported = false;
        SRReturnCode code = srStreamlineIsDLSSGSupported(&supported);
        return code == SR_RETURN_CODE_OK && supported ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nDLSSGSetOptions(
        JNIEnv *,
        jclass,
        jboolean enabled,
        jint framesToGenerate
    ) {
        uint32_t targetFrames = static_cast<uint32_t>((std::max)(1, static_cast<int>(framesToGenerate)));
        return static_cast<jint>(srStreamlineDLSSGSetOptions(
            enabled == JNI_TRUE,
            targetFrames
        ));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nDLSSGGetState(
        JNIEnv *env,
        jclass,
        jobject outState
    ) {
        if (!outState) {
            return static_cast<jint>(SR_RETURN_CODE_NULL_POINTER);
        }
        uint64_t estimatedVram = 0;
        uint32_t status = 0;
        SRReturnCode code = srStreamlineDLSSGGetState(&estimatedVram, &status);
        if (code != SR_RETURN_CODE_OK) {
            return static_cast<jint>(code);
        }
        jclass stateClass = env->GetObjectClass(outState);
        if (!stateClass) {
            return static_cast<jint>(SR_RETURN_CODE_NULL_POINTER);
        }
        jfieldID vramField = env->GetFieldID(stateClass, "estimatedVramUsage", "J");
        jfieldID statusField = env->GetFieldID(stateClass, "status", "I");
        if (!vramField || !statusField) {
            return static_cast<jint>(SR_RETURN_CODE_INVALID_ARGUMENT);
        }
        env->SetLongField(outState, vramField, static_cast<jlong>(estimatedVram));
        env->SetIntField(outState, statusField, static_cast<jint>(status));
        return static_cast<jint>(SR_RETURN_CODE_OK);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nSetVulkanInfo(
        JNIEnv *,
        jclass,
        jlong session,
        jlong device,
        jlong instance,
        jlong physicalDevice,
        jint computeQueueIndex,
        jint computeQueueFamily,
        jint graphicsQueueIndex,
        jint graphicsQueueFamily,
        jint opticalFlowQueueIndex,
        jint opticalFlowQueueFamily,
        jboolean useNativeOpticalFlowMode,
        jint computeQueueCreateFlags,
        jint graphicsQueueCreateFlags,
        jint opticalFlowQueueCreateFlags
    ) {
        if (!isActiveJavaSession(session)) {
            return static_cast<jint>(sl::Result::eErrorNotInitialized);
        }
        sl::VulkanInfo info{};
        info.device = reinterpret_cast<VkDevice>(device);
        info.instance = reinterpret_cast<VkInstance>(instance);
        info.physicalDevice = reinterpret_cast<VkPhysicalDevice>(physicalDevice);
        info.computeQueueIndex = static_cast<uint32_t>(computeQueueIndex);
        info.computeQueueFamily = static_cast<uint32_t>(computeQueueFamily);
        info.graphicsQueueIndex = static_cast<uint32_t>(graphicsQueueIndex);
        info.graphicsQueueFamily = static_cast<uint32_t>(graphicsQueueFamily);
        info.opticalFlowQueueIndex = static_cast<uint32_t>(opticalFlowQueueIndex);
        info.opticalFlowQueueFamily = static_cast<uint32_t>(opticalFlowQueueFamily);
        info.useNativeOpticalFlowMode = useNativeOpticalFlowMode == JNI_TRUE;
        info.computeQueueCreateFlags = static_cast<uint32_t>(computeQueueCreateFlags);
        info.graphicsQueueCreateFlags = static_cast<uint32_t>(graphicsQueueCreateFlags);
        info.opticalFlowQueueCreateFlags = static_cast<uint32_t>(opticalFlowQueueCreateFlags);
        return static_cast<jint>(callSetVulkanInfo(info));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nIsFeatureSupported(
        JNIEnv *env,
        jclass,
        jlong session,
        jint feature,
        jlong physicalDevice,
        jbooleanArray outSupported
    ) {
        if (!isActiveJavaSession(session) || !outSupported || env->GetArrayLength(outSupported) == 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::AdapterInfo adapter{};
        adapter.vkPhysicalDevice = reinterpret_cast<void *>(physicalDevice);
        sl::Result result = callIsFeatureSupported(static_cast<sl::Feature>(feature), adapter);
        setBooleanOut(env, outSupported, result == sl::Result::eOk);
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nIsFeatureLoaded(
        JNIEnv *env,
        jclass,
        jlong session,
        jint feature,
        jbooleanArray outLoaded
    ) {
        if (!isActiveJavaSession(session) || !outLoaded || env->GetArrayLength(outLoaded) == 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        bool loaded = false;
        sl::Result result = callIsFeatureLoaded(static_cast<sl::Feature>(feature), loaded);
        setBooleanOut(env, outLoaded, loaded);
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nSetFeatureLoaded(
        JNIEnv *,
        jclass,
        jlong session,
        jint feature,
        jboolean loaded
    ) {
        if (!isActiveJavaSession(session)) {
            return static_cast<jint>(sl::Result::eErrorNotInitialized);
        }
        return static_cast<jint>(callSetFeatureLoaded(static_cast<sl::Feature>(feature), loaded == JNI_TRUE));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nGetFeatureRequirements(
        JNIEnv *env,
        jclass,
        jlong session,
        jint feature,
        jobject outRequirements
    ) {
        if (!isActiveJavaSession(session) || !outRequirements) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::FeatureRequirements requirements{};
        sl::Result result = callGetFeatureRequirements(static_cast<sl::Feature>(feature), requirements);
        if (result != sl::Result::eOk) {
            return static_cast<jint>(result);
        }
        setInt(env, outRequirements, "flags", static_cast<jint>(requirements.flags));
        setInt(env, outRequirements, "maxNumCpuThreads", static_cast<jint>(requirements.maxNumCPUThreads));
        setInt(env, outRequirements, "maxNumViewports", static_cast<jint>(requirements.maxNumViewports));
        writeIntArrayField(env, outRequirements, "requiredTags", requirements.requiredTags, requirements.numRequiredTags);
        constexpr const char *kVersionSignature =
            "Lio/homo/superresolution/core/streamline/StreamlineTypes$Version;";
        jobject osDetected = getObject(env, outRequirements, "osVersionDetected", kVersionSignature);
        jobject osRequired = getObject(env, outRequirements, "osVersionRequired", kVersionSignature);
        jobject driverDetected = getObject(env, outRequirements, "driverVersionDetected", kVersionSignature);
        jobject driverRequired = getObject(env, outRequirements, "driverVersionRequired", kVersionSignature);
        writeVersion(env, osDetected, requirements.osVersionDetected);
        writeVersion(env, osRequired, requirements.osVersionRequired);
        writeVersion(env, driverDetected, requirements.driverVersionDetected);
        writeVersion(env, driverRequired, requirements.driverVersionRequired);
        if (osDetected) env->DeleteLocalRef(osDetected);
        if (osRequired) env->DeleteLocalRef(osRequired);
        if (driverDetected) env->DeleteLocalRef(driverDetected);
        if (driverRequired) env->DeleteLocalRef(driverRequired);
        setInt(env, outRequirements, "vkNumComputeQueuesRequired", static_cast<jint>(requirements.vkNumComputeQueuesRequired));
        setInt(env, outRequirements, "vkNumGraphicsQueuesRequired", static_cast<jint>(requirements.vkNumGraphicsQueuesRequired));
        writeStringArrayField(env, outRequirements, "vkDeviceExtensions", requirements.vkDeviceExtensions, requirements.vkNumDeviceExtensions);
        writeStringArrayField(env, outRequirements, "vkInstanceExtensions", requirements.vkInstanceExtensions, requirements.vkNumInstanceExtensions);
        writeStringArrayField(env, outRequirements, "vkFeatures12", requirements.vkFeatures12, requirements.vkNumFeatures12);
        writeStringArrayField(env, outRequirements, "vkFeatures13", requirements.vkFeatures13, requirements.vkNumFeatures13);
        setInt(env, outRequirements, "vkNumOpticalFlowQueuesRequired", static_cast<jint>(requirements.vkNumOpticalFlowQueuesRequired));
        return static_cast<jint>(sl::Result::eOk);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nGetFeatureVersion(
        JNIEnv *env,
        jclass,
        jlong session,
        jint feature,
        jobject outVersion
    ) {
        if (!isActiveJavaSession(session) || !outVersion) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::FeatureVersion version{};
        sl::Result result = callGetFeatureVersion(static_cast<sl::Feature>(feature), version);
        if (result != sl::Result::eOk) {
            return static_cast<jint>(result);
        }
        constexpr const char *kVersionSignature =
            "Lio/homo/superresolution/core/streamline/StreamlineTypes$Version;";
        jobject versionSl = getObject(env, outVersion, "versionSl", kVersionSignature);
        jobject versionNgx = getObject(env, outVersion, "versionNgx", kVersionSignature);
        writeVersion(env, versionSl, version.versionSL);
        writeVersion(env, versionNgx, version.versionNGX);
        if (versionSl) env->DeleteLocalRef(versionSl);
        if (versionNgx) env->DeleteLocalRef(versionNgx);
        return static_cast<jint>(sl::Result::eOk);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nGetNewFrameToken(
        JNIEnv *env,
        jclass,
        jlong session,
        jboolean hasFrameIndex,
        jint frameIndex,
        jobject outToken
    ) {
        if (!isActiveJavaSession(session) || !outToken) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        uint32_t nativeFrameIndex = static_cast<uint32_t>(frameIndex);
        sl::FrameToken *token = nullptr;
        sl::Result result = callGetNewFrameToken(token, hasFrameIndex == JNI_TRUE ? &nativeFrameIndex : nullptr);
        if (result != sl::Result::eOk || !token) {
            return static_cast<jint>(result);
        }
        setLong(env, outToken, "nativeHandle", reinterpret_cast<jlong>(token));
        setInt(env, outToken, "frameIndex", static_cast<jint>(static_cast<uint32_t>(*token)));
        return static_cast<jint>(sl::Result::eOk);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nSetTag(
        JNIEnv *env,
        jclass,
        jlong session,
        jint viewport,
        jobjectArray tagArray,
        jlong commandBuffer
    ) {
        if (!isActiveJavaSession(session)) {
            return static_cast<jint>(sl::Result::eErrorNotInitialized);
        }
        std::vector<OwnedResourceTag> ownedTags;
        if (!readResourceTags(env, tagArray, ownedTags)) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        std::vector<sl::ResourceTag> tags;
        tags.reserve(ownedTags.size());
        for (const OwnedResourceTag &owned : ownedTags) {
            tags.push_back(*owned.tag);
        }
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        return static_cast<jint>(callSetTag(
            handle,
            tags.data(),
            static_cast<uint32_t>(tags.size()),
            reinterpret_cast<sl::CommandBuffer *>(commandBuffer)
        ));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nSetTagForFrame(
        JNIEnv *env,
        jclass,
        jlong session,
        jlong frameToken,
        jint viewport,
        jobjectArray tagArray,
        jlong commandBuffer
    ) {
        if (!isActiveJavaSession(session) || frameToken == 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        std::vector<OwnedResourceTag> ownedTags;
        if (!readResourceTags(env, tagArray, ownedTags)) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        std::vector<sl::ResourceTag> tags;
        tags.reserve(ownedTags.size());
        for (const OwnedResourceTag &owned : ownedTags) {
            tags.push_back(*owned.tag);
        }
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        return static_cast<jint>(callSetTagForFrame(
            *reinterpret_cast<sl::FrameToken *>(frameToken),
            handle,
            tags.data(),
            static_cast<uint32_t>(tags.size()),
            reinterpret_cast<sl::CommandBuffer *>(commandBuffer)
        ));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nSetConstants(
        JNIEnv *env,
        jclass,
        jlong session,
        jobject javaConstants,
        jlong frameToken,
        jint viewport
    ) {
        if (!isActiveJavaSession(session) || frameToken == 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::Constants constants{};
        if (!readConstants(env, javaConstants, constants)) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        return static_cast<jint>(callSetConstants(
            constants,
            *reinterpret_cast<sl::FrameToken *>(frameToken),
            handle
        ));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nAllocateResources(
        JNIEnv *,
        jclass,
        jlong session,
        jlong commandBuffer,
        jint feature,
        jint viewport
    ) {
        if (!isActiveJavaSession(session)) {
            return static_cast<jint>(sl::Result::eErrorNotInitialized);
        }
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        return static_cast<jint>(callAllocateResources(
            reinterpret_cast<sl::CommandBuffer *>(commandBuffer),
            static_cast<sl::Feature>(feature),
            handle
        ));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nFreeResources(
        JNIEnv *,
        jclass,
        jlong session,
        jint feature,
        jint viewport
    ) {
        if (!isActiveJavaSession(session)) {
            return static_cast<jint>(sl::Result::eErrorNotInitialized);
        }
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        return static_cast<jint>(callFreeResources(static_cast<sl::Feature>(feature), handle));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nEvaluateFeature(
        JNIEnv *env,
        jclass,
        jlong session,
        jint feature,
        jlong frameToken,
        jobjectArray javaInputs,
        jlong commandBuffer
    ) {
        if (!isActiveJavaSession(session) || frameToken == 0 || !javaInputs) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        jsize count = env->GetArrayLength(javaInputs);
        if (count <= 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        constexpr const char *kViewportSignature =
            "Lio/homo/superresolution/core/streamline/StreamlineTypes$Viewport;";
        constexpr const char *kResourceTagSignature =
            "Lio/homo/superresolution/core/streamline/StreamlineTypes$ResourceTag;";
        constexpr const char *kConstantsSignature =
            "Lio/homo/superresolution/core/streamline/StreamlineTypes$Constants;";
        std::vector<std::unique_ptr<sl::ViewportHandle>> viewports;
        std::vector<std::unique_ptr<sl::Constants>> constants;
        std::vector<OwnedResourceTag> tags;
        std::vector<const sl::BaseStructure *> inputs;
        viewports.reserve(static_cast<size_t>(count));
        constants.reserve(static_cast<size_t>(count));
        tags.reserve(static_cast<size_t>(count));
        inputs.reserve(static_cast<size_t>(count));

        for (jsize index = 0; index < count; ++index) {
            jobject input = env->GetObjectArrayElement(javaInputs, index);
            if (!input) {
                return static_cast<jint>(sl::Result::eErrorInvalidParameter);
            }
            jint kind = getInt(env, input, "kind");
            switch (kind) {
                case 0: {
                    jobject viewport = getObject(env, input, "viewport", kViewportSignature);
                    if (!viewport) {
                        env->DeleteLocalRef(input);
                        return static_cast<jint>(sl::Result::eErrorInvalidParameter);
                    }
                    viewports.push_back(std::make_unique<sl::ViewportHandle>(
                        static_cast<uint32_t>(getInt(env, viewport, "value"))
                    ));
                    inputs.push_back(viewports.back().get());
                    env->DeleteLocalRef(viewport);
                    break;
                }
                case 1: {
                    jobject tag = getObject(env, input, "resourceTag", kResourceTagSignature);
                    tags.emplace_back();
                    bool valid = readResourceTag(env, tag, tags.back());
                    if (tag) {
                        env->DeleteLocalRef(tag);
                    }
                    if (!valid) {
                        env->DeleteLocalRef(input);
                        return static_cast<jint>(sl::Result::eErrorInvalidParameter);
                    }
                    inputs.push_back(tags.back().tag.get());
                    break;
                }
                case 2: {
                    jobject javaConstants = getObject(env, input, "constants", kConstantsSignature);
                    auto nativeConstants = std::make_unique<sl::Constants>();
                    bool valid = readConstants(env, javaConstants, *nativeConstants);
                    if (javaConstants) {
                        env->DeleteLocalRef(javaConstants);
                    }
                    if (!valid) {
                        env->DeleteLocalRef(input);
                        return static_cast<jint>(sl::Result::eErrorInvalidParameter);
                    }
                    inputs.push_back(nativeConstants.get());
                    constants.push_back(std::move(nativeConstants));
                    break;
                }
                default:
                    env->DeleteLocalRef(input);
                    return static_cast<jint>(sl::Result::eErrorInvalidParameter);
            }
            env->DeleteLocalRef(input);
        }
        return static_cast<jint>(callEvaluateFeature(
            static_cast<sl::Feature>(feature),
            *reinterpret_cast<sl::FrameToken *>(frameToken),
            inputs.data(),
            static_cast<uint32_t>(inputs.size()),
            reinterpret_cast<sl::CommandBuffer *>(commandBuffer)
        ));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nGetFeatureFunction(
        JNIEnv *env,
        jclass,
        jlong session,
        jint feature,
        jstring name,
        jlongArray outAddress
    ) {
        if (!isActiveJavaSession(session) || !name || !outAddress || env->GetArrayLength(outAddress) == 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        std::string functionName = javaStringToUtf8(env, name);
        if (functionName.empty()) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        void *address = nullptr;
        sl::Result result = callGetFeatureFunction(static_cast<sl::Feature>(feature), functionName.c_str(), address);
        if (result == sl::Result::eOk) {
            setLongOut(env, outAddress, reinterpret_cast<jlong>(address));
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nDlssGetOptimalSettings(
        JNIEnv *env,
        jclass,
        jlong session,
        jobject javaOptions,
        jobject outSettings
    ) {
        if (!isActiveJavaSession(session) || !javaOptions || !outSettings) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::DLSSOptions options{};
        sl::DLSSOptimalSettings settings{};
        readDLSSOptions(env, javaOptions, options);
        sl::Result result = callDLSSGetOptimalSettings(options, settings);
        if (result != sl::Result::eOk) {
            return static_cast<jint>(result);
        }
        setInt(env, outSettings, "optimalRenderWidth", static_cast<jint>(settings.optimalRenderWidth));
        setInt(env, outSettings, "optimalRenderHeight", static_cast<jint>(settings.optimalRenderHeight));
        setFloat(env, outSettings, "optimalSharpness", settings.optimalSharpness);
        setInt(env, outSettings, "renderWidthMin", static_cast<jint>(settings.renderWidthMin));
        setInt(env, outSettings, "renderHeightMin", static_cast<jint>(settings.renderHeightMin));
        setInt(env, outSettings, "renderWidthMax", static_cast<jint>(settings.renderWidthMax));
        setInt(env, outSettings, "renderHeightMax", static_cast<jint>(settings.renderHeightMax));
        return static_cast<jint>(sl::Result::eOk);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nDlssGetState(
        JNIEnv *env,
        jclass,
        jlong session,
        jint viewport,
        jobject outState
    ) {
        if (!isActiveJavaSession(session) || !outState) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        sl::DLSSState state{};
        sl::Result result = callDLSSGetState(handle, state);
        if (result == sl::Result::eOk) {
            setLong(env, outState, "estimatedVramUsage", static_cast<jlong>(state.estimatedVRAMUsageInBytes));
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nDlssSetOptions(
        JNIEnv *env,
        jclass,
        jlong session,
        jint viewport,
        jobject javaOptions
    ) {
        if (!isActiveJavaSession(session) || !javaOptions) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        sl::DLSSOptions options{};
        readDLSSOptions(env, javaOptions, options);
        return static_cast<jint>(callDLSSSetOptions(handle, options));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nDlssGGetState(
        JNIEnv *env,
        jclass,
        jlong session,
        jint viewport,
        jobject outState,
        jobject javaOptions
    ) {
        if (!isActiveJavaSession(session) || !outState) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::DLSSGOptions options{};
        const sl::DLSSGOptions *optionsPointer = nullptr;
        if (javaOptions) {
            readDLSSGOptions(env, javaOptions, options);
            optionsPointer = &options;
        }
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        sl::DLSSGState state{};
        sl::Result result = callDLSSGGetState(handle, state, optionsPointer);
        if (result != sl::Result::eOk) {
            return static_cast<jint>(result);
        }
        setLong(env, outState, "estimatedVramUsage", static_cast<jlong>(state.estimatedVRAMUsageInBytes));
        setInt(env, outState, "status", static_cast<jint>(state.status));
        setInt(env, outState, "minWidthOrHeight", static_cast<jint>(state.minWidthOrHeight));
        setInt(env, outState, "numFramesActuallyPresented", static_cast<jint>(state.numFramesActuallyPresented));
        setInt(env, outState, "numFramesToGenerateMax", static_cast<jint>(state.numFramesToGenerateMax));
        setByte(env, outState, "reserved4", static_cast<jbyte>(state.bReserved4));
        setByte(env, outState, "vsyncSupportAvailable", static_cast<jbyte>(state.bIsVsyncSupportAvailable));
        setLong(env, outState, "inputsProcessingCompletionFence", reinterpret_cast<jlong>(state.inputsProcessingCompletionFence));
        setLong(env, outState, "lastPresentInputsProcessingCompletionFenceValue",
                static_cast<jlong>(state.lastPresentInputsProcessingCompletionFenceValue));
        setByte(env, outState, "dynamicMfgSupported", static_cast<jbyte>(state.bIsDynamicMFGSupported));
        return static_cast<jint>(sl::Result::eOk);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nDlssGSetOptions(
        JNIEnv *env,
        jclass,
        jlong session,
        jint viewport,
        jobject javaOptions
    ) {
        if (!isActiveJavaSession(session) || !javaOptions || !g_javaSession) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        constexpr const char *kListenerSignature =
            "Lio/homo/superresolution/core/streamline/StreamlineApiErrorListener;";
        jobject listener = getObject(env, javaOptions, "onApiError", kListenerSignature);
        if (g_javaSession->apiErrorListener) {
            env->DeleteGlobalRef(g_javaSession->apiErrorListener);
            g_javaSession->apiErrorListener = nullptr;
            g_javaSession->apiErrorMethod = nullptr;
        }
        if (listener) {
            g_javaSession->apiErrorListener = env->NewGlobalRef(listener);
            jclass listenerClass = env->GetObjectClass(listener);
            g_javaSession->apiErrorMethod = listenerClass
                ? env->GetMethodID(listenerClass, "onApiError", "(I)V")
                : nullptr;
            if (listenerClass) {
                env->DeleteLocalRef(listenerClass);
            }
            env->DeleteLocalRef(listener);
            if (!g_javaSession->apiErrorListener || !g_javaSession->apiErrorMethod) {
                return static_cast<jint>(sl::Result::eErrorInvalidParameter);
            }
        }
        sl::DLSSGOptions options{};
        readDLSSGOptions(env, javaOptions, options);
        options.onErrorCallback = g_javaSession->apiErrorListener ? sendJavaApiError : nullptr;
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        return static_cast<jint>(callDLSSGSetOptions(handle, options));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nPclGetState(
        JNIEnv *env,
        jclass,
        jlong session,
        jobject outState
    ) {
        if (!isActiveJavaSession(session) || !outState) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::PCLState state{};
        sl::Result result = callPCLGetState(state);
        if (result == sl::Result::eOk) {
            setInt(env, outState, "statsWindowMessage", static_cast<jint>(state.statsWindowMessage));
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nPclSetMarker(
        JNIEnv *,
        jclass,
        jlong session,
        jint marker,
        jlong frameToken
    ) {
        if (!isActiveJavaSession(session) || frameToken == 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        return static_cast<jint>(callPCLSetMarker(
            static_cast<sl::PCLMarker>(marker),
            *reinterpret_cast<sl::FrameToken *>(frameToken)
        ));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nPclSetOptions(
        JNIEnv *env,
        jclass,
        jlong session,
        jobject javaOptions
    ) {
        if (!isActiveJavaSession(session) || !javaOptions) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::PCLOptions options{};
        options.virtualKey = static_cast<sl::PCLHotKey>(getInt(env, javaOptions, "virtualKey"));
        options.idThread = static_cast<uint32_t>(getInt(env, javaOptions, "threadId"));
        return static_cast<jint>(callPCLSetOptions(options));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nReflexGetState(
        JNIEnv *env,
        jclass,
        jlong session,
        jobject outState
    ) {
        if (!isActiveJavaSession(session) || !outState) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::ReflexState state{};
        sl::Result result = callReflexGetState(state);
        if (result != sl::Result::eOk) {
            return static_cast<jint>(result);
        }
        setBoolean(env, outState, "lowLatencyAvailable", state.lowLatencyAvailable ? JNI_TRUE : JNI_FALSE);
        setBoolean(env, outState, "latencyReportAvailable", state.latencyReportAvailable ? JNI_TRUE : JNI_FALSE);
        setInt(env, outState, "statsWindowMessage", static_cast<jint>(state.statsWindowMessage));
        setBoolean(env, outState, "flashIndicatorDriverControlled",
                   state.flashIndicatorDriverControlled ? JNI_TRUE : JNI_FALSE);
        std::vector<jlong> reports(static_cast<size_t>(sl::kReflexFrameReportCount) * 16);
        std::vector<jlong> reports2(static_cast<size_t>(sl::kReflexFrameReportCount) * 2);
        for (int index = 0; index < sl::kReflexFrameReportCount; ++index) {
            const sl::ReflexReport &report = state.frameReport[index];
            size_t base = static_cast<size_t>(index) * 16;
            reports[base] = static_cast<jlong>(report.frameID);
            reports[base + 1] = static_cast<jlong>(report.inputSampleTime);
            reports[base + 2] = static_cast<jlong>(report.simStartTime);
            reports[base + 3] = static_cast<jlong>(report.simEndTime);
            reports[base + 4] = static_cast<jlong>(report.renderSubmitStartTime);
            reports[base + 5] = static_cast<jlong>(report.renderSubmitEndTime);
            reports[base + 6] = static_cast<jlong>(report.presentStartTime);
            reports[base + 7] = static_cast<jlong>(report.presentEndTime);
            reports[base + 8] = static_cast<jlong>(report.driverStartTime);
            reports[base + 9] = static_cast<jlong>(report.driverEndTime);
            reports[base + 10] = static_cast<jlong>(report.osRenderQueueStartTime);
            reports[base + 11] = static_cast<jlong>(report.osRenderQueueEndTime);
            reports[base + 12] = static_cast<jlong>(report.gpuRenderStartTime);
            reports[base + 13] = static_cast<jlong>(report.gpuRenderEndTime);
            reports[base + 14] = static_cast<jlong>(report.gpuActiveRenderTimeUs);
            reports[base + 15] = static_cast<jlong>(report.gpuFrameTimeUs);

            const sl::ReflexReport2 &report2 = state.frameReport2[index];
            size_t base2 = static_cast<size_t>(index) * 2;
            reports2[base2] = static_cast<jlong>(report2.cameraConstructedTime);
            reports2[base2 + 1] = static_cast<jlong>(report2.crossAdapterCopyTimeUs);
        }
        jlongArray javaReports = env->NewLongArray(static_cast<jsize>(reports.size()));
        jlongArray javaReports2 = env->NewLongArray(static_cast<jsize>(reports2.size()));
        if (!javaReports || !javaReports2) {
            if (javaReports) env->DeleteLocalRef(javaReports);
            if (javaReports2) env->DeleteLocalRef(javaReports2);
            return static_cast<jint>(sl::Result::eErrorExceptionHandler);
        }
        env->SetLongArrayRegion(javaReports, 0, static_cast<jsize>(reports.size()), reports.data());
        env->SetLongArrayRegion(javaReports2, 0, static_cast<jsize>(reports2.size()), reports2.data());
        setObject(env, outState, "frameReports", "[J", javaReports);
        setObject(env, outState, "frameReports2", "[J", javaReports2);
        env->DeleteLocalRef(javaReports);
        env->DeleteLocalRef(javaReports2);
        return static_cast<jint>(sl::Result::eOk);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nReflexSleep(
        JNIEnv *,
        jclass,
        jlong session,
        jlong frameToken
    ) {
        if (!isActiveJavaSession(session) || frameToken == 0) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        return static_cast<jint>(callReflexSleep(*reinterpret_cast<sl::FrameToken *>(frameToken)));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nReflexSetOptions(
        JNIEnv *env,
        jclass,
        jlong session,
        jobject javaOptions
    ) {
        if (!isActiveJavaSession(session) || !javaOptions) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::ReflexOptions options{};
        options.mode = static_cast<sl::ReflexMode>(getInt(env, javaOptions, "mode"));
        options.frameLimitUs = static_cast<uint32_t>(getInt(env, javaOptions, "frameLimitUs"));
        options.useMarkersToOptimize = getBoolean(env, javaOptions, "useMarkersToOptimize") == JNI_TRUE;
        options.virtualKey = static_cast<uint16_t>(getInt(env, javaOptions, "virtualKey"));
        options.idThread = static_cast<uint32_t>(getInt(env, javaOptions, "threadId"));
        return static_cast<jint>(callReflexSetOptions(options));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nReflexSetCameraData(
        JNIEnv *env,
        jclass,
        jlong session,
        jint viewport,
        jlong frameToken,
        jobject javaData
    ) {
        if (!isActiveJavaSession(session) || frameToken == 0 || !javaData) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::ReflexCameraData data{};
        readReflexCameraData(env, javaData, data);
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        return static_cast<jint>(callReflexSetCameraData(
            handle,
            *reinterpret_cast<sl::FrameToken *>(frameToken),
            data
        ));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_streamline_StreamlineNative_nReflexGetPredictedCameraData(
        JNIEnv *env,
        jclass,
        jlong session,
        jint viewport,
        jlong frameToken,
        jobject outData
    ) {
        if (!isActiveJavaSession(session) || frameToken == 0 || !outData) {
            return static_cast<jint>(sl::Result::eErrorInvalidParameter);
        }
        sl::ReflexPredictedCameraData data{};
        sl::ViewportHandle handle(static_cast<uint32_t>(viewport));
        sl::Result result = callReflexGetPredictedCameraData(
            handle,
            *reinterpret_cast<sl::FrameToken *>(frameToken),
            data
        );
        if (result == sl::Result::eOk) {
            writeReflexPredictedCameraData(env, outData, data);
        }
        return static_cast<jint>(result);
    }
}
