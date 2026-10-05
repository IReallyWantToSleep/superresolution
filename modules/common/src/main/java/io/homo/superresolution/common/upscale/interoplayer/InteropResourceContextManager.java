package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.interop.InteropResourceContext;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.workmode.SRWorkModeProvider;

/** The active work mode is the sole owner of its interop format context. */
public final class InteropResourceContextManager {
    private InteropResourceContextManager() {
    }

    public static InteropResourceContext getCurrentContext() {
        SRWorkModeProvider provider = SRWorkModeManager.getCurrentProvider();
        return provider == null ? InteropResourceContext.empty() : provider.getInteropResourceContext();
    }
}
