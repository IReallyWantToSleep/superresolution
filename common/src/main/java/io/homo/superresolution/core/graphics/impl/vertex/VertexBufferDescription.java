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

package io.homo.superresolution.core.graphics.impl.vertex;

public class VertexBufferDescription {
    private final int sizeInBytes;
    private final boolean dynamic;
    private final VertexFormat vertexFormat;

    public VertexBufferDescription(int sizeInBytes, boolean dynamic, VertexFormat vertexFormat) {
        this.sizeInBytes = sizeInBytes;
        this.dynamic = dynamic;
        if (vertexFormat == null) {
            throw new IllegalArgumentException();
        }
        this.vertexFormat = vertexFormat;
    }

    public static VertexBufferDescription create(int sizeInBytes, boolean dynamic, VertexFormat vertexFormat) {
        return new VertexBufferDescription(sizeInBytes, dynamic, vertexFormat);
    }

    public static VertexBufferDescription create(int sizeInBytes, VertexFormat vertexFormat) {
        return new VertexBufferDescription(sizeInBytes, false, vertexFormat);
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public VertexFormat getVertexFormat() {
        return vertexFormat;
    }
}
