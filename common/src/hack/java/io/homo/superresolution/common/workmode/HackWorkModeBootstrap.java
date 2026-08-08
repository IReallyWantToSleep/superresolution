package io.homo.superresolution.common.workmode;

import io.homo.superresolution.common.config.SuperResolutionConfig;

public final class HackWorkModeBootstrap {
    private HackWorkModeBootstrap() {
    }

    public static void register() {
        if (SuperResolutionConfig.isUnstableIncompatibleShaderSupportEnabledAtStartup()) {
            SRWorkModeManager.register(new HackSRWorkModeProvider());
        }
    }
}
