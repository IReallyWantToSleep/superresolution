/*package io.homo.superresolution.common.render.gl.utils;

import io.homo.superresolution.common.render.gl.GlState;
import io.homo.superresolution.common.render.gl.shader.BlitShader;
import io.homo.superresolution.common.render.gl.vertex.VertexBuffer;
import io.homo.superresolution.common.render.gl.vertex.VertexArray;

import static org.lwjgl.opengl.GL30.*;

public class BlitRenderer {
    public static void blitToScreen(int textureId, int viewWidth, int viewHeight) {
        try (GlState ignored = new GlState()) {
            glViewport(0, 0, viewWidth, viewHeight);
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
        }
    }
}*/

package io.homo.superresolution.core.gl.utils;

import io.homo.superresolution.core.gl.shader.BlitShader;
import io.homo.superresolution.core.gl.vertex.VertexBuffer;
import io.homo.superresolution.core.gl.vertex.VertexArray;

import static org.lwjgl.opengl.GL30.*;

public class BlitRenderer {
    public static void blitToScreen(int textureId, int viewWidth, int viewHeight) {
        glColorMask(true, true, true, false);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glViewport(0, 0, viewWidth, viewHeight);
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
        glDepthMask(true);
        glColorMask(true, true, true, true);
    }
}
