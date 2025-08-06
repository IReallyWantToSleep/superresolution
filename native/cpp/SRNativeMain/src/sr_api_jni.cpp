#include "sr/sr_api.h"
#include <vulkan/vulkan.h>
#include "JNI0.h"
#include "define.h"
#include "utils.h"

SRTextureResource fromJavaSRVkTextureResource(JNIEnv *env, jobject obj)
{
    jclass cls = env->GetObjectClass(obj);
    jfieldID imageFieldId = env->GetFieldID(cls, "image", JAVA_TYPE_LONG);
    jfieldID imageMemoryFieldId = env->GetFieldID(cls, "imageMemory", JAVA_TYPE_LONG);
    jfieldID imageViewFieldId = env->GetFieldID(cls, "imageView", JAVA_TYPE_LONG);
    jfieldID deviceFieldId = env->GetFieldID(cls, "device", JAVA_TYPE_LONG);

    jfieldID widthFieldId = env->GetFieldID(cls, "width", JAVA_TYPE_INT);
    jfieldID heightFieldId = env->GetFieldID(cls, "height", JAVA_TYPE_INT);

    jlong image = env->GetLongField(obj, imageFieldId);
    jlong imageMemory = env->GetLongField(obj, imageMemoryFieldId);
    jlong imageView = env->GetLongField(obj, imageViewFieldId);
    jlong device = env->GetLongField(obj, deviceFieldId);

    jint width = env->GetIntField(obj, widthFieldId);
    jint height = env->GetIntField(obj, heightFieldId);

    SRTextureResource resource = {};
    SRTextureResourceDescription desc = {};
    resource.handle = reinterpret_cast<VkImage>(image);
    desc.width = width;
    desc.height = height;
    resource.desc = desc;
    return resource;
}

jobject toJavaSRVkTextureResource(JNIEnv *env, SRTextureResource resource)
{
    jclass resClass = env->FindClass(JAVA_SRVK_TEXTURE_RESOURCE);
    jmethodID constrocMID = env->GetMethodID(
        resClass,
        "<init>",
        "(JJJJII)V");
    jobject obj = env->NewObject(
        resClass,
        constrocMID,
        reinterpret_cast<jlong>(resource.handle),
        0,
        0,
        0,
        resource.desc.width,
        resource.desc.height);
    return obj;
}

void *java_vkGetDeviceProcAddr(JNIEnv *env, const char *name)
{
    jclass cpp_helper = env->FindClass(JAVA_CPPHELPER_CLASS);
    jmethodID methodID = env->GetStaticMethodID(cpp_helper, "CPP_vkGetDeviceProcAddr", "(Ljava/lang/String;)J");
    if (methodID)
    {
        jstring jmsg = env->NewStringUTF(name);
        jlong jlongValue = env->CallStaticLongMethod(cpp_helper, methodID, jmsg);
        void *glfwProc = reinterpret_cast<void *>(jlongValue);
        env->DeleteLocalRef(jmsg);
        return glfwProc;
    }
}

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
    SRCreateUpscaleContextDesc desc = {};
    desc.renderSize = {static_cast<uint32_t>(renderSizeX), static_cast<uint32_t>(renderSizeY)};
    desc.upscaledSize = {static_cast<uint32_t>(upscaledSizeX), static_cast<uint32_t>(upscaledSizeY)};
    desc.device = (VkDevice)device;
    desc.phyDevice = (VkPhysicalDevice)phyDevice;
    desc.flags = static_cast<uint32_t>(flags);
    SRUpscaleContext *context = new SRUpscaleContext();
    return srCreateUpscaleContext(
        context,
        reinterpret_cast<SRUpscaleProvider *>(provider),
        &desc);
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrDestroyUpscaleContext(
    JNIEnv *env,
    jclass,
    jlong context)
{
    return srDestroyUpscaleContext(reinterpret_cast<SRUpscaleContext *>(context));
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
    SRUpscaleContext *context = reinterpret_cast<SRUpscaleContext *>(contextPtr);
    SRDispatchUpscaleDesc desc = {};
    desc.commandList = reinterpret_cast<void *>(commandList);
    desc.color = fromJavaSRVkTextureResource(env, color);
    desc.depth = fromJavaSRVkTextureResource(env, depth);
    desc.motionVectors = fromJavaSRVkTextureResource(env, motionVectors);
    desc.exposure = fromJavaSRVkTextureResource(env, exposure);
    desc.reactive = fromJavaSRVkTextureResource(env, reactive);
    desc.transparencyAndComposition = fromJavaSRVkTextureResource(env, transparencyAndComposition);
    desc.output = fromJavaSRVkTextureResource(env, output);

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
    jclass clazz,
    jlong context,
    jobject outResultObj,
    jint queryType)
{
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrGetUpscaleProvider(
    JNIEnv *env,
    jclass clazz,
    jobject outProvider,
    jlong providerId)
{
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_NsrLoadUpscaleProvidersFromLibrary(
    JNIEnv *env,
    jclass clazz,
    jstring libPath,
    jstring getProvidersFuncName,
    jstring getProvidersCountFuncName)
{
}