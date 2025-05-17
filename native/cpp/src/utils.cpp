#include "utils.h"
#include <iostream>
#include <vector>
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
