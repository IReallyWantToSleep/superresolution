package io.homo.superresolution.common.workmode;

import io.homo.superresolution.common.minecraft.handler.IMinecraftRenderHandler;
import io.homo.superresolution.common.minecraft.handler.MinecraftRenderHandler;

public class HackSRWorkModeProvider implements SRWorkModeProvider {
    @Override
    public String id() {
        return SRWorkModeManager.HACK;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public IMinecraftRenderHandler createRenderHandler() {
        return new MinecraftRenderHandler();
    }

    @Override
    public SRWorkModeState getState() {
        return SRWorkModeState.defaults();
    }
}
