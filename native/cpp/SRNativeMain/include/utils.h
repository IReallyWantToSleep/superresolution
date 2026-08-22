#pragma once

#include "jni/jni.h"

bool init_java_bridge(JNIEnv *env);
void java_log(const char *msg, int level);
jlong java_call_cpp_glfw_get_proc_address(const char *name);
jlong java_call_cpp_vk_get_device_proc_address(const char *name);

bool ToCppBool(jboolean value);
