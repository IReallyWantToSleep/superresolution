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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VertexFormat {
    private final List<VertexAttribute> attributes;
    private final int stride;

    private VertexFormat(List<VertexAttribute> attributes, int stride) {
        this.attributes = new ArrayList<>(attributes);
        this.stride = stride;
    }

    public static VertexFormat.Builder builder() {
        return new VertexFormat.Builder();
    }

    public List<VertexAttribute> attributes() {
        return attributes;
    }

    public int stride() {
        return stride;
    }

    public static class VertexAttribute {
        private final int location;
        private final String name;
        private final VertexAttributeFormat format;
        private final int offset;

        public VertexAttribute(int location, String name, VertexAttributeFormat format, int offset) {
            this.location = location;
            this.name = name;
            this.format = format;
            this.offset = offset;
        }

        public int location() {
            return location;
        }

        public String name() {
            return name;
        }

        public VertexAttributeFormat format() {
            return format;
        }

        public int offset() {
            return offset;
        }
    }

    public static class Builder {
        private final Map<Integer, AttributeEntry> attributes = new LinkedHashMap<>();
        private int currentOffset = 0;

        public Builder addAttribute(int location, String name, VertexAttributeFormat format) {
            if (attributes.containsKey(location)) {
                throw new IllegalArgumentException("Location " + location + " already used");
            }

            attributes.put(location, new AttributeEntry(location, name, format, currentOffset));
            currentOffset += format.getSize();
            return this;
        }

        public Builder addAttribute(int location, VertexAttributeFormat format) {
            return addAttribute(location, "attrib_" + location, format);
        }

        public VertexFormat build() {
            List<VertexAttribute> attrList = new ArrayList<>();
            for (AttributeEntry entry : attributes.values()) {
                attrList.add(new VertexAttribute(
                        entry.location,
                        entry.name,
                        entry.format,
                        entry.offset
                ));
            }

            int finalStride = currentOffset;
            return new VertexFormat(attrList, finalStride);
        }

        private static class AttributeEntry {
            final int location;
            final String name;
            final VertexAttributeFormat format;
            final int offset;

            AttributeEntry(int location, String name, VertexAttributeFormat format, int offset) {
                this.location = location;
                this.name = name;
                this.format = format;
                this.offset = offset;
            }
        }
    }
}
