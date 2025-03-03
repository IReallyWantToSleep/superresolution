package io.homo.superresolution.common.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;

import java.util.function.Function;

public enum RenderTargetType {
    ENTITY(LevelRenderer::entityTarget),
    TRANSLUCENT(LevelRenderer::getTranslucentTarget),
    ITEM_ENTITY(LevelRenderer::getItemEntityTarget),
    PARTICLES(LevelRenderer::getParticlesTarget),
    WEATHER(LevelRenderer::getWeatherTarget),
    CLOUDS(LevelRenderer::getCloudsTarget),
    HAND((levelRenderer) -> HandRenderTarget.getHandRenderTarget());

    private final Function<LevelRenderer, RenderTarget> callback;

    RenderTargetType(Function<LevelRenderer, RenderTarget> callback) {
        this.callback = callback;
    }

    public RenderTarget get(LevelRenderer levelRenderer) {
        return callback.apply(levelRenderer);
    }
}
