package io.homo.superresolution.core.graphics.opengl.utils;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.shader.GlBlitShader;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexBuffer;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexArray;
import org.lwjgl.opengl.GL45;

import static org.lwjgl.opengl.GL30.*;

public class GlBlitRenderer {
    public static void blitToScreen(ITexture textureId, int viewWidth, int viewHeight) {
        glColorMask(true, true, true, false);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glViewport(0, 0, viewWidth, viewHeight);
        GlShaderProgram blitShader = GlBlitShader.getShader();
        Gl.glUseProgram(blitShader.handle);
        blitShader.uniforms().samplerTexture("uTexture").set(
                textureId
        );
        try (GlVertexArray vaoObj = new GlVertexArray();
             GlVertexBuffer vbo = new GlVertexBuffer()) {
            float[] vertices = {
                    -1f, -1f, 0f, 0f,
                    1f, -1f, 1f, 0f,
                    1f, 1f, 1f, 1f,
                    -1f, 1f, 0f, 1f
            };
            int vao = vaoObj.id();
            Gl.DSA.vertexArrayVertexBuffer(
                    vao,
                    0,
                    vbo.getId(),
                    0,
                    4 * Float.BYTES
            );

            Gl.DSA.vertexArrayAttribFormat(
                    vao,
                    0,
                    2,
                    GL_FLOAT,
                    false,
                    0
            );
            Gl.DSA.enableVertexArrayAttrib(vao, 0);
            Gl.DSA.vertexArrayAttribBinding(vao, 0, 0);
            Gl.DSA.vertexArrayAttribFormat(
                    vao,
                    1,
                    2,
                    GL_FLOAT,
                    false,
                    2 * Float.BYTES
            );
            Gl.DSA.enableVertexArrayAttrib(vao, 1);
            Gl.DSA.vertexArrayAttribBinding(vao, 1, 0);

            vbo.uploadData(vertices, GL_STATIC_DRAW);

            Gl.DSA.bindVertexArray(vao);
            GL45.glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        }
        glDepthMask(true);
        glColorMask(true, true, true, true);
    }
}
