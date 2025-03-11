package io.homo.superresolution.common.render;

import io.homo.superresolution.common.mixin.core.accessor.LevelRendererAccessor;
import io.homo.superresolution.common.render.impl.framebuffer.MinecraftRenderTarget;
import net.minecraft.client.renderer.LevelRenderer;

import java.util.function.Function;

public enum RenderTargetType {
    ENTITY((levelRenderer -> (MinecraftRenderTarget) (((LevelRendererAccessor) levelRenderer).getEntityRenderTarget()))),
    TRANSLUCENT((levelRenderer -> (MinecraftRenderTarget) levelRenderer.getTranslucentTarget())),
    ITEM_ENTITY((levelRenderer -> (MinecraftRenderTarget) levelRenderer.getItemEntityTarget())),
    PARTICLES((levelRenderer -> (MinecraftRenderTarget) levelRenderer.getParticlesTarget())),
    WEATHER((levelRenderer -> (MinecraftRenderTarget) levelRenderer.getWeatherTarget())),
    CLOUDS((levelRenderer -> (MinecraftRenderTarget) levelRenderer.getCloudsTarget())),
    HAND((levelRenderer) -> HandRenderTarget.getHandRenderTarget());

    private final Function<LevelRenderer, MinecraftRenderTarget> callback;

    RenderTargetType(Function<LevelRenderer, MinecraftRenderTarget> callback) {
        this.callback = callback;
    }

    public MinecraftRenderTarget get(LevelRenderer levelRenderer) {
        return callback.apply(levelRenderer);
    }
}
