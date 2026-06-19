package io.homo.superresolution.shadercompat;

import io.homo.superresolution.common.workmode.SRWorkModeManager;

public final class ShaderCompatBootstrap {
    private ShaderCompatBootstrap() {
    }

    public static void register() {
        SRWorkModeManager.register(new ShaderCompatSRWorkModeProvider());
    }
}
