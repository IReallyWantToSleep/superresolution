package io.homo.superresolution.common.upscale;

import io.homo.superresolution.common.render.gl.GlState;
import io.homo.superresolution.common.render.gl.shader.BlitShader;
import io.homo.superresolution.common.render.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.common.render.gl.vertex.VertexArray;
import io.homo.superresolution.common.render.gl.vertex.VertexBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.utils.FileReadHelper;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glColorMask;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class MotionVectorsGenerator {
    private static final GlGeneralShaderProgram shaderProgram =
            GlGeneralShaderProgram.create()
                    .addAllFragShaderTextList(FileReadHelper.readText("/shader/calc_motion_vector.frag.glsl"))
                    .addAllVertShaderTextList(FileReadHelper.readText("/shader/calc_motion_vector.vert.glsl"))
                    .setShaderName("motion_vector")
                    .build();

    public static void update(DispatchResource dispatchResource, IFrameBuffer target) {
        try (GlState ignored = new GlState()) {
            if (!shaderProgram.compiled) shaderProgram.compileShader();
            shaderProgram.use();
            shaderProgram.setMatrix4(
                    "u_LookAt",
                    dispatchResource.viewMatrix()
            );
            shaderProgram.setMatrix4(
                    "u_LookAtLast",
                    dispatchResource.lastViewMatrix()
            );
            target.bind(FrameBufferBindPoint.WRITE);
            glColorMask(true, true, true, false);
            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            glViewport(0, 0, dispatchResource.renderWidth(), dispatchResource.renderHeight());
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
            shaderProgram.clear();
            glDepthMask(true);
            glColorMask(true, true, true, true);
        }
    }
}
