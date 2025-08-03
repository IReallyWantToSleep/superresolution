package io.homo.superresolution.core.graphics.opengl.vertex;


import io.homo.superresolution.core.graphics.impl.vertex.IVertexArray;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttribute;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import org.lwjgl.opengl.GL45;

import static org.lwjgl.opengl.GL41.*;


public class GlVertexArray implements IVertexArray {
    private final int id;

    public GlVertexArray() {
        this.id = Gl.DSA.createVertexArray();
    }

    @Override
    public void destroy() {
        Gl.DSA.deleteVertexArray(id);
    }

    @Override
    public void setAttributes(VertexAttribute[] attributes, IVertexBuffer vertexBuffer) {
        if (!Gl.isSupportDSA()) {
            try (GlState ignored = new GlState(GlState.STATE_VERTEX_OPERATIONS | GlState.STATE_VBO)) {
                glBindVertexArray(id);
                glBindBuffer(GL_ARRAY_BUFFER, (int) vertexBuffer.handle());
                for (VertexAttribute attr : attributes) {
                    int loc = attr.getLocation();
                    int componentCount = attr.getComponentCount();
                    int stride = attr.getStride();
                    int offset = attr.getOffset();

                    switch (attr.getDataType()) {
                        case FLOAT:
                            glVertexAttribPointer(
                                    loc,
                                    componentCount,
                                    GL_FLOAT,
                                    false,
                                    stride,
                                    offset
                            );
                            break;
                        case INTEGER:
                            glVertexAttribIPointer(
                                    loc,
                                    componentCount,
                                    GL_INT,
                                    stride,
                                    offset
                            );
                            break;
                    }
                    glEnableVertexAttribArray(loc);
                }
                glBindBuffer(GL_ARRAY_BUFFER, 0);
                glBindVertexArray(0);
            }
        } else {
            int bindingIndex = 0;
            Gl.DSA.vertexArrayVertexBuffer(id, bindingIndex, (int) vertexBuffer.handle(), 0, attributes[0].getStride());
            for (VertexAttribute attr : attributes) {
                int loc = attr.getLocation();
                Gl.DSA.vertexArrayAttribBinding(id, loc, bindingIndex);
                switch (attr.getDataType()) {
                    case FLOAT ->
                            Gl.DSA.vertexArrayAttribFormat(id, loc, attr.getComponentCount(), GL45.GL_FLOAT, false, attr.getOffset());
                    case INTEGER ->
                            Gl.DSA.vertexArrayAttribFormat(id, loc, attr.getComponentCount(), GL45.GL_INT, false, attr.getOffset());
                }
                Gl.DSA.enableVertexArrayAttrib(id, loc);
            }
        }

    }

    @Override
    public long handle() {
        return id;
    }
}