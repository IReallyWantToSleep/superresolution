package io.homo.superresolution.common.render.gl.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import io.homo.superresolution.common.render.gl.shader.BlitShader;
import io.homo.superresolution.common.render.gl.vertex.VertexBuffer;
import io.homo.superresolution.common.render.gl.vertex.VertexArray;

import static org.lwjgl.opengl.GL30.*;

public class BlitRenderer {
    public static void blitToScreen(int textureId, int viewWidth, int viewHeight) {
        GlStateManager._colorMask(true, true, true, false);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, viewWidth, viewHeight);
        BlitShader blitShader = BlitShader.getShader();
        blitShader.use();
        blitShader.bindTexture(textureId);
        try (VertexArray vao = new VertexArray();
             VertexBuffer vbo = new VertexBuffer()) {
            float[] vertices = {
                    -1f, -1f, 0f, 0f,
                    1f, -1f, 1f, 0f,
                    1f, 1f, 1f, 1f,
                    -1f, 1f, 0f, 1f
            };
            vao.bind();
            vbo.bind(GL_ARRAY_BUFFER);
            vbo.uploadData(vertices, GL_STATIC_DRAW);
            int stride = 4 * Float.BYTES;
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0); // 位置属性
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES); // 纹理坐标属性
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        }
        blitShader.clear();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
    }
}
