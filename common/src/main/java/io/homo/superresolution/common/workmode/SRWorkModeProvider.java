package io.homo.superresolution.common.workmode;

import io.homo.superresolution.common.debug.imgui.ImGuiDebugContext;
import io.homo.superresolution.common.minecraft.handler.IMinecraftRenderHandler;

public interface SRWorkModeProvider {
    String id();

    boolean isActive();

    IMinecraftRenderHandler createRenderHandler();

    SRWorkModeState getState();

    default void onClientSetup() {
    }

    default void reloadShaderPack() {
    }

    default void renderImGuiDebug(ImGuiDebugContext ctx) {
    }

    default void collectDebugTextures(ImGuiDebugContext ctx) {
    }
}
