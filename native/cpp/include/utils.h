
#include "JNI0.h"

void java_log(const char* msg,int level);
void set_env(JNIEnv * env);
JNIEnv* get_env();
void check_env(JNIEnv *env);
bool ToCppBool(jboolean value);

