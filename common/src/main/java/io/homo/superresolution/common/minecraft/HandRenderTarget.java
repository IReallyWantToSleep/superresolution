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

import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

public class HandRenderTarget {
    public static IBindableFrameBuffer handRenderTarget;

    public static IBindableFrameBuffer getHandRenderTarget() {
        if (handRenderTarget == null) {
            handRenderTarget = (IBindableFrameBuffer) RenderSystems.current().device().createFramebuffer(
                    FramebufferDescription.create()
                            .colorFormat(TextureFormat.RGBA8)
                            .depthFormat(TextureFormat.DEPTH24)
                            .size(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight())
                            .build()
            );
            handRenderTarget.setClearColorRGBA(0, 0, 0, 0);
            handRenderTarget.clearFrameBuffer();
        }
        return handRenderTarget;
    }
}
