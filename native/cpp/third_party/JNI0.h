#ifdef ON_LINUX64
    #include "jni_linux64.h"
#elif defined(ON_WIN64)
    #include "jni_win64.h"
#elif defined(ON_ANDROID)
    #include <jni.h>
#else
#include "jni_win64.h"
#endif