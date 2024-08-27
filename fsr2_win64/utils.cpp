#include "pch.h"
#include "utils.h"

JNIEnv* cur_env;
void set_env(JNIEnv* env) {
    cur_env = env;
}

JNIEnv* get_env()
{
    return cur_env;
}

void java_log(const char* msg, int level) {
    jclass cpp_helper = cur_env->FindClass("io/homo/superresolution/fsr2/CPPHelper");
    jmethodID methodID = cur_env->GetStaticMethodID(cpp_helper, "CPP_Log", "(Ljava/lang/String;I)V");
    if (methodID) {
        jstring jmsg = cur_env->NewStringUTF(msg);
        if (jmsg) {
            cur_env->CallStaticVoidMethod(cpp_helper, methodID, jmsg,jint(level));
            cur_env->DeleteLocalRef(jmsg);
        }
    }
}

GLFWglproc java_glfwGetProcAddress(const char* name)
{
    jclass cpp_helper = cur_env->FindClass("io/homo/superresolution/fsr2/CPPHelper");
    jmethodID methodID = cur_env->GetStaticMethodID(cpp_helper, "CPP_glfwGetProcAddress", "(Ljava/lang/String;)J");
    if (methodID) {
        jstring jmsg = cur_env->NewStringUTF(name);
        jlong jlongValue = cur_env->CallStaticLongMethod(cpp_helper, methodID, jmsg);
        GLFWglproc glfwProc = reinterpret_cast<GLFWglproc>(jlongValue);
        cur_env->DeleteLocalRef(jmsg);
        return glfwProc;
    }
    return 0;
}
