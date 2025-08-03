package io.homo.superresolution.core.graphics.opengl.utils;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
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
            IRenderState.StateSnapshot stateSnapshot = RenderSystems.opengl().device().commendEncoder().renderState().get();

            RenderSystems.opengl().device().commendEncoder()
                    .begin()
                    .renderState()
                    .colorMask(true, true, true, false)
                    .depthTest(false)
                    .depthWrite(false)
                    .cullFace(false)
                    .viewport(0, 0, viewWidth, viewHeight);
            RenderSystems.opengl().device().commendEncoder()
                    .draw(
                            blitShader,
                            null,
                            DrawObject.fullscreenQuad(RenderSystems.opengl().device()).once(),
                            0,
                            DrawObject.fullscreenQuadVertexCount()
                    );
            RenderSystems.opengl().device().commendEncoder()
                    .renderState()
                    .apply(stateSnapshot);
            RenderSystems.opengl().device().submitCommandBuffer(RenderSystems.opengl().device().commendEncoder().end());
        }
    }
}
