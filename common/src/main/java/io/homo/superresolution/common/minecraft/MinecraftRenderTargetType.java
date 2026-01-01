/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.minecraft;

import io.homo.superresolution.common.mixin.core.accessor.LevelRendererAccessor;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
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
