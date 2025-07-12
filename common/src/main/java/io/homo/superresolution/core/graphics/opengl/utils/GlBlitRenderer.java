package io.homo.superresolution.core.graphics.opengl.utils;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.shader.GlBlitShader;

public class GlBlitRenderer {
    private static DrawObject fullscreenQuad;

    public static void blitToScreen(ITexture textureId, int viewWidth, int viewHeight) {
        try (GlState state = new GlState(
                GlState.STATE_ALL
        )) {
            if (fullscreenQuad == null) {
                fullscreenQuad = DrawObject.fullscreenQuad(RenderSystems.current());
            }
            RenderSystems.current().renderState().save()
                    .colorMask(true, true, true, false)
                    .depthTest(false)
                    .depthWrite(false)
                    .cullFace(false)
                    .viewport(0, 0, viewWidth, viewHeight);
            var blitShader = GlBlitShader.getShader();
            blitShader.uniforms().samplerTexture("uTexture").set(
                    textureId
            );
            RenderSystems.current().draw(
                    blitShader,
                    null,
                    DrawObject.fullscreenQuad(RenderSystems.current()).once(),
                    0,
                    DrawObject.fullscreenQuadVertexCount()
            );
            RenderSystems.current().renderState().restore();
        }
    }
}
