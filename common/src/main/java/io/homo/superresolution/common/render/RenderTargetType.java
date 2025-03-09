package io.homo.superresolution.common.render;

import io.homo.superresolution.common.mixin.core.accessor.LevelRendererAccessor;
import net.minecraft.client.renderer.LevelRenderer;

import java.util.function.Function;

public enum RenderTargetType {
    #if MC_VER > MC_1_21_1
    ENTITY((levelRenderer -> new McRenderTargetWrapper(((LevelRendererAccessor) levelRenderer).getEntityRenderTarget()))),
    #else
    ENTITY((levelRenderer -> new McRenderTargetWrapper(levelRenderer.entityTarget()))),
    #endif
    TRANSLUCENT((levelRenderer -> new McRenderTargetWrapper(levelRenderer.getTranslucentTarget()))),
    ITEM_ENTITY((levelRenderer -> new McRenderTargetWrapper(levelRenderer.getItemEntityTarget()))),
    PARTICLES((levelRenderer -> new McRenderTargetWrapper(levelRenderer.getParticlesTarget()))),
    WEATHER((levelRenderer -> new McRenderTargetWrapper(levelRenderer.getWeatherTarget()))),
    CLOUDS((levelRenderer -> new McRenderTargetWrapper(levelRenderer.getCloudsTarget()))),
    HAND((levelRenderer) -> HandRenderTarget.getHandRenderTarget());

    private final Function<LevelRenderer, McRenderTargetWrapper> callback;

    RenderTargetType(Function<LevelRenderer, McRenderTargetWrapper> callback) {
        this.callback = callback;
    }

    public McRenderTargetWrapper get(LevelRenderer levelRenderer) {
        return callback.apply(levelRenderer);
    }
}
