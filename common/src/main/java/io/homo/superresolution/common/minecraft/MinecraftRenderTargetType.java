package io.homo.superresolution.common.minecraft;

import io.homo.superresolution.common.mixin.core.accessor.LevelRendererAccessor;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import net.minecraft.client.renderer.LevelRenderer;

import java.util.function.Function;

public enum MinecraftRenderTargetType {
    ENTITY((levelRenderer -> MinecraftRenderTargetWrapper.of(((LevelRendererAccessor) levelRenderer).getEntityRenderTarget()))),
    TRANSLUCENT((levelRenderer -> MinecraftRenderTargetWrapper.of(levelRenderer.getTranslucentTarget()))),
    ITEM_ENTITY((levelRenderer -> MinecraftRenderTargetWrapper.of(levelRenderer.getItemEntityTarget()))),
    PARTICLES((levelRenderer -> MinecraftRenderTargetWrapper.of(levelRenderer.getParticlesTarget()))),
    WEATHER((levelRenderer -> MinecraftRenderTargetWrapper.of(levelRenderer.getWeatherTarget()))),
    CLOUDS((levelRenderer -> MinecraftRenderTargetWrapper.of(levelRenderer.getCloudsTarget()))),
    HAND((levelRenderer) -> HandRenderTarget.getHandRenderTarget());

    private final Function<LevelRenderer, IBindableFrameBuffer> callback;

    MinecraftRenderTargetType(Function<LevelRenderer, IBindableFrameBuffer> callback) {
        this.callback = callback;
    }

    public IBindableFrameBuffer get(LevelRenderer levelRenderer) {
        return callback.apply(levelRenderer);
    }
}
