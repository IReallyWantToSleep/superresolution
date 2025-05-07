#ifdef ON_LINUX
    #include "jni_linux64.h"
#else
    #include "jni.h"
#endif
#include "glfw3.h"
#include "ffx-fsr2-api/ffx_types.h"
#include "vulkan/vulkan.h"

void java_log(const char* msg,int level);
void set_env(JNIEnv * env);
JNIEnv* get_env();
void check_env(JNIEnv *env);
GLFWglproc java_glfwGetProcAddress(const char* name);
bool ToCppBool(jboolean value);
FfxResource ffxResourceJavaToCpp(JNIEnv* env, jobject javaffxres);
FfxResource __ffxResourceJavaToCpp(
    int resource, bool isDepth, uint64_t descriptorData,
    int type, int format, int width, int height, int depth,
    int mipCount, int flags, int state
);
PFN_vkVoidFunction java_getDeviceProcAddr(VkDevice device, const char *pName);
void java_VkGetPhysicalDeviceMemoryProperties(VkPhysicalDevice device, VkPhysicalDeviceMemoryProperties* pMemoryProperties);
void java_VkGetPhysicalDeviceProperties2(VkPhysicalDevice physicalDevice, VkPhysicalDeviceProperties2* pProperties);
void java_VkGetPhysicalDeviceFeatures2(VkPhysicalDevice physicalDevice, VkPhysicalDeviceFeatures2* pFeatures);
VkResult java_VkEnumerateDeviceExtensionProperties(VkPhysicalDevice physicalDevice, const char* pLayerName, uint32_t* pPropertyCount, VkExtensionProperties* pProperties);
void java_VkGetPhysicalDeviceProperties(VkPhysicalDevice physicalDevice, VkPhysicalDeviceProperties* pProperties);


