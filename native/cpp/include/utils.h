#ifdef ON_LINUX
    #include "jni_linux64.h"
#else
    #include "jni.h"
#endif
#include "glfw3.h"
#include "vulkan/vulkan.h"

void java_log(const char* msg,int level);
void set_env(JNIEnv * env);
JNIEnv* get_env();
void check_env(JNIEnv *env);
GLFWglproc java_glfwGetProcAddress(const char* name);
bool ToCppBool(jboolean value);

