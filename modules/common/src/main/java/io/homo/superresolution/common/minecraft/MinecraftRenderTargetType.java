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

import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import net.minecraft.client.renderer.LevelRenderer;

import java.util.function.Function;

public enum MinecraftRenderTargetType {
    #if MC_VER < MC_26_2
    ENTITY((levelRenderer -> MinecraftRenderTargetWrapper.of(((io.homo.superresolution.common.mixin.core.accessor.LevelRendererAccessor) levelRenderer).getEntityRenderTarget()))),
    #endif
    HAND((levelRenderer) -> HandRenderTarget.getHandRenderTarget());

    private final Function<LevelRenderer, IBindableFrameBuffer> callback;

    MinecraftRenderTargetType(Function<LevelRenderer, IBindableFrameBuffer> callback) {
        this.callback = callback;
    }

    public IBindableFrameBuffer get(LevelRenderer levelRenderer) {
        return callback.apply(levelRenderer);
    }
}
