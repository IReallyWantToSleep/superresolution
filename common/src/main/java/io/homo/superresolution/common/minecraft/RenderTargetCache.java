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

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;

import java.util.*;

public class RenderTargetCache {
    private static final Map<IBindableFrameBuffer, FrameBufferRenderTargetAdapter> cached = new HashMap<>();

    public static RenderTarget cacheOf(IBindableFrameBuffer frameBuffer) {
        if (cached.get(frameBuffer) == null) {
            FrameBufferRenderTargetAdapter renderTarget = FrameBufferRenderTargetAdapter.ofRenderTarget(frameBuffer);
            cached.put(frameBuffer, renderTarget);
            return renderTarget;
        } else {
            return cached.get(frameBuffer).bindFrameBuffer(frameBuffer);
        }
    }
}
