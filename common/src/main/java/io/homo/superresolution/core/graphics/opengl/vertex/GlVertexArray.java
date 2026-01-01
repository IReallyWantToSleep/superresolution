/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.graphics.opengl.vertex;


import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttributeFormat;
import io.homo.superresolution.core.graphics.impl.vertex.VertexFormat;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import org.lwjgl.opengl.GL45;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL41.*;


public class GlVertexArray implements GpuObject {
    private final int id;

    public GlVertexArray() {
        this.id = Gl.DSA.createVertexArray();
    }

    public void destroy() {
        Gl.DSA.deleteVertexArray(id);
    }

    public void bind() {
        glBindVertexArray(id);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    public void setFormat(IVertexBuffer vertexBuffer) {
        VertexFormat format = vertexBuffer.getVertexFormat();
        if (!Gl.isSupportDSA()) {
            try (GlState ignored = new GlState(GlState.STATE_VERTEX_OPERATIONS | GlState.STATE_VBO)) {
                glBindVertexArray(id);
                glBindBuffer(GL_ARRAY_BUFFER, (int) vertexBuffer.handle());

                int stride = format.stride();
                for (VertexFormat.VertexAttribute attr : format.attributes()) {
                    int location = attr.location();
                    VertexAttributeFormat attrFormat = attr.format();
                    int size = attrFormat.getComponentCount();
                    int type = getGlType(attrFormat);
                    boolean normalized = isNormalized(attrFormat);
                    int offset = attr.offset();

                    if (isIntegerType(attrFormat)) {
                        glVertexAttribIPointer(location, size, type, stride, offset);
                    } else {
                        glVertexAttribPointer(location, size, type, normalized, stride, offset);
                    }
                    glEnableVertexAttribArray(location);
                }

                glBindBuffer(GL_ARRAY_BUFFER, 0);
                glBindVertexArray(0);
            }
        } else {
            int bindingIndex = 0;
            Gl.DSA.vertexArrayVertexBuffer(id, bindingIndex, (int) vertexBuffer.handle(), 0, format.stride());

            for (VertexFormat.VertexAttribute attr : format.attributes()) {
                int location = attr.location();
                VertexAttributeFormat attrFormat = attr.format();

                Gl.DSA.vertexArrayAttribBinding(id, location, bindingIndex);

                int type = getGlType(attrFormat);
                boolean normalized = isNormalized(attrFormat);
                int size = attrFormat.getComponentCount();

                if (isIntegerType(attrFormat)) {
                    GL45.glVertexArrayAttribIFormat(id, location, size, type, attr.offset());
                } else {
                    Gl.DSA.vertexArrayAttribFormat(id, location, size, type, normalized, attr.offset());
                }
                Gl.DSA.enableVertexArrayAttrib(id, location);
            }
        }
    }

    private int getGlType(VertexAttributeFormat format) {
        return switch (format) {
            case FLOAT, FLOAT2, FLOAT3,
                 FLOAT4 -> GL_FLOAT;
            case INT, INT2, INT3, INT4 -> GL_INT;
            case UINT, UINT2, UINT3, UINT4 -> GL_UNSIGNED_INT;
            case BYTE4_NORMALIZED -> GL_BYTE;
            case UBYTE4_NORMALIZED -> GL_UNSIGNED_BYTE;
            case SHORT2, SHORT4 -> GL_SHORT;
            case USHORT2, USHORT4 -> GL_UNSIGNED_SHORT;
        };
    }

    private boolean isNormalized(VertexAttributeFormat format) {
        return switch (format) {
            case BYTE4_NORMALIZED, UBYTE4_NORMALIZED -> true;
            default -> false;
        };
    }

    private boolean isIntegerType(VertexAttributeFormat format) {
        return switch (format) {
            case INT, INT2, INT3, INT4, UINT, UINT2, UINT3, UINT4 -> true;
            default -> false;
        };
    }

    @Override
    public long handle() {
        return id;
    }
}