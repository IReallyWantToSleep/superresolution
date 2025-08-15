#include "sr/sr_api.h"
#include <vulkan/vulkan.h>
#include "JNI0.h"
#include "define.h"
#include "utils.h"
#include <string>
#include <iostream>
#include "sr/sr_modules.h"
static JNIEnv *g_envForCallback = nullptr;

void sr_message_callback_bridge(SRMsgType type, const wchar_t *message)
{
    if (!g_envForCallback || !message)
        return;

    std::wstring wmsg(message);
    std::string utf8msg(wmsg.begin(), wmsg.end());

    java_log(g_envForCallback, const_cast<char *>(utf8msg.c_str()), (int)type);
}

void throwJavaException(JNIEnv *env, const char *message, const char *exceptionClassName = "java/lang/RuntimeException")
{
    jclass exClass = env->FindClass(exceptionClassName);
    if (exClass == nullptr)
    {
        return;
    }
    env->ThrowNew(exClass, message);
}

SRTextureResourceDescription fromJavaSRTextureResourceDesc(JNIEnv *env, jobject obj)
{
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
    if (formatObj != nullptr)
    {
        jclass formatCls = env->GetObjectClass(formatObj);
        jfieldID valueFieldId = env->GetFieldID(formatCls, "value", "I");
        formatValue = env->GetIntField(formatObj, valueFieldId);
    }

    SRTextureResourceDescription desc = {};
    desc.format = static_cast<SRSurfaceFormat>(formatValue);
    desc.width = width;
    desc.height = height;
    desc.mipmapCount = mipmapCount;
    desc.usage = static_cast<SRResourceUsage>(usage);
    return desc;
}

SRTextureResource fromJavaSRTextureResourceVK(JNIEnv *env, jobject obj)
{
    g_envForCallback = env;

    jclass cls = env->GetObjectClass(obj);

    jfieldID imageFieldId = env->GetFieldID(cls, "handle", JAVA_TYPE_LONG);
    jlong image = env->GetLongField(obj, imageFieldId);

    jfieldID descFieldId = env->GetFieldID(cls, "description", "Lio/homo/superresolution/srapi/SRTextureResourceDescription;");
    jobject descObj = env->GetObjectField(obj, descFieldId);

    SRTextureResourceDescription desc = fromJavaSRTextureResourceDesc(env, descObj);

    SRTextureResource resource = {};
    resource.exist = true;
    resource.handle = reinterpret_cast<void *>(image);
    resource.desc = desc;

    return resource;
}

void *java_vkGetDeviceProcAddr(void *device, const char *name)
{

    if (!g_envForCallback)
    {
        std::cout << "g_envForCallback is null!!!!!!!!!!!!!!" << std::endl;
        return nullptr;
    };

    jclass cpp_helper = g_envForCallback->FindClass(JAVA_CPPHELPER_CLASS);
    jmethodID methodID = g_envForCallback->GetStaticMethodID(
        cpp_helper,
        "CPP_vkGetDeviceProcAddr",
        "(Ljava/lang/String;)J");

    if (!methodID)
    {
        std::cout << "methodID is null!!!!!!!!!!!!!!" << std::endl;
        return nullptr;
    }

    jstring jmsg = g_envForCallback->NewStringUTF(name);
    jlong jlongValue = g_envForCallback->CallStaticLongMethod(cpp_helper, methodID, jmsg);
    g_envForCallback->DeleteLocalRef(jmsg);

    return reinterpret_cast<void *>(jlongValue);
}

void *java_glfwGetProcAddress(void *device, const char *name)
{

    if (!g_envForCallback)
    {
        std::cout << "g_envForCallback is null!!!!!!!!!!!!!!" << std::endl;
        return nullptr;
    };

    jclass cpp_helper = g_envForCallback->FindClass(JAVA_CPPHELPER_CLASS);
    jmethodID methodID = g_envForCallback->GetStaticMethodID(
        cpp_helper,
        "CPP_glfwGetProcAddress",
        "(Ljava/lang/String;)J");

    if (!methodID)
    {
        std::cout << "methodID is null!!!!!!!!!!!!!!" << std::endl;
        return nullptr;
    }

    jstring jmsg = g_envForCallback->NewStringUTF(name);
    jlong jlongValue = g_envForCallback->CallStaticLongMethod(cpp_helper, methodID, jmsg);
    g_envForCallback->DeleteLocalRef(jmsg);

    return reinterpret_cast<void *>(jlongValue);
}

#ifdef __cplusplus
extern "C"
{
#endif

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrCreateUpscaleContext(
        JNIEnv *env,
        jclass,
        jobject outContextObj,
        jlong provider,
        jlong device,
        jlong phyDevice,
        jint upscaledSizeX,
        jint upscaledSizeY,
        jint renderSizeX,
        jint renderSizeY,
        jint flags)
    {
        g_envForCallback = env;

        SRCreateUpscaleContextDesc desc = *new SRCreateUpscaleContextDesc();
        desc.renderSize = *new SRVectorUint2{static_cast<uint32_t>(renderSizeX), static_cast<uint32_t>(renderSizeY)};
        desc.upscaledSize = *new SRVectorUint2{static_cast<uint32_t>(upscaledSizeX), static_cast<uint32_t>(upscaledSizeY)};
        desc.device = (VkDevice)device;
        desc.phyDevice = (VkPhysicalDevice)phyDevice;
        desc.flags = static_cast<uint32_t>(flags);
        desc.deviceProcAddr =  reinterpret_cast<SRUpscaleProvider *>(provider)->providerId == SR_MODULES_FSR2OGL_ID ? java_glfwGetProcAddress : java_vkGetDeviceProcAddr;
        desc.messageCallback = sr_message_callback_bridge;

        SRUpscaleContext *context = new SRUpscaleContext();
        SRReturnCode rc = srCreateUpscaleContext(
            context,
            reinterpret_cast<SRUpscaleProvider *>(provider),
            &desc);

        if (rc != SR_RETURN_CODE_OK)
        {
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
        jlong contextPtr)
    {
        g_envForCallback = env;

        SRUpscaleContext *context = reinterpret_cast<SRUpscaleContext *>(contextPtr);
        if (!context)
        {
            return SR_RETURN_CODE_ERROR;
        }

        SRReturnCode rc = srDestroyUpscaleContext(context);

        delete context;

        return rc;
    }
    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrDispatchUpscale(
        JNIEnv *env,
        jclass clazz,
        jlong contextPtr,
        jlong commandList,
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
        jint flags)
    {
        g_envForCallback = env;
        SRUpscaleContext *context = reinterpret_cast<SRUpscaleContext *>(contextPtr);
        SRDispatchUpscaleDesc desc = {};
        desc.commandList = reinterpret_cast<void *>(commandList);
        if (color)
        {
            desc.color = fromJavaSRTextureResourceVK(env, color);
        }
        else
        {
            desc.color = {};
            desc.color.exist = false;
        }

        if (depth)
        {
            desc.depth = fromJavaSRTextureResourceVK(env, depth);
        }
        else
        {
            desc.depth = {};
            desc.depth.exist = false;
        }

        if (motionVectors)
        {
            desc.motionVectors = fromJavaSRTextureResourceVK(env, motionVectors);
        }
        else
        {
            desc.motionVectors = {};
            desc.motionVectors.exist = false;
        }

        if (exposure)
        {
            desc.exposure = fromJavaSRTextureResourceVK(env, exposure);
        }
        else
        {
            desc.exposure = {};
            desc.exposure.exist = false;
        }

        if (reactive)
        {
            desc.reactive = fromJavaSRTextureResourceVK(env, reactive);
        }
        else
        {
            desc.reactive = {};
            desc.reactive.exist = false;
        }

        if (transparencyAndComposition)
        {
            desc.transparencyAndComposition = fromJavaSRTextureResourceVK(env, transparencyAndComposition);
        }
        else
        {
            desc.transparencyAndComposition = {};
            desc.transparencyAndComposition.exist = false;
        }

        if (output)
        {
            desc.output = fromJavaSRTextureResourceVK(env, output);
        }
        else
        {
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

        return (jint)srDispatchUpscale(context, &desc);
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrQueryUpscaleContext(
        JNIEnv *env,
        jclass,
        jlong contextPtr,
        jobject outResultObj,
        jint queryType)
    {
        g_envForCallback = env;

        auto *context = reinterpret_cast<SRUpscaleContext *>(contextPtr);
        SRUpscaleContextQueryResult result = {};

        SRReturnCode code = srQueryUpscaleContext(context, &result, static_cast<SRUpscaleContextQueryType>(queryType));

        if (code != SR_RETURN_CODE_OK)
            return code;

        jclass resultCls = env->GetObjectClass(outResultObj);
        jfieldID typeField = env->GetFieldID(resultCls, "type", "I");
        env->SetIntField(outResultObj, typeField, static_cast<jint>(queryType));

        if (queryType == SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO)
        {
            auto *verInfo = static_cast<SRUpscaleContextQueryVersionInfoResult *>(result.data);
            jfieldID versionNumberField = env->GetFieldID(resultCls, "versionNumber", "J");
            jfieldID versionIdField = env->GetFieldID(resultCls, "versionId", "J");
            jfieldID versionNameField = env->GetFieldID(resultCls, "versionName", "Ljava/lang/String;");
            env->SetLongField(outResultObj, versionNumberField, static_cast<jlong>(verInfo->versionNumber));
            env->SetLongField(outResultObj, versionIdField, static_cast<jlong>(verInfo->versionId));
            env->SetObjectField(outResultObj, versionNameField, env->NewStringUTF(verInfo->versionName));
        }
        else if (queryType == SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO)
        {
            auto *memInfo = static_cast<SRUpscaleContextQueryGpuMemoryInfoResult *>(result.data);
            jfieldID gpuMemField = env->GetFieldID(resultCls, "gpuMemory", "J");
            env->SetLongField(outResultObj, gpuMemField, static_cast<jlong>(memInfo->gpuMemory));
        }

        return code;
    }

    JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrGetUpscaleProvider(
        JNIEnv *env,
        jclass,
        jobject outProvider,
        jlong providerId)
    {
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
        jstring getProvidersCountFuncName)
    {
        g_envForCallback = env;

        if (libPath == nullptr || getProvidersFuncName == nullptr || getProvidersCountFuncName == nullptr)
        {
            throwJavaException(env, "One or more input strings are null.");
            return SR_RETURN_CODE_ERROR;
        }

        const jchar *libPathChars = env->GetStringChars(libPath, nullptr);
        std::wstring wLibPath(reinterpret_cast<const wchar_t *>(libPathChars), env->GetStringLength(libPath));
        env->ReleaseStringChars(libPath, libPathChars);

        const char *funcName = env->GetStringUTFChars(getProvidersFuncName, nullptr);
        const char *countName = env->GetStringUTFChars(getProvidersCountFuncName, nullptr);

        std::string funcNameStr(funcName);
        std::string countNameStr(countName);

        env->ReleaseStringUTFChars(getProvidersFuncName, funcName);
        env->ReleaseStringUTFChars(getProvidersCountFuncName, countName);

        return srLoadUpscaleProvidersFromLibrary(wLibPath, funcNameStr, countNameStr, sr_message_callback_bridge);
    }

#ifdef __cplusplus
}
#endif