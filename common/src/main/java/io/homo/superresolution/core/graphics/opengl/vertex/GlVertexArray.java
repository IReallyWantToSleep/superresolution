package io.homo.superresolution.core.graphics.opengl.vertex;


import io.homo.superresolution.core.graphics.impl.vertex.IVertexArray;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttribute;
import org.lwjgl.opengl.GL45;

public class GlVertexArray implements IVertexArray {
    private final int id;

    public GlVertexArray() {
        this.id = GL45.glCreateVertexArrays();
    }

    @Override
    public void destroy() {
        GL45.glDeleteVertexArrays(id);
    }

    @Override
    public void setAttributes(VertexAttribute[] attributes, IVertexBuffer vertexBuffer) {
        int bindingIndex = 0;
        GL45.glVertexArrayVertexBuffer(id, bindingIndex, vertexBuffer.handle(), 0, attributes[0].getStride());

        for (VertexAttribute attr : attributes) {
            int loc = attr.getLocation();
            GL45.glVertexArrayAttribBinding(id, loc, bindingIndex);
            switch (attr.getDataType()) {
                case FLOAT ->
                        GL45.glVertexArrayAttribFormat(id, loc, attr.getComponentCount(), GL45.GL_FLOAT, false, attr.getOffset());
                case INTEGER ->
                        GL45.glVertexArrayAttribIFormat(id, loc, attr.getComponentCount(), GL45.GL_INT, attr.getOffset());
            }
            GL45.glEnableVertexArrayAttrib(id, loc);
        }
    }

    @Override
    public int handle() {
        return id;
    }
}