package io.homo.superresolution.common.workmode;

public final class HackWorkModeBootstrap {
    private HackWorkModeBootstrap() {
    }

    public static void register() {
        SRWorkModeManager.register(new HackSRWorkModeProvider());
    }
}
