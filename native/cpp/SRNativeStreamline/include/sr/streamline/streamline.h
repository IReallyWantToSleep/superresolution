#pragma once

#include "sr/sr_api.h"

#ifdef __cplusplus
extern "C" {
#endif

SR_API SRReturnCode srStreamlineInit(const char *pluginPath, const char *logPath, SRMessageCallback messageCallback);
SR_API SRReturnCode srStreamlineShutdown();
SR_API SRReturnCode srStreamlineIsDLSSGSupported(bool *outSupported);
SR_API SRReturnCode srStreamlineDLSSGSetOptions(bool enabled, uint32_t framesToGenerate);
SR_API SRReturnCode srStreamlineDLSSGGetState(uint64_t *outEstimatedVram, uint32_t *outStatus);
SR_API SRUpscaleContextCallbacks srGetStreamlineDLSSUpscaleCallbacks();

#ifdef __cplusplus
}
#endif
