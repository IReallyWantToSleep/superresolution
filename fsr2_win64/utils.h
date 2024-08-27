void java_log(const char* msg,int level);
void set_env(JNIEnv * env);
JNIEnv* get_env();
GLFWglproc java_glfwGetProcAddress(const char* name);