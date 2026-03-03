#include "sr/sr_api.h"
#ifdef __cplusplus
extern "C" {
    #endif
    SR_API SRUpscaleContextCallbacks srGetXeSSUpscaleCallbacks();

    SR_API SRReturnCode srXeSSLoadFunctionsFromDll(const char *dllPath, SRMessageCallback messageCallback);
    #ifdef __cplusplus
}
#endif