#include "utils.h"
#include <iostream>
#include <mutex>
#include "define.h"

namespace {
    std::mutex g_javaBridgeMutex;
    JavaVM *g_javaVm = nullptr;
    jclass g_cppHelperClass = nullptr;
    jmethodID g_cppLogMethodId = nullptr;
    jmethodID g_cppGlfwGetProcAddressMethodId = nullptr;
    jmethodID g_cppVkGetDeviceProcAddrMethodId = nullptr;

    bool ensureJavaBridgeLocked(JNIEnv *env) {
        if (!env) {
            return g_javaVm && g_cppHelperClass && g_cppLogMethodId &&
                   g_cppGlfwGetProcAddressMethodId && g_cppVkGetDeviceProcAddrMethodId;
        }

        if (!g_javaVm && env->GetJavaVM(&g_javaVm) != JNI_OK) {
            g_javaVm = nullptr;
            return false;
        }

        if (!g_cppHelperClass) {
            jclass localClass = env->FindClass(JAVA_CPPHELPER_CLASS);
            if (!localClass) {
                if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                }
                return false;
            }
            g_cppHelperClass = reinterpret_cast<jclass>(env->NewGlobalRef(localClass));
            env->DeleteLocalRef(localClass);
            if (!g_cppHelperClass) {
                return false;
            }
        }

        if (!g_cppLogMethodId) {
            g_cppLogMethodId = env->GetStaticMethodID(g_cppHelperClass, "CPP_Log", "(Ljava/lang/String;I)V");
            if (!g_cppLogMethodId) {
                if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                }
                return false;
            }
        }

        if (!g_cppGlfwGetProcAddressMethodId) {
            g_cppGlfwGetProcAddressMethodId = env->GetStaticMethodID(
                g_cppHelperClass,
                "CPP_glfwGetProcAddress",
                "(Ljava/lang/String;)J"
            );
            if (!g_cppGlfwGetProcAddressMethodId) {
                if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                }
                return false;
            }
        }

        if (!g_cppVkGetDeviceProcAddrMethodId) {
            g_cppVkGetDeviceProcAddrMethodId = env->GetStaticMethodID(
                g_cppHelperClass,
                "CPP_vkGetDeviceProcAddr",
                "(Ljava/lang/String;)J"
            );
            if (!g_cppVkGetDeviceProcAddrMethodId) {
                if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                }
                return false;
            }
        }

        return true;
    }

    bool copyJavaBridgeState(
        JavaVM **javaVm,
        jclass *helperClass,
        jmethodID *logMethodId,
        jmethodID *glfwGetProcAddressMethodId,
        jmethodID *vkGetDeviceProcAddrMethodId
    ) {
        std::lock_guard<std::mutex> lock(g_javaBridgeMutex);
        if (!ensureJavaBridgeLocked(nullptr)) {
            return false;
        }
        if (javaVm) {
            *javaVm = g_javaVm;
        }
        if (helperClass) {
            *helperClass = g_cppHelperClass;
        }
        if (logMethodId) {
            *logMethodId = g_cppLogMethodId;
        }
        if (glfwGetProcAddressMethodId) {
            *glfwGetProcAddressMethodId = g_cppGlfwGetProcAddressMethodId;
        }
        if (vkGetDeviceProcAddrMethodId) {
            *vkGetDeviceProcAddrMethodId = g_cppVkGetDeviceProcAddrMethodId;
        }
        return true;
    }

    JNIEnv *getEnvForCurrentThread(JavaVM *javaVm, bool *attached) {
        if (attached) {
            *attached = false;
        }
        if (!javaVm) {
            return nullptr;
        }

        JNIEnv *env = nullptr;
        jint status = javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8);
        if (status == JNI_OK) {
            return env;
        }
        if (status != JNI_EDETACHED) {
            return nullptr;
        }
        if (javaVm->AttachCurrentThread(reinterpret_cast<void **>(&env), nullptr) != JNI_OK) {
            return nullptr;
        }
        if (attached) {
            *attached = true;
        }
        return env;
    }

    void releaseEnvForCurrentThread(JavaVM *javaVm, bool attached) {
        if (attached && javaVm) {
            javaVm->DetachCurrentThread();
        }
    }

    jlong callCppHelperLongMethod(jmethodID methodId, const char *name) {
        if (!name || !methodId) {
            return 0;
        }

        JavaVM *javaVm = nullptr;
        jclass helperClass = nullptr;
        if (!copyJavaBridgeState(&javaVm, &helperClass, nullptr, nullptr, nullptr)) {
            return 0;
        }

        bool attached = false;
        JNIEnv *env = getEnvForCurrentThread(javaVm, &attached);
        if (!env) {
            return 0;
        }

        jstring jmsg = env->NewStringUTF(name);
        if (!jmsg) {
            releaseEnvForCurrentThread(javaVm, attached);
            return 0;
        }

        jlong value = env->CallStaticLongMethod(helperClass, methodId, jmsg);
        env->DeleteLocalRef(jmsg);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            value = 0;
        }

        releaseEnvForCurrentThread(javaVm, attached);
        return value;
    }
}

bool init_java_bridge(JNIEnv *env) {
    std::lock_guard<std::mutex> lock(g_javaBridgeMutex);
    return ensureJavaBridgeLocked(env);
}

void java_log(const char *msg, int level) {
    if (!msg) {
        return;
    }

    JavaVM *javaVm = nullptr;
    jclass helperClass = nullptr;
    jmethodID methodId = nullptr;
    if (!copyJavaBridgeState(&javaVm, &helperClass, &methodId, nullptr, nullptr)) {
        return;
    }

    bool attached = false;
    JNIEnv *env = getEnvForCurrentThread(javaVm, &attached);
    if (!env) {
        return;
    }

    jstring jmsg = env->NewStringUTF(msg);
    if (!jmsg) {
        releaseEnvForCurrentThread(javaVm, attached);
        return;
    }

    env->CallStaticVoidMethod(helperClass, methodId, jmsg, jint(level));
    env->DeleteLocalRef(jmsg);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }

    releaseEnvForCurrentThread(javaVm, attached);
}

jlong java_call_cpp_glfw_get_proc_address(const char *name) {
    JavaVM *javaVm = nullptr;
    jclass helperClass = nullptr;
    jmethodID methodId = nullptr;
    if (!copyJavaBridgeState(&javaVm, &helperClass, nullptr, &methodId, nullptr)) {
        return 0;
    }
    return callCppHelperLongMethod(methodId, name);
}

jlong java_call_cpp_vk_get_device_proc_address(const char *name) {
    JavaVM *javaVm = nullptr;
    jclass helperClass = nullptr;
    jmethodID methodId = nullptr;
    if (!copyJavaBridgeState(&javaVm, &helperClass, nullptr, nullptr, &methodId)) {
        return 0;
    }
    return callCppHelperLongMethod(methodId, name);
}

bool ToCppBool(jboolean value) {
    return value == JNI_TRUE;
}
