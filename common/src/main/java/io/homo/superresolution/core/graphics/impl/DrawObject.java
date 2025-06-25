package io.homo.superresolution.core.graphics.impl;


import io.homo.superresolution.core.graphics.impl.vertex.*;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexArray;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.impl.Destroyable;

public class DrawObject implements Destroyable, AutoCloseable {
    private final IVertexBuffer vertexBuffer;
    private final IVertexArray vertexArray;
    private final PrimitiveType primitiveType;

    public DrawObject(
            IVertexBuffer vertexBuffer,
            IVertexArray vertexArray,
            PrimitiveType primitiveType
    ) {
        this.vertexBuffer = vertexBuffer;
        this.vertexArray = vertexArray;
        this.primitiveType = primitiveType;
    }

    public static int fullscreenQuadVertexCount() {
        return 4;
    }

    public static DrawObject fullscreenQuad(IRenderSystem renderSystem) {
        float[] vertices = {
                -1f, 1f, 0f, 0f,
                1f, 1f, 1f, 0f,
                -1f, -1f, 0f, 1f,
                1f, -1f, 1f, 1f
        };
        VertexBufferDescription desc = new VertexBufferDescription(vertices.length * Float.BYTES, false);
        IVertexBuffer vbo = renderSystem.createVertexBuffer(desc);
        vbo.updateData(vertices, 0, vertices.length);
        VertexAttribute[] attributes = new VertexAttribute[]{
                new VertexAttribute(0, 2, VertexAttribute.DataType.FLOAT, 4 * Float.BYTES, 0),
                new VertexAttribute(1, 2, VertexAttribute.DataType.FLOAT, 4 * Float.BYTES, 2 * Float.BYTES)
        };
        IVertexArray vao = new GlVertexArray();
        vao.setAttributes(attributes, vbo);
        return new DrawObject(vbo, vao, PrimitiveType.TRIANGLE_STRIP);
    }

    public PrimitiveType getPrimitiveType() {
        return primitiveType;
    }

    public IVertexArray getVertexArray() {
        return vertexArray;
    }

    public IVertexBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    @Override
    public void destroy() {
        this.vertexBuffer.destroy();
        this.vertexArray.destroy();
    }

    @Override
    public void close() {
        this.destroy();
    }
}