#include "utils.h"
#include <iostream>
#include <vector>
#include "ffx-fsr2-api/ffx_types.h"
#include "vulkan/vulkan.h"
#include "define.h"

JNIEnv *cur_env;
void set_env(JNIEnv *env)
{
    cur_env = env;
}

JNIEnv *get_env()
{
    return cur_env;
}

void check_env(JNIEnv *env)
{
    set_env(env);
}

void java_log(const char *msg, int level)
{
    jclass cpp_helper = cur_env->FindClass(JAVA_CPPHELPER_CLASS);
    jmethodID methodID = cur_env->GetStaticMethodID(cpp_helper, "CPP_Log", "(Ljava/lang/String;I)V");
    jstring jmsg = cur_env->NewStringUTF(msg);
    cur_env->CallStaticVoidMethod(cpp_helper, methodID, jmsg, jint(level));
    cur_env->DeleteLocalRef(jmsg);
}

GLFWglproc java_glfwGetProcAddress(const char *name)
{
    jclass cpp_helper = cur_env->FindClass(JAVA_CPPHELPER_CLASS);
    jmethodID methodID = cur_env->GetStaticMethodID(cpp_helper, "CPP_glfwGetProcAddress", "(Ljava/lang/String;)J");
    if (methodID)
    {
        jstring jmsg = cur_env->NewStringUTF(name);
        jlong jlongValue = cur_env->CallStaticLongMethod(cpp_helper, methodID, jmsg);
        GLFWglproc glfwProc = reinterpret_cast<GLFWglproc>(jlongValue);
        cur_env->DeleteLocalRef(jmsg);
        return glfwProc;
    }
    return 0;
}

bool ToCppBool(jboolean value)
{
    return value == JNI_TRUE;
}

FfxResource ffxResourceJavaToCpp(JNIEnv *env, jobject javaffxres)
{
    if (javaffxres == NULL)
    {
        return {};
    }
    jclass cls = env->GetObjectClass(javaffxres);
    jfieldID resourceFieldId = env->GetFieldID(cls, "resource", "J");
    jfieldID isDepthFieldId = env->GetFieldID(cls, "isDepth", "Z");
    jfieldID descriptorDataFieldId = env->GetFieldID(cls, "descriptorData", "J");
    jfieldID typeFieldId = env->GetFieldID(cls, "type", "I");
    jfieldID formatFieldId = env->GetFieldID(cls, "format", "I");
    jfieldID widthFieldId = env->GetFieldID(cls, "width", "I");
    jfieldID heightFieldId = env->GetFieldID(cls, "height", "I");
    jfieldID depthFieldId = env->GetFieldID(cls, "depth", "I");
    jfieldID mipCountFieldId = env->GetFieldID(cls, "mipCount", "I");
    jfieldID flagsFieldId = env->GetFieldID(cls, "flags", "I");
    jfieldID stateFieldId = env->GetFieldID(cls, "state", "I");
    uint64_t resourceField = env->GetLongField(javaffxres, resourceFieldId);
    bool isDepth = env->GetBooleanField(javaffxres, isDepthFieldId);
    uint64_t descriptorDataField = env->GetLongField(javaffxres, descriptorDataFieldId);
    int type = env->GetIntField(javaffxres, typeFieldId);
    int format = env->GetIntField(javaffxres, formatFieldId);
    int width = env->GetIntField(javaffxres, widthFieldId);
    int height = env->GetIntField(javaffxres, heightFieldId);
    int depth = env->GetIntField(javaffxres, depthFieldId);
    int mipCount = env->GetIntField(javaffxres, mipCountFieldId);
    int flags = env->GetIntField(javaffxres, flagsFieldId);
    int state = env->GetIntField(javaffxres, stateFieldId);

    FfxResource res = __ffxResourceJavaToCpp(resourceField, isDepth, descriptorDataField, type, format, width, height, depth, mipCount, flags, state);

    return res;
}

FfxResource __ffxResourceJavaToCpp(
    int resource, bool isDepth, uint64_t descriptorData,
    int type, int format, int width, int height, int depth,
    int mipCount, int flags, int state)
{
    FfxResource ffxresource = {};
    ffxresource.resource = reinterpret_cast<void *>(static_cast<uintptr_t>(resource));
    ffxresource.descriptorData = descriptorData;
    ffxresource.state = (FfxResourceStates)state;
    ffxresource.isDepth = isDepth;
    ffxresource.description.flags = (FfxResourceFlags)flags;
    ffxresource.description.type = (FfxResourceType)type;
    ffxresource.description.width = width;
    ffxresource.description.height = height;
    ffxresource.description.depth = depth;
    ffxresource.description.mipCount = mipCount;
    ffxresource.description.format = (FfxSurfaceFormat)format;
    return ffxresource;
}

PFN_vkVoidFunction java_getDeviceProcAddr(VkDevice device, const char *pName)
{
    return NULL;
};
void java_VkGetPhysicalDeviceMemoryProperties(VkPhysicalDevice device, VkPhysicalDeviceMemoryProperties *pMemoryProperties)
{
    return;
};
void java_VkGetPhysicalDeviceProperties2(VkPhysicalDevice physicalDevice, VkPhysicalDeviceProperties2 *pProperties)
{
    return;
};
void java_VkGetPhysicalDeviceFeatures2(VkPhysicalDevice physicalDevice, VkPhysicalDeviceFeatures2 *pFeatures)
{
    return;
};
VkResult java_VkEnumerateDeviceExtensionProperties(VkPhysicalDevice physicalDevice, const char *pLayerName, uint32_t *pPropertyCount, VkExtensionProperties *pProperties)
{
    return VK_SUCCESS;
};
void java_VkGetPhysicalDeviceProperties(VkPhysicalDevice physicalDevice, VkPhysicalDeviceProperties *pProperties)
{
    return;
};