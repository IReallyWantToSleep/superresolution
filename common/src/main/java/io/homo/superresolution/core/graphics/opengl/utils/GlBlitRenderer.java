/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.opengl.utils;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.shader.GlBlitShader;
import io.homo.superresolution.core.graphics.system.IRenderState;

public class GlBlitRenderer {
    private static DrawObject fullscreenQuad;

    public static void blitToScreen(ITexture textureId, int viewWidth, int viewHeight) {
        try (GlState state = new GlState(
                GlState.STATE_ALL
        )) {
            if (fullscreenQuad == null) {
                fullscreenQuad = DrawObject.fullscreenQuad(RenderSystems.current().device());
            }
            var blitShader = GlBlitShader.getShader();
            blitShader.uniforms().samplerTexture("uTexture").set(
                    textureId
            );
            IRenderState.StateSnapshot stateSnapshot = RenderSystems.opengl().device().commandEncoder().renderState().get();

            RenderSystems.opengl().device().commandEncoder()
                    .begin()
                    .renderState()
                    .colorMask(true, true, true, false)
                    .depthTest(false)
                    .depthWrite(false)
                    .cullFace(false)
                    .viewport(0, 0, viewWidth, viewHeight);
            RenderSystems.opengl().device().commandEncoder()
                    .draw(
                            blitShader,
                            null,
                            DrawObject.fullscreenQuad(RenderSystems.opengl().device()).once(),
                            0,
                            DrawObject.fullscreenQuadVertexCount()
                    );
            RenderSystems.opengl().device().commandEncoder()
                    .renderState()
                    .apply(stateSnapshot);
            RenderSystems.opengl().device().submitCommandBuffer(RenderSystems.opengl().device().commandEncoder().end());
        }
    }
}
