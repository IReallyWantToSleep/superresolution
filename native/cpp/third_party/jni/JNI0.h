#ifdef ON_LINUX64
    #include "jni_linux64.h"
#elif defined(ON_WIN64)
    #include "jni_win64.h"
#elif defined(ON_ANDROID)
    #include <jni.h>
#elif defined(ON_MACOS)
    #include "jni_macarm64.h"
#else
#include "jni_win64.h"
#endif

#define JAVA_TYPE_BOOL "Z"
#define JAVA_TYPE_INT "I"
#define JAVA_TYPE_LONG "J"
#define JAVA_TYPE_FLOAT "F"
#define JAVA_TYPE_CHAR "C"
#define JAVA_TYPE_BYTE "B"
#define JAVA_TYPE_SHORT "S"
#define JAVA_TYPE_DOUBLE "D"

#define JAVA_TYPE_BOOL_ARR "[Z"
#define JAVA_TYPE_INT_ARR "[I"
#define JAVA_TYPE_LONG_ARR "[J"
#define JAVA_TYPE_FLOAT_ARR "[F"
#define JAVA_TYPE_CHAR_ARR "[C"
#define JAVA_TYPE_BYTE_ARR "[B"
#define JAVA_TYPE_SHORT_ARR "[S"
#define JAVA_TYPE_DOUBLE_ARR "[D"




