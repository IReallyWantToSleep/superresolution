#include "sr/sr_api.h"
#include <vulkan/vulkan.h>
#include "define.h"
#include "utils.h"
#include <string>
#include <iostream>
#include "sr/sr_modules.h"
#include <codecvt>
#include <locale>
#include <cstring>
#include "io_homo_superresolution_core_SuperResolutionNative.h"
static JNIEnv *g_envForCallback = nullptr;

void sr_message_callback_bridge(SRMessageType type, const wchar_t *message) {
    if (!g_envForCallback || !message)
        return;

    try {
        std::wstring wmsg(message);
        // 显式转换为UTF-8,在大多数时候和大多数平台下应该没问题
        std::wstring_convert<std::codecvt_utf8<wchar_t> > converter;
        std::string utf8msg = converter.to_bytes(wmsg);

        java_log(g_envForCallback, const_cast<char *>(utf8msg.c_str()), (int) type);
    } catch (const std::exception &e) {
        // Fallback: 如果wstring转换失败，尝试逐字符转换，跳过无效字符
        std::string fallback = "[wstring conversion failed: ";
        fallback += e.what();
        fallback += "] ";
        const wchar_t *p = message;
        while (*p) {
            if (*p >= 0x20 && *p < 0x7F) {
                fallback += static_cast<char>(*p);
            } else {
                fallback += '?';
            }
            ++p;
        }
        java_log(g_envForCallback, const_cast<char *>(fallback.c_str()), (int) type);
    }
}

void throwJavaException(JNIEnv *env, const char *message,
                        const char *exceptionClassName = "java/lang/RuntimeException") {
    jclass exClass = env->FindClass(exceptionClassName);
    if (exClass == nullptr) {
        return;
    }
    env->ThrowNew(exClass, message);
}

SRTextureResourceDescription fromJavaSRTextureResourceDesc(JNIEnv *env, jobject obj) {
    g_envForCallback = env;

    jclass cls = env->GetObjectClass(obj);

    jfieldID widthFieldId = env->GetFieldID(cls, "width", JAVA_TYPE_INT);
    jfieldID heightFieldId = env->GetFieldID(cls, "height", JAVA_TYPE_INT);
    jfieldID mipmapCountFieldId = env->GetFieldID(cls, "mipmapCount", JAVA_TYPE_INT);
    jfieldID usageFieldId = env->GetFieldID(cls, "usage", JAVA_TYPE_INT);
    jfieldID formatFieldId = env->GetFieldID(cls, "format", "Lio/homo/superresolution/srapi/SRSurfaceFormat;");

    jint width = env->GetIntField(obj, widthFieldId);
    jint height = env->GetIntField(obj, heightFieldId);
    jint mipmapCount = env->GetIntField(obj, mipmapCountFieldId);
    jint usage = env->GetIntField(obj, usageFieldId);

    jobject formatObj = env->GetObjectField(obj, formatFieldId);
    jint formatValue = 0;
    if (formatObj != nullptr) {
        jclass formatCls = env->GetObjectClass(formatObj);
        jfieldID valueFieldId = env->GetFieldID(formatCls, "value", "I");
        formatValue = env->GetIntField(formatObj, valueFieldId);
    }

    SRTextureResourceDescription desc = {};
    desc.format = static_cast<SRTextureFormat>(formatValue);
    desc.width = width;
    desc.height = height;
    desc.mipmapCount = mipmapCount;
    desc.usage = static_cast<SRResourceUsage>(usage);
    return desc;
}

SRTextureResource fromJavaSRTextureResourceVK(JNIEnv *env, jobject obj) {
    g_envForCallback = env;

    jclass cls = env->GetObjectClass(obj);

    jfieldID imageFieldId = env->GetFieldID(cls, "handle", JAVA_TYPE_LONG);
    jlong image = env->GetLongField(obj, imageFieldId);

    jfieldID imageViewFieldId = env->GetFieldID(cls, "imageView", JAVA_TYPE_LONG);
    jlong imageView = env->GetLongField(obj, imageViewFieldId);

    jfieldID descFieldId = env->GetFieldID(cls, "description",
                                           "Lio/homo/superresolution/srapi/SRTextureResourceDescription;");
    jobject descObj = env->GetObjectField(obj, descFieldId);

    SRTextureResourceDescription desc = fromJavaSRTextureResourceDesc(env, descObj);

    SRTextureResource resource = {};
    resource.exist = true;
    resource.handle = reinterpret_cast<void *>(image);
    resource.desc = desc;
    resource.imageView = reinterpret_cast<void *>(imageView);
    return resource;
}

void *java_vkGetDeviceProcAddr(void *device, const char *name) {
    if (!g_envForCallback) {
        std::cout << "g_envForCallback is null!!!!!!!!!!!!!!" << std::endl;
        return nullptr;
    };

    jclass cpp_helper = g_envForCallback->FindClass(JAVA_CPPHELPER_CLASS);
    jmethodID methodID = g_envForCallback->GetStaticMethodID(
        cpp_helper,
        "CPP_vkGetDeviceProcAddr",
        "(Ljava/lang/String;)J");

    if (!methodID) {
        std::cout << "methodID is null!!!!!!!!!!!!!!" << std::endl;
        return nullptr;
    }

    jstring jmsg = g_envForCallback->NewStringUTF(name);
    jlong jlongValue = g_envForCallback->CallStaticLongMethod(cpp_helper, methodID, jmsg);
    g_envForCallback->DeleteLocalRef(jmsg);

    return reinterpret_cast<void *>(jlongValue);
}

void *java_glfwGetProcAddress(void *device, const char *name) {
    if (!g_envForCallback) {
        std::cout << "g_envForCallback is null!!!!!!!!!!!!!!" << std::endl;
        return nullptr;
    };

    jclass cpp_helper = g_envForCallback->FindClass(JAVA_CPPHELPER_CLASS);
    jmethodID methodID = g_envForCallback->GetStaticMethodID(
        cpp_helper,
        "CPP_glfwGetProcAddress",
        "(Ljava/lang/String;)J");

    if (!methodID) {
        std::cout << "methodID is null!!!!!!!!!!!!!!" << std::endl;
        return nullptr;
    }

    jstring jmsg = g_envForCallback->NewStringUTF(name);
    jlong jlongValue = g_envForCallback->CallStaticLongMethod(cpp_helper, methodID, jmsg);
    g_envForCallback->DeleteLocalRef(jmsg);

    return reinterpret_cast<void *>(jlongValue);
}

#ifdef __cplusplus
extern "C" {
    #endif

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrCreateUpscaleContext(
        JNIEnv *env,
        jclass,
        jobject outContextObj,
        jlong provider,
        jint renderApiType,
        jobject openglDeviceInfo,
        jobject vulkanDeviceInfo,
        jint upscaledSizeX,
        jint upscaledSizeY,
        jint renderSizeX,
        jint renderSizeY,
        jlong messageCallback,
        jlong extraParamsPtr,
        jint flags) {
        g_envForCallback = env;

        SRCreateUpscaleContextDesc desc = {};
        desc.renderApiType = static_cast<SRRenderApiType>(renderApiType);
        desc.renderSize = {static_cast<uint32_t>(renderSizeX), static_cast<uint32_t>(renderSizeY)};
        desc.upscaledSize = {static_cast<uint32_t>(upscaledSizeX), static_cast<uint32_t>(upscaledSizeY)};
        desc.flags = static_cast<uint32_t>(static_cast<int32_t>(flags));
        desc.messageCallback = sr_message_callback_bridge;

        if (extraParamsPtr != 0) {
            desc.extraParams = *reinterpret_cast<SRContextExtraParams *>(extraParamsPtr);
        }

        if (renderApiType == SR_RENDER_API_TYPE_OPENGL) {
            if (openglDeviceInfo != nullptr) {
                jclass glInfoCls = env->GetObjectClass(openglDeviceInfo);
                jfieldID deviceProcAddrField = env->GetFieldID(glInfoCls, "deviceProcAddr", "J");
                jlong deviceProcAddr = env->GetLongField(openglDeviceInfo, deviceProcAddrField);

                desc.renderDeviceInfo.opengl.deviceProcAddr = deviceProcAddr != 0
                                                                  ? reinterpret_cast<SRGetFuncAddress>(deviceProcAddr)
                                                                  : java_glfwGetProcAddress;
            } else {
                desc.renderDeviceInfo.opengl.deviceProcAddr = java_glfwGetProcAddress;
            }
        } else if (renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            if (vulkanDeviceInfo != nullptr) {
                jclass vkInfoCls = env->GetObjectClass(vulkanDeviceInfo);
                jfieldID instanceField = env->GetFieldID(vkInfoCls, "instance", "J");
                jfieldID physicalDeviceField = env->GetFieldID(vkInfoCls, "physicalDevice", "J");
                jfieldID deviceField = env->GetFieldID(vkInfoCls, "device", "J");
                jfieldID initCommandBufferField = env->GetFieldID(vkInfoCls, "initCommandBuffer", "J");
                jfieldID deviceProcAddrField = env->GetFieldID(vkInfoCls, "deviceProcAddr", "J");
                jfieldID instanceProcAddrField = env->GetFieldID(vkInfoCls, "instanceProcAddr", "J");

                desc.renderDeviceInfo.vulkan.instance = reinterpret_cast<VkInstance>(env->GetLongField(
                    vulkanDeviceInfo, instanceField));
                desc.renderDeviceInfo.vulkan.physicalDevice = reinterpret_cast<VkPhysicalDevice>(env->GetLongField(
                    vulkanDeviceInfo, physicalDeviceField));
                desc.renderDeviceInfo.vulkan.device = reinterpret_cast<VkDevice>(env->GetLongField(
                    vulkanDeviceInfo, deviceField));
                desc.renderDeviceInfo.vulkan.initCommandBuffer = reinterpret_cast<VkCommandBuffer>(env->GetLongField(
                    vulkanDeviceInfo, initCommandBufferField));

                jlong deviceProcAddr = env->GetLongField(vulkanDeviceInfo, deviceProcAddrField);
                jlong instanceProcAddr = env->GetLongField(vulkanDeviceInfo, instanceProcAddrField);

                desc.renderDeviceInfo.vulkan.deviceProcAddr = deviceProcAddr != 0
                                                                  ? reinterpret_cast<SRGetFuncAddress>(deviceProcAddr)
                                                                  : java_vkGetDeviceProcAddr;
                desc.renderDeviceInfo.vulkan.instanceProcAddr = instanceProcAddr != 0
                                                                    ? reinterpret_cast<SRGetFuncAddress>(
                                                                        instanceProcAddr)
                                                                    : nullptr;
            } else {
                sr_message_callback_bridge(SR_MESSAGE_TYPE_ERROR,
                                           L"Vulkan device info is required for Vulkan API type.");
                return SR_RETURN_CODE_INVALID_ARGUMENT;
            }
        } else {
            sr_message_callback_bridge(SR_MESSAGE_TYPE_ERROR, L"Invalid render API type.");
            return SR_RETURN_CODE_INVALID_ARGUMENT;
        }

        SRUpscaleContext *context = new SRUpscaleContext();
        SRReturnCode rc = srCreateUpscaleContext(
            context,
            reinterpret_cast<SRUpscaleProvider *>(provider),
            &desc);

        if (rc != SR_RETURN_CODE_OK) {
            delete context;
            return rc;
        }
        jclass cls = env->GetObjectClass(outContextObj);
        jfieldID nativePtrField = env->GetFieldID(cls, "nativePtr", "J");
        env->SetLongField(outContextObj, nativePtrField, reinterpret_cast<jlong>(context));

        return rc;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrDestroyUpscaleContext(
        JNIEnv *env,
        jclass,
        jlong contextPtr) {
        g_envForCallback = env;

        SRUpscaleContext *context = reinterpret_cast<SRUpscaleContext *>(contextPtr);
        if (!context) {
            return SR_RETURN_CODE_NULL_POINTER;
        }

        SRReturnCode rc = srDestroyUpscaleContext(context);

        delete context;

        return rc;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrDispatchUpscale(
        JNIEnv *env,
        jclass clazz,
        jlong contextPtr,
        jint renderApiType,
        jlong vulkanCommandBuffer,
        jobject color,
        jobject depth,
        jobject motionVectors,
        jobject exposure,
        jobject reactive,
        jobject transparencyAndComposition,
        jobject output,
        jfloat jitterOffsetX,
        jfloat jitterOffsetY,
        jfloat motionVectorScaleX,
        jfloat motionVectorScaleY,
        jint renderSizeX,
        jint renderSizeY,
        jint upscaleSizeX,
        jint upscaleSizeY,
        jfloat frameTimeDelta,
        jboolean enableSharpening,
        jfloat sharpness,
        jfloat preExposure,
        jfloat cameraNear,
        jfloat cameraFar,
        jfloat cameraFovAngleVertical,
        jfloat viewSpaceToMetersFactor,
        jboolean reset,
        jlong extraParamsPtr,
        jint flags) {
        g_envForCallback = env;
        SRUpscaleContext *context = reinterpret_cast<SRUpscaleContext *>(contextPtr);
        SRDispatchUpscaleDesc desc = {};

        desc.commandList.renderApiType = static_cast<SRRenderApiType>(renderApiType);
        if (renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            desc.commandList.apiCommandBuffer.vulkan.commandBuffer = reinterpret_cast<VkCommandBuffer>(
                vulkanCommandBuffer);
        }

        if (extraParamsPtr != 0) {
            desc.extraParams = *reinterpret_cast<SRContextExtraParams *>(extraParamsPtr);
        }
        if (color) {
            desc.color = fromJavaSRTextureResourceVK(env, color);
        } else {
            desc.color = {};
            desc.color.exist = false;
        }

        if (depth) {
            desc.depth = fromJavaSRTextureResourceVK(env, depth);
        } else {
            desc.depth = {};
            desc.depth.exist = false;
        }

        if (motionVectors) {
            desc.motionVectors = fromJavaSRTextureResourceVK(env, motionVectors);
        } else {
            desc.motionVectors = {};
            desc.motionVectors.exist = false;
        }

        if (exposure) {
            desc.exposure = fromJavaSRTextureResourceVK(env, exposure);
        } else {
            desc.exposure = {};
            desc.exposure.exist = false;
        }

        if (reactive) {
            desc.reactive = fromJavaSRTextureResourceVK(env, reactive);
        } else {
            desc.reactive = {};
            desc.reactive.exist = false;
        }

        if (transparencyAndComposition) {
            desc.transparencyAndComposition = fromJavaSRTextureResourceVK(env, transparencyAndComposition);
        } else {
            desc.transparencyAndComposition = {};
            desc.transparencyAndComposition.exist = false;
        }

        if (output) {
            desc.output = fromJavaSRTextureResourceVK(env, output);
        } else {
            desc.output = {};
            desc.output.exist = false;
        }

        desc.jitterOffset.x = jitterOffsetX;
        desc.jitterOffset.y = jitterOffsetY;
        desc.motionVectorScale.x = motionVectorScaleX;
        desc.motionVectorScale.y = motionVectorScaleY;
        desc.renderSize.x = renderSizeX;
        desc.renderSize.y = renderSizeY;
        desc.upscaleSize.x = upscaleSizeX;
        desc.upscaleSize.y = upscaleSizeY;

        desc.frameTimeDelta = frameTimeDelta;
        desc.enableSharpening = enableSharpening;
        desc.sharpness = sharpness;
        desc.preExposure = preExposure;

        desc.cameraNear = cameraNear;
        desc.cameraFar = cameraFar;
        desc.cameraFovAngleVertical = cameraFovAngleVertical;
        desc.viewSpaceToMetersFactor = viewSpaceToMetersFactor;

        desc.reset = reset;

        return (jint) srDispatchUpscale(context, &desc);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrQueryUpscaleContext(
        JNIEnv *env,
        jclass,
        jlong contextPtr,
        jobject outResultObj,
        jint queryType) {
        g_envForCallback = env;

        auto *context = reinterpret_cast<SRUpscaleContext *>(contextPtr);
        SRUpscaleContextQueryResult result = {};

        SRReturnCode code = srQueryUpscaleContext(context, &result, static_cast<SRUpscaleContextQueryType>(queryType));

        if (code != SR_RETURN_CODE_OK)
            return code;

        jclass resultCls = env->GetObjectClass(outResultObj);
        if (queryType == SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO) {
            auto *verInfo = static_cast<SRQueryVersionResult *>(result.data);
            jfieldID versionNumberField = env->GetFieldID(resultCls, "versionNumber", "J");
            jfieldID versionIdField = env->GetFieldID(resultCls, "versionId", "J");
            env->SetLongField(outResultObj, versionNumberField, static_cast<jlong>(verInfo->versionNumber));
            env->SetLongField(outResultObj, versionIdField, static_cast<jlong>(verInfo->versionId));
        } else if (queryType == SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO) {
            auto *memInfo = static_cast<SRQueryGpuMemoryResult *>(result.data);
            jfieldID gpuMemField = env->GetFieldID(resultCls, "gpuMemory", "J");
            env->SetLongField(outResultObj, gpuMemField, static_cast<jlong>(memInfo->gpuMemory));
        } else if (queryType == SR_UPSCALE_CONTEXT_QUERY_AVAILABLE) {
            auto *memInfo = static_cast<SRQueryAvailabilityResult *>(result.data);
            jfieldID isAvailableField = env->GetFieldID(resultCls, "isAvailable", "Z");
            env->SetBooleanField(outResultObj, isAvailableField, static_cast<jboolean>(memInfo->isAvailable));
        }

        return code;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrGetUpscaleProvider(
        JNIEnv *env,
        jclass,
        jobject outProvider,
        jlong providerId) {
        g_envForCallback = env;

        SRUpscaleProvider *provider = new SRUpscaleProvider();
        SRReturnCode code = srGetUpscaleProvider(provider, static_cast<uint64_t>(providerId));

        if (code != SR_RETURN_CODE_OK)
            return code;

        jclass providerCls = env->GetObjectClass(outProvider);
        jfieldID nativePtrField = env->GetFieldID(providerCls, "nativePtr", "J");
        env->SetLongField(outProvider, nativePtrField, reinterpret_cast<jlong>(provider));
        return code;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrLoadUpscaleProvidersFromLibrary(
        JNIEnv *env,
        jclass,
        jstring libPath,
        jstring getProvidersFuncName,
        jstring getProvidersCountFuncName) {
        g_envForCallback = env;

        if (libPath == nullptr || getProvidersFuncName == nullptr || getProvidersCountFuncName == nullptr) {
            throwJavaException(env, "One or more input strings are null.");
            return SR_RETURN_CODE_NULL_POINTER;
        }

        const char *libPathChars = env->GetStringUTFChars(libPath, nullptr);
        std::string LibPath(reinterpret_cast<const char *>(libPathChars), env->GetStringUTFLength(libPath));
        env->ReleaseStringUTFChars(libPath, libPathChars);

        const char *funcName = env->GetStringUTFChars(getProvidersFuncName, nullptr);
        const char *countName = env->GetStringUTFChars(getProvidersCountFuncName, nullptr);

        std::string funcNameStr(funcName);
        std::string countNameStr(countName);

        env->ReleaseStringUTFChars(getProvidersFuncName, funcName);
        env->ReleaseStringUTFChars(getProvidersCountFuncName, countName);

        return srLoadUpscaleProvidersFromLibrary(LibPath, funcNameStr, countNameStr, sr_message_callback_bridge);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrInitUpscaleContext(
        JNIEnv *, jclass, jlong contextPtr) {
        auto context = reinterpret_cast<SRUpscaleContext *>(contextPtr);
        if (!context) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        return srInitUpscaleContext(context);
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_SuperResolutionNative_NsrCreateParams(JNIEnv *env, jclass) {
        g_envForCallback = env;
        SRContextExtraParams *params = new SRContextExtraParams();
        memset(params, 0, sizeof(SRContextExtraParams));
        params->extraParamCount = 0;
        return reinterpret_cast<jlong>(params);
    }

    JNIEXPORT void JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrDestroyParams(
        JNIEnv *env, jclass, jlong paramsPtr) {
        g_envForCallback = env;
        if (paramsPtr != 0) {
            auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
            srDestroyExtraParams(params);
            delete params;
        }
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsSetBool(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jboolean value) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return SR_RETURN_CODE_NULL_POINTER;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        SRReturnCode code = srParamsSetBool(params, nameChars, static_cast<bool>(value));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jint>(code);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsSetInt32(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jint value) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return SR_RETURN_CODE_NULL_POINTER;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        SRReturnCode code = srParamsSetInt32(params, nameChars, static_cast<int32_t>(value));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jint>(code);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsSetUint32(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jlong value) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return SR_RETURN_CODE_NULL_POINTER;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        SRReturnCode code = srParamsSetUint32(params, nameChars, static_cast<uint32_t>(value));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jint>(code);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsSetFloat(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jfloat value) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return SR_RETURN_CODE_NULL_POINTER;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        SRReturnCode code = srParamsSetFloat(params, nameChars, static_cast<float>(value));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jint>(code);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsSetDouble(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jdouble value) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return SR_RETURN_CODE_NULL_POINTER;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        SRReturnCode code = srParamsSetDouble(params, nameChars, static_cast<double>(value));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jint>(code);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsSetString(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jstring value) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return SR_RETURN_CODE_NULL_POINTER;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        const char *valueChars = value != nullptr ? env->GetStringUTFChars(value, nullptr) : nullptr;
        SRReturnCode code = srParamsSetString(params, nameChars, valueChars);
        env->ReleaseStringUTFChars(name, nameChars);
        if (valueChars != nullptr)
            env->ReleaseStringUTFChars(value, valueChars);
        return static_cast<jint>(code);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsSetPointer(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jlong value) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return SR_RETURN_CODE_NULL_POINTER;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        SRReturnCode code = srParamsSetPointer(params, nameChars, reinterpret_cast<void *>(value));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jint>(code);
    }

    JNIEXPORT jlong JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrFindParam(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return 0;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        const SRContextExtraParam *param = srFindParam(params, nameChars);
        env->ReleaseStringUTFChars(name, nameChars);
        return reinterpret_cast<jlong>(param);
    }

    JNIEXPORT jboolean JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsGetBool(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jboolean defaultValue) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return defaultValue;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        bool outValue = false;
        srParamsGetBool(params, nameChars, &outValue, static_cast<bool>(defaultValue));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jboolean>(outValue);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsGetInt32(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jint defaultValue) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return defaultValue;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        int32_t outValue = 0;
        srParamsGetInt32(params, nameChars, &outValue, static_cast<int32_t>(defaultValue));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jint>(outValue);
    }

    JNIEXPORT jlong JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsGetUint32(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jlong defaultValue) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return defaultValue;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        uint32_t outValue = 0;
        srParamsGetUint32(params, nameChars, &outValue, static_cast<uint32_t>(defaultValue));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jlong>(outValue);
    }

    JNIEXPORT jfloat JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsGetFloat(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jfloat defaultValue) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return defaultValue;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        float outValue = 0.0f;
        srParamsGetFloat(params, nameChars, &outValue, static_cast<float>(defaultValue));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jfloat>(outValue);
    }

    JNIEXPORT jdouble JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsGetDouble(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jdouble defaultValue) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return defaultValue;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        double outValue = 0.0;
        srParamsGetDouble(params, nameChars, &outValue, static_cast<double>(defaultValue));
        env->ReleaseStringUTFChars(name, nameChars);
        return static_cast<jdouble>(outValue);
    }

    JNIEXPORT jstring JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsGetString(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name, jstring defaultValue) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return defaultValue;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        const char *defValueChars = defaultValue != nullptr ? env->GetStringUTFChars(defaultValue, nullptr) : nullptr;
        const char *outValue = nullptr;
        srParamsGetString(params, nameChars, &outValue, defValueChars);
        env->ReleaseStringUTFChars(name, nameChars);
        if (defValueChars != nullptr)
            env->ReleaseStringUTFChars(defaultValue, defValueChars);

        return outValue != nullptr ? env->NewStringUTF(outValue) : defaultValue;
    }

    JNIEXPORT jlong JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamsGetPointer(
        JNIEnv *env, jclass, jlong paramsPtr, jstring name) {
        g_envForCallback = env;
        if (paramsPtr == 0 || name == nullptr)
            return 0;

        auto *params = reinterpret_cast<SRContextExtraParams *>(paramsPtr);
        const char *nameChars = env->GetStringUTFChars(name, nullptr);
        void *outValue = nullptr;
        srParamsGetPointer(params, nameChars, &outValue);
        env->ReleaseStringUTFChars(name, nameChars);
        return reinterpret_cast<jlong>(outValue);
    }

    JNIEXPORT jstring JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamGetName(
        JNIEnv *env, jclass, jlong paramPtr) {
        g_envForCallback = env;
        if (paramPtr == 0)
            return nullptr;

        auto *param = reinterpret_cast<const SRContextExtraParam *>(paramPtr);
        return param->name != nullptr ? env->NewStringUTF(param->name) : nullptr;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamGetValueType(
        JNIEnv *env, jclass, jlong paramPtr) {
        g_envForCallback = env;
        if (paramPtr == 0)
            return SR_PARAM_VALUE_TYPE_UNKNOWN;

        auto *param = reinterpret_cast<const SRContextExtraParam *>(paramPtr);
        return static_cast<jint>(param->valueType);
    }

    JNIEXPORT jboolean JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamGetValueAsBool(
        JNIEnv *env, jclass, jlong paramPtr) {
        g_envForCallback = env;
        if (paramPtr == 0)
            return JNI_FALSE;

        auto *param = reinterpret_cast<const SRContextExtraParam *>(paramPtr);
        return param->valueType == SR_PARAM_VALUE_TYPE_BOOL ? static_cast<jboolean>(param->value.boolValue) : JNI_FALSE;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamGetValueAsInt32(
        JNIEnv *env, jclass, jlong paramPtr) {
        g_envForCallback = env;
        if (paramPtr == 0)
            return 0;

        auto *param = reinterpret_cast<const SRContextExtraParam *>(paramPtr);
        return param->valueType == SR_PARAM_VALUE_TYPE_INT32 ? static_cast<jint>(param->value.int32Value) : 0;
    }

    JNIEXPORT jlong JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamGetValueAsUint32(
        JNIEnv *env, jclass, jlong paramPtr) {
        g_envForCallback = env;
        if (paramPtr == 0)
            return 0;

        auto *param = reinterpret_cast<const SRContextExtraParam *>(paramPtr);
        return param->valueType == SR_PARAM_VALUE_TYPE_UINT32 ? static_cast<jlong>(param->value.uint32Value) : 0;
    }

    JNIEXPORT jfloat JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamGetValueAsFloat(
        JNIEnv *env, jclass, jlong paramPtr) {
        g_envForCallback = env;
        if (paramPtr == 0)
            return 0.0f;

        auto *param = reinterpret_cast<const SRContextExtraParam *>(paramPtr);
        return param->valueType == SR_PARAM_VALUE_TYPE_FLOAT ? static_cast<jfloat>(param->value.floatValue) : 0.0f;
    }

    JNIEXPORT jdouble JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamGetValueAsDouble(
        JNIEnv *env, jclass, jlong paramPtr) {
        g_envForCallback = env;
        if (paramPtr == 0)
            return 0.0;

        auto *param = reinterpret_cast<const SRContextExtraParam *>(paramPtr);
        return param->valueType == SR_PARAM_VALUE_TYPE_DOUBLE ? static_cast<jdouble>(param->value.doubleValue) : 0.0;
    }

    JNIEXPORT jstring JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamGetValueAsString(
        JNIEnv *env, jclass, jlong paramPtr) {
        g_envForCallback = env;
        if (paramPtr == 0)
            return nullptr;

        auto *param = reinterpret_cast<const SRContextExtraParam *>(paramPtr);
        if (param->valueType == SR_PARAM_VALUE_TYPE_STRING && param->value.stringValue != nullptr) {
            return env->NewStringUTF(param->value.stringValue);
        }
        return nullptr;
    }

    JNIEXPORT jlong JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrParamGetValueAsPointer(
        JNIEnv *env, jclass, jlong paramPtr) {
        g_envForCallback = env;
        if (paramPtr == 0)
            return 0;

        auto *param = reinterpret_cast<const SRContextExtraParam *>(paramPtr);
        return param->valueType == SR_PARAM_VALUE_TYPE_POINTER ? reinterpret_cast<jlong>(param->value.ptrValue) : 0;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrShutdown(JNIEnv *, jclass) {
        return (jint) srShutdown();
    }

    #ifdef __cplusplus
}
#endif