#include "vulkan/vulkan.h"
#include "JNI0.h"
#include "sr_api.h"

SRTextureResource fromJavaSRVkTextureResource(JNIEnv *env, jobject obj);
jobject toJavaSRVkTextureResource(JNIEnv *env, SRTextureResource resource);