package io.homo.superresolution.core.graphics.opengl.pipeline.jobs;

import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttribute;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;

public class GlPipelineGraphicsJob extends GlPipelineJob {
    public IFrameBuffer targetFrameBuffer;
    public GlShaderProgram program;

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
            IRenderSystem rs = RenderSystems.current();
            rs.setShaderProgram(program);
            float[] vertices = {
                    -1f, -1f, 0f, 0f,
                    1f, -1f, 1f, 0f,
                    1f, 1f, 1f, 1f,

                    -1f, -1f, 0f, 0f,
                    1f, 1f, 1f, 1f,
                    -1f, 1f, 0f, 1f
            };
            VertexBufferDescription desc = new VertexBufferDescription(vertices.length * Float.BYTES, false);
            IVertexBuffer vbo = rs.createVertexBuffer(desc);
            rs.uploadVertexData(vbo, vertices, 0, vertices.length);
            VertexAttribute[] attributes = new VertexAttribute[]{
                    new VertexAttribute(0, 2, VertexAttribute.DataType.FLOAT, 4 * Float.BYTES, 0),
                    new VertexAttribute(1, 2, VertexAttribute.DataType.FLOAT, 4 * Float.BYTES, 2 * Float.BYTES)
            };
            rs.setVertexAttributes(attributes);
            rs.draw(PrimitiveType.TRIANGLES, vbo, 0, 6);
            rs.destroyVertexBuffer(vbo);
        }
    }
}