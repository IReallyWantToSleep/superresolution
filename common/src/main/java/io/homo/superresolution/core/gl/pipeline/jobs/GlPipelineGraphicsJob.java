package io.homo.superresolution.core.gl.pipeline.jobs;

import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.core.gl.vertex.GlVertexArray;
import io.homo.superresolution.core.gl.vertex.GlVertexBuffer;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class GlPipelineGraphicsJob extends GlPipelineJob {
    public IFrameBuffer targetFrameBuffer;
    public AbstractGlShaderProgram program;

    public void setTargetFrameBuffer(IFrameBuffer frameBuffer) {
        if (this.type != GlPipelineJobType.Graphics) {
            throw new IllegalStateException("Only Graphics jobs can set FrameBuffer targets!");
        }
        this.targetFrameBuffer = frameBuffer;
    }

    @Override
    public void schedule(GlPipelineJobDispatchResource dispatchResource) {
        setupResource();
    }

    @Override
    public void execute(GlPipelineJobDispatchResource dispatchResource) {
        try (GlState ignored = new GlState()) {
            if (targetFrameBuffer != null) {
                targetFrameBuffer.bind(FrameBufferBindPoint.WRITE, true);
            }
            program.use();
            try (GlVertexArray vao = new GlVertexArray();
                 GlVertexBuffer vbo = new GlVertexBuffer()) {
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
                glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
                glEnableVertexAttribArray(1);
                glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
            }
        }
    }

}
