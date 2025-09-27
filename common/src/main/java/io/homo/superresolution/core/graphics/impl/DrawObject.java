/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.impl;

import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.vertex.*;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexArray;
import io.homo.superresolution.core.impl.Destroyable;

public class DrawObject implements Destroyable {
    private final IVertexBuffer vertexBuffer;
    private final IVertexArray vertexArray;
    private final PrimitiveType primitiveType;
    private boolean once;
    private static DrawObject fullscreenQuadInstance;

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

    public static DrawObject fullscreenQuad(IDevice device) {
        //if (fullscreenQuadInstance == null) {
        float[] vertices = {
                -1f, 1f, 0f, 1f,
                1f, 1f, 1f, 1f,
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f
        };

        VertexBufferDescription desc = new VertexBufferDescription(vertices.length * Float.BYTES, false);
        IVertexBuffer vbo = device.createVertexBuffer(desc);
        vbo.updateData(vertices, 0, vertices.length);

        VertexAttribute[] attributes = new VertexAttribute[]{
                new VertexAttribute(0, 2, VertexAttribute.DataType.FLOAT, 4 * Float.BYTES, 0),
                new VertexAttribute(1, 2, VertexAttribute.DataType.FLOAT, 4 * Float.BYTES, 2 * Float.BYTES)
        };
        //TODO:转换成通用实现
        IVertexArray vao = new GlVertexArray();
        vao.setAttributes(attributes, vbo);

        return new DrawObject(vbo, vao, PrimitiveType.TriangleStrip);
        //}

        //return fullscreenQuadInstance;
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

    public boolean isOnce() {
        return once;
    }

    public DrawObject once() {
        this.once = true;
        return this;
    }

    @Override
    public void destroy() {

        this.vertexBuffer.destroy();
        this.vertexArray.destroy();

    }

    public void close() {
        this.destroy();
    }
}
