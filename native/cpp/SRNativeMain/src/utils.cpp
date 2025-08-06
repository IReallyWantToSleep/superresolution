#include "utils.h"
#include <iostream>
#include <vector>
#include "define.h"

void java_log(JNIEnv *env, char *msg, int level)
{
    jclass cpp_helper = env->FindClass(JAVA_CPPHELPER_CLASS);
    jmethodID methodID = env->GetStaticMethodID(cpp_helper, "CPP_Log", "(Ljava/lang/String;I)V");
    jstring jmsg = env->NewStringUTF(msg);
    env->CallStaticVoidMethod(cpp_helper, methodID, jmsg, jint(level));
    env->DeleteLocalRef(jmsg);
}

bool ToCppBool(jboolean value)
{
    return value == JNI_TRUE;
}
