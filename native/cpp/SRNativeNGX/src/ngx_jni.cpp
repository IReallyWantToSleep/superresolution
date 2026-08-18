#include <jni.h>

#include <codecvt>
#include <locale>
#include <string>
#include <vector>

#include <vulkan/vulkan.h>

#include "io_homo_superresolution_core_ngx_NgxNative.h"
#include "nvsdk_ngx_defs.h"
#include "nvsdk_ngx_params.h"
#include "nvsdk_ngx_vk.h"

namespace {
    constexpr jint kInvalidParameter = NVSDK_NGX_Result_FAIL_InvalidParameter;

    JavaVM *g_javaVm = nullptr;
    jobject g_logCallback = nullptr;
    jmethodID g_logMethod = nullptr;

    struct FeatureCommonInfoStorage {
        std::vector<std::wstring> paths;
        std::vector<const wchar_t *> pathPointers;
        NVSDK_NGX_FeatureCommonInfo info = {};
    };

    struct ProgressCallbackContext {
        JavaVM *javaVm = nullptr;
        jobject callback = nullptr;
        jmethodID method = nullptr;
    };

    thread_local ProgressCallbackContext *g_progressContext = nullptr;

    std::string toUtf8(JNIEnv *env, jstring value) {
        if (!env || !value) {
            return {};
        }
        const char *chars = env->GetStringUTFChars(value, nullptr);
        if (!chars) {
            return {};
        }
        std::string result(chars);
        env->ReleaseStringUTFChars(value, chars);
        return result;
    }

    std::wstring toWide(JNIEnv *env, jstring value) {
        std::string utf8 = toUtf8(env, value);
        if (utf8.empty()) {
            return {};
        }
        std::wstring_convert<std::codecvt_utf8<wchar_t> > converter;
        return converter.from_bytes(utf8);
    }

    jint intField(JNIEnv *env, jobject object, const char *name) {
        jclass type = env->GetObjectClass(object);
        jfieldID field = env->GetFieldID(type, name, "I");
        jint value = field ? env->GetIntField(object, field) : 0;
        env->DeleteLocalRef(type);
        return value;
    }

    jlong longField(JNIEnv *env, jobject object, const char *name) {
        jclass type = env->GetObjectClass(object);
        jfieldID field = env->GetFieldID(type, name, "J");
        jlong value = field ? env->GetLongField(object, field) : 0;
        env->DeleteLocalRef(type);
        return value;
    }

    jboolean booleanField(JNIEnv *env, jobject object, const char *name) {
        jclass type = env->GetObjectClass(object);
        jfieldID field = env->GetFieldID(type, name, "Z");
        jboolean value = field ? env->GetBooleanField(object, field) : JNI_FALSE;
        env->DeleteLocalRef(type);
        return value;
    }

    jobject objectField(JNIEnv *env, jobject object, const char *name, const char *signature) {
        jclass type = env->GetObjectClass(object);
        jfieldID field = env->GetFieldID(type, name, signature);
        jobject value = field ? env->GetObjectField(object, field) : nullptr;
        env->DeleteLocalRef(type);
        return value;
    }

    void setLongField(JNIEnv *env, jobject object, const char *name, jlong value) {
        jclass type = env->GetObjectClass(object);
        jfieldID field = env->GetFieldID(type, name, "J");
        if (field) {
            env->SetLongField(object, field, value);
        }
        env->DeleteLocalRef(type);
    }

    void setIntField(JNIEnv *env, jobject object, const char *name, jint value) {
        jclass type = env->GetObjectClass(object);
        jfieldID field = env->GetFieldID(type, name, "I");
        if (field) {
            env->SetIntField(object, field, value);
        }
        env->DeleteLocalRef(type);
    }

    void setStringField(JNIEnv *env, jobject object, const char *name, const char *value) {
        jclass type = env->GetObjectClass(object);
        jfieldID field = env->GetFieldID(type, name, "Ljava/lang/String;");
        if (field) {
            jstring text = env->NewStringUTF(value ? value : "");
            env->SetObjectField(object, field, text);
            env->DeleteLocalRef(text);
        }
        env->DeleteLocalRef(type);
    }

    bool getJavaEnv(JavaVM *javaVm, JNIEnv *&env, bool &attached) {
        env = nullptr;
        attached = false;
        if (!javaVm) {
            return false;
        }
        jint result = javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8);
        if (result == JNI_OK) {
            return true;
        }
        if (result != JNI_EDETACHED
            || javaVm->AttachCurrentThread(reinterpret_cast<void **>(&env), nullptr) != JNI_OK) {
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

    void NVSDK_CONV ngxLogCallback(
        const char *message,
        NVSDK_NGX_Logging_Level loggingLevel,
        NVSDK_NGX_Feature sourceFeature
    ) {
        if (!g_javaVm || !g_logCallback || !g_logMethod || !message) {
            return;
        }
        JNIEnv *env = nullptr;
        bool attached = false;
        if (!getJavaEnv(g_javaVm, env, attached)) {
            return;
        }
        jstring javaMessage = env->NewStringUTF(message);
        env->CallVoidMethod(g_logCallback, g_logMethod, javaMessage,
                            static_cast<jint>(loggingLevel), static_cast<jint>(sourceFeature));
        env->DeleteLocalRef(javaMessage);
        clearJavaException(env);
        if (attached) {
            g_javaVm->DetachCurrentThread();
        }
    }

    void NVSDK_CONV ngxProgressCallback(float progress, bool *shouldCancel) {
        ProgressCallbackContext *context = g_progressContext;
        if (!context || !context->javaVm || !context->callback || !context->method) {
            return;
        }
        JNIEnv *env = nullptr;
        bool attached = false;
        if (!getJavaEnv(context->javaVm, env, attached)) {
            return;
        }
        jboolean cancel = env->CallBooleanMethod(context->callback, context->method, progress);
        clearJavaException(env);
        if (shouldCancel) {
            *shouldCancel = cancel == JNI_TRUE;
        }
        if (attached) {
            context->javaVm->DetachCurrentThread();
        }
    }

    void replaceLogCallback(JNIEnv *env, jobject featureInfo) {
        if (g_logCallback) {
            env->DeleteGlobalRef(g_logCallback);
            g_logCallback = nullptr;
            g_logMethod = nullptr;
        }
        if (!featureInfo) {
            return;
        }
        jobject callback = objectField(
            env,
            featureInfo,
            "loggingCallback",
            "Lio/homo/superresolution/core/ngx/NgxLogCallback;"
        );
        if (!callback) {
            return;
        }
        jclass callbackType = env->GetObjectClass(callback);
        g_logMethod = env->GetMethodID(callbackType, "onLog", "(Ljava/lang/String;II)V");
        g_logCallback = env->NewGlobalRef(callback);
        env->DeleteLocalRef(callbackType);
        env->DeleteLocalRef(callback);
        env->GetJavaVM(&g_javaVm);
    }

    void fillFeatureCommonInfo(
        JNIEnv *env,
        jobject featureInfo,
        FeatureCommonInfoStorage &storage,
        bool updateLogCallback
    ) {
        storage.info = {};
        if (!featureInfo) {
            return;
        }
        if (updateLogCallback) {
            replaceLogCallback(env, featureInfo);
        }
        auto paths = reinterpret_cast<jobjectArray>(
            objectField(env, featureInfo, "featurePaths", "[Ljava/lang/String;"));
        if (paths) {
            jsize count = env->GetArrayLength(paths);
            storage.paths.reserve(static_cast<size_t>(count));
            storage.pathPointers.reserve(static_cast<size_t>(count));
            for (jsize index = 0; index < count; index++) {
                auto path = static_cast<jstring>(env->GetObjectArrayElement(paths, index));
                storage.paths.push_back(toWide(env, path));
                env->DeleteLocalRef(path);
            }
            for (const std::wstring &path: storage.paths) {
                storage.pathPointers.push_back(path.c_str());
            }
            storage.info.PathListInfo.Path = storage.pathPointers.data();
            storage.info.PathListInfo.Length = static_cast<unsigned int>(storage.pathPointers.size());
            env->DeleteLocalRef(paths);
        }
        storage.info.LoggingInfo.LoggingCallback = g_logCallback ? ngxLogCallback : nullptr;
        storage.info.LoggingInfo.MinimumLoggingLevel =
                static_cast<NVSDK_NGX_Logging_Level>(intField(env, featureInfo, "minimumLoggingLevel"));
        storage.info.LoggingInfo.DisableOtherLoggingSinks =
                booleanField(env, featureInfo, "disableOtherLoggingSinks") == JNI_TRUE;
    }

    bool fillFeatureDiscoveryInfo(
        JNIEnv *env,
        jobject discovery,
        FeatureCommonInfoStorage &storage,
        NVSDK_NGX_FeatureDiscoveryInfo &out
    ) {
        if (!discovery) {
            return false;
        }
        out = {};
        out.SDKVersion = static_cast<NVSDK_NGX_Version>(intField(env, discovery, "sdkVersion"));
        out.FeatureID = static_cast<NVSDK_NGX_Feature>(intField(env, discovery, "feature"));
        jobject identifier = objectField(
            env,
            discovery,
            "identifier",
            "Lio/homo/superresolution/core/ngx/NgxApplicationIdentifier;"
        );
        if (!identifier) {
            return false;
        }
        jint identifierType = intField(env, identifier, "identifierType");
        out.Identifier.IdentifierType = static_cast<NVSDK_NGX_Application_Identifier_Type>(identifierType);
        if (identifierType == NVSDK_NGX_Application_Identifier_Type_Project_Id) {
            auto projectId = static_cast<jstring>(objectField(env, identifier, "projectId", "Ljava/lang/String;"));
            auto engineVersion = static_cast<jstring>(objectField(env, identifier, "engineVersion",
                                                                  "Ljava/lang/String;"));
            static thread_local std::string projectIdStorage;
            static thread_local std::string engineVersionStorage;
            projectIdStorage = toUtf8(env, projectId);
            engineVersionStorage = toUtf8(env, engineVersion);
            out.Identifier.v.ProjectDesc.ProjectId = projectIdStorage.c_str();
            out.Identifier.v.ProjectDesc.EngineType =
                    static_cast<NVSDK_NGX_EngineType>(intField(env, identifier, "engineType"));
            out.Identifier.v.ProjectDesc.EngineVersion = engineVersionStorage.c_str();
            env->DeleteLocalRef(projectId);
            env->DeleteLocalRef(engineVersion);
        } else {
            out.Identifier.v.ApplicationId = static_cast<unsigned long long>(
                longField(env, identifier, "applicationId"));
        }
        env->DeleteLocalRef(identifier);

        auto applicationDataPath = static_cast<jstring>(objectField(
            env, discovery, "applicationDataPath", "Ljava/lang/String;"
        ));
        static thread_local std::wstring applicationDataPathStorage;
        applicationDataPathStorage = toWide(env, applicationDataPath);
        out.ApplicationDataPath = applicationDataPathStorage.empty() ? nullptr : applicationDataPathStorage.c_str();
        env->DeleteLocalRef(applicationDataPath);

        jobject featureInfo = objectField(
            env,
            discovery,
            "featureInfo",
            "Lio/homo/superresolution/core/ngx/NgxFeatureCommonInfo;"
        );
        if (featureInfo) {
            fillFeatureCommonInfo(env, featureInfo, storage, false);
            out.FeatureInfo = &storage.info;
            env->DeleteLocalRef(featureInfo);
        }
        return true;
    }

    NVSDK_NGX_Parameter *asParameters(jlong value) {
        return reinterpret_cast<NVSDK_NGX_Parameter *>(value);
    }
}

extern "C" {
    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nInitWithProjectId(
        JNIEnv *env,
        jclass,
        jstring projectId,
        jint engineType,
        jstring engineVersion,
        jstring applicationDataPath,
        jlong instance,
        jlong physicalDevice,
        jlong device,
        jlong getInstanceProcAddr,
        jlong getDeviceProcAddr,
        jobject featureInfo,
        jint sdkVersion
    ) {
        FeatureCommonInfoStorage featureInfoStorage;
        fillFeatureCommonInfo(env, featureInfo, featureInfoStorage, true);
        std::string projectIdValue = toUtf8(env, projectId);
        std::string engineVersionValue = toUtf8(env, engineVersion);
        std::wstring appPathValue = toWide(env, applicationDataPath);
        return NVSDK_NGX_VULKAN_Init_with_ProjectID(
            projectIdValue.c_str(),
            static_cast<NVSDK_NGX_EngineType>(engineType),
            engineVersionValue.c_str(),
            appPathValue.c_str(),
            reinterpret_cast<VkInstance>(instance),
            reinterpret_cast<VkPhysicalDevice>(physicalDevice),
            reinterpret_cast<VkDevice>(device),
            reinterpret_cast<PFN_vkGetInstanceProcAddr>(getInstanceProcAddr),
            reinterpret_cast<PFN_vkGetDeviceProcAddr>(getDeviceProcAddr),
            featureInfo ? &featureInfoStorage.info : nullptr,
            static_cast<NVSDK_NGX_Version>(sdkVersion)
        );
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nShutdown(JNIEnv *env, jclass) {
        jint result = NVSDK_NGX_VULKAN_Shutdown1(VK_NULL_HANDLE);
        if (g_logCallback) {
            env->DeleteGlobalRef(g_logCallback);
            g_logCallback = nullptr;
            g_logMethod = nullptr;
        }
        return result;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nGetFeatureRequirements(
        JNIEnv *env,
        jclass,
        jlong instance,
        jlong physicalDevice,
        jobject discoveryInfo,
        jobject outRequirements
    ) {
        if (!outRequirements) {
            return kInvalidParameter;
        }
        FeatureCommonInfoStorage featureInfoStorage;
        NVSDK_NGX_FeatureDiscoveryInfo discovery = {};
        if (!fillFeatureDiscoveryInfo(env, discoveryInfo, featureInfoStorage, discovery)) {
            return kInvalidParameter;
        }
        NVSDK_NGX_FeatureRequirement requirements = {};
        NVSDK_NGX_Result result = NVSDK_NGX_VULKAN_GetFeatureRequirements(
            reinterpret_cast<VkInstance>(instance),
            reinterpret_cast<VkPhysicalDevice>(physicalDevice),
            &discovery,
            &requirements
        );
        if (NVSDK_NGX_SUCCEED(result)) {
            setIntField(env, outRequirements, "featureSupported", requirements.FeatureSupported);
            setIntField(env, outRequirements, "minHardwareArchitecture",
                        static_cast<jint>(requirements.MinHWArchitecture));
            setStringField(env, outRequirements, "minOsVersion", requirements.MinOSVersion);
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nAllocateParameters(
        JNIEnv *env, jclass, jobject outParameters
    ) {
        if (!outParameters) {
            return kInvalidParameter;
        }
        NVSDK_NGX_Parameter *parameters = nullptr;
        NVSDK_NGX_Result result = NVSDK_NGX_VULKAN_AllocateParameters(&parameters);
        if (NVSDK_NGX_SUCCEED(result)) {
            setLongField(env, outParameters, "nativePointer", reinterpret_cast<jlong>(parameters));
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nGetCapabilityParameters(
        JNIEnv *env, jclass, jobject outParameters
    ) {
        if (!outParameters) {
            return kInvalidParameter;
        }
        NVSDK_NGX_Parameter *parameters = nullptr;
        NVSDK_NGX_Result result = NVSDK_NGX_VULKAN_GetCapabilityParameters(&parameters);
        if (NVSDK_NGX_SUCCEED(result)) {
            setLongField(env, outParameters, "nativePointer", reinterpret_cast<jlong>(parameters));
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nDestroyParameters(
        JNIEnv *, jclass, jlong parameters
    ) {
        return parameters
                   ? static_cast<jint>(NVSDK_NGX_VULKAN_DestroyParameters(asParameters(parameters)))
                   : kInvalidParameter;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nCreateFeature(
        JNIEnv *env, jclass, jlong commandBuffer, jint feature, jlong parameters, jobject outFeature
    ) {
        if (!parameters || !outFeature) {
            return kInvalidParameter;
        }
        NVSDK_NGX_Handle *handle = nullptr;
        NVSDK_NGX_Result result = NVSDK_NGX_VULKAN_CreateFeature(
            reinterpret_cast<VkCommandBuffer>(commandBuffer),
            static_cast<NVSDK_NGX_Feature>(feature),
            asParameters(parameters),
            &handle
        );
        if (NVSDK_NGX_SUCCEED(result)) {
            setLongField(env, outFeature, "nativePointer", reinterpret_cast<jlong>(handle));
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nReleaseFeature(
        JNIEnv *, jclass, jlong feature
    ) {
        return feature
                   ? static_cast<jint>(NVSDK_NGX_VULKAN_ReleaseFeature(reinterpret_cast<NVSDK_NGX_Handle *>(feature)))
                   : kInvalidParameter;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nEvaluateFeature(
        JNIEnv *env, jclass, jlong commandBuffer, jlong feature, jlong parameters, jobject progressCallback
    ) {
        if (!feature || !parameters) {
            return kInvalidParameter;
        }
        ProgressCallbackContext context = {};
        if (progressCallback) {
            env->GetJavaVM(&context.javaVm);
            context.callback = env->NewGlobalRef(progressCallback);
            jclass callbackType = env->GetObjectClass(progressCallback);
            context.method = env->GetMethodID(callbackType, "onProgress", "(F)Z");
            env->DeleteLocalRef(callbackType);
            g_progressContext = &context;
        }
        NVSDK_NGX_Result result = NVSDK_NGX_VULKAN_EvaluateFeature_C(
            reinterpret_cast<VkCommandBuffer>(commandBuffer),
            reinterpret_cast<NVSDK_NGX_Handle *>(feature),
            asParameters(parameters),
            progressCallback ? ngxProgressCallback : nullptr
        );
        g_progressContext = nullptr;
        if (context.callback) {
            env->DeleteGlobalRef(context.callback);
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT void JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersSetUnsignedLong(
        JNIEnv *env, jclass, jlong parameters, jstring name, jlong value
    ) {
        std::string key = toUtf8(env, name);
        NVSDK_NGX_Parameter_SetULL(asParameters(parameters), key.c_str(), static_cast<unsigned long long>(value));
    }

    JNIEXPORT void JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersSetFloat(
        JNIEnv *env, jclass, jlong parameters, jstring name, jfloat value
    ) {
        std::string key = toUtf8(env, name);
        NVSDK_NGX_Parameter_SetF(asParameters(parameters), key.c_str(), value);
    }

    JNIEXPORT void JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersSetDouble(
        JNIEnv *env, jclass, jlong parameters, jstring name, jdouble value
    ) {
        std::string key = toUtf8(env, name);
        NVSDK_NGX_Parameter_SetD(asParameters(parameters), key.c_str(), value);
    }

    JNIEXPORT void JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersSetUnsignedInt(
        JNIEnv *env, jclass, jlong parameters, jstring name, jlong value
    ) {
        std::string key = toUtf8(env, name);
        NVSDK_NGX_Parameter_SetUI(asParameters(parameters), key.c_str(), static_cast<unsigned int>(value));
    }

    JNIEXPORT void JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersSetInt(
        JNIEnv *env, jclass, jlong parameters, jstring name, jint value
    ) {
        std::string key = toUtf8(env, name);
        NVSDK_NGX_Parameter_SetI(asParameters(parameters), key.c_str(), value);
    }

    JNIEXPORT void JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersSetPointer(
        JNIEnv *env, jclass, jlong parameters, jstring name, jlong value
    ) {
        std::string key = toUtf8(env, name);
        NVSDK_NGX_Parameter_SetVoidPointer(asParameters(parameters), key.c_str(), reinterpret_cast<void *>(value));
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersGetUnsignedLong(
        JNIEnv *env, jclass, jlong parameters, jstring name, jlongArray outValue
    ) {
        if (!outValue || env->GetArrayLength(outValue) < 1) {
            return kInvalidParameter;
        }
        std::string key = toUtf8(env, name);
        unsigned long long value = 0;
        NVSDK_NGX_Result result = NVSDK_NGX_Parameter_GetULL(asParameters(parameters), key.c_str(), &value);
        if (NVSDK_NGX_SUCCEED(result)) {
            auto javaValue = static_cast<jlong>(value);
            env->SetLongArrayRegion(outValue, 0, 1, &javaValue);
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersGetFloat(
        JNIEnv *env, jclass, jlong parameters, jstring name, jfloatArray outValue
    ) {
        if (!outValue || env->GetArrayLength(outValue) < 1) {
            return kInvalidParameter;
        }
        std::string key = toUtf8(env, name);
        float value = 0.0f;
        NVSDK_NGX_Result result = NVSDK_NGX_Parameter_GetF(asParameters(parameters), key.c_str(), &value);
        if (NVSDK_NGX_SUCCEED(result)) {
            env->SetFloatArrayRegion(outValue, 0, 1, &value);
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersGetDouble(
        JNIEnv *env, jclass, jlong parameters, jstring name, jdoubleArray outValue
    ) {
        if (!outValue || env->GetArrayLength(outValue) < 1) {
            return kInvalidParameter;
        }
        std::string key = toUtf8(env, name);
        double value = 0.0;
        NVSDK_NGX_Result result = NVSDK_NGX_Parameter_GetD(asParameters(parameters), key.c_str(), &value);
        if (NVSDK_NGX_SUCCEED(result)) {
            env->SetDoubleArrayRegion(outValue, 0, 1, &value);
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersGetUnsignedInt(
        JNIEnv *env, jclass, jlong parameters, jstring name, jlongArray outValue
    ) {
        if (!outValue || env->GetArrayLength(outValue) < 1) {
            return kInvalidParameter;
        }
        std::string key = toUtf8(env, name);
        unsigned int value = 0;
        NVSDK_NGX_Result result = NVSDK_NGX_Parameter_GetUI(asParameters(parameters), key.c_str(), &value);
        if (NVSDK_NGX_SUCCEED(result)) {
            auto javaValue = static_cast<jlong>(value);
            env->SetLongArrayRegion(outValue, 0, 1, &javaValue);
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersGetInt(
        JNIEnv *env, jclass, jlong parameters, jstring name, jintArray outValue
    ) {
        if (!outValue || env->GetArrayLength(outValue) < 1) {
            return kInvalidParameter;
        }
        std::string key = toUtf8(env, name);
        int value = 0;
        NVSDK_NGX_Result result = NVSDK_NGX_Parameter_GetI(asParameters(parameters), key.c_str(), &value);
        if (NVSDK_NGX_SUCCEED(result)) {
            jint javaValue = static_cast<jint>(value);
            env->SetIntArrayRegion(outValue, 0, 1, &javaValue);
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersGetPointer(
        JNIEnv *env, jclass, jlong parameters, jstring name, jlongArray outValue
    ) {
        if (!outValue || env->GetArrayLength(outValue) < 1) {
            return kInvalidParameter;
        }
        std::string key = toUtf8(env, name);
        void *value = nullptr;
        NVSDK_NGX_Result result = NVSDK_NGX_Parameter_GetVoidPointer(asParameters(parameters), key.c_str(), &value);
        if (NVSDK_NGX_SUCCEED(result)) {
            auto javaValue = reinterpret_cast<jlong>(value);
            env->SetLongArrayRegion(outValue, 0, 1, &javaValue);
        }
        return static_cast<jint>(result);
    }

    JNIEXPORT void JNICALL Java_io_homo_superresolution_core_ngx_NgxNative_nParametersReset(
        JNIEnv *, jclass, jlong parameters
    ) {
        if (parameters) {
            asParameters(parameters)->Reset();
        }
    }
}
