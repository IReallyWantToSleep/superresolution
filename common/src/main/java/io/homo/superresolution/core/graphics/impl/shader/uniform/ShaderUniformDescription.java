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

package io.homo.superresolution.core.graphics.impl.shader.uniform;

public class ShaderUniformDescription {
    private final String name;
    private final ShaderUniformType type;
    private int binding = -1;
    private int bufferSize = -1;
    private ShaderUniformAccess access = ShaderUniformAccess.Both;

    private ShaderUniformDescription(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.binding = builder.binding;
        this.bufferSize = builder.bufferSize;
        this.access = builder.access;
    }

    public static Builder builder(String name, ShaderUniformType type) {
        return new Builder(name, type);
    }

    public ShaderUniformAccess access() {
        return this.access;
    }

    public String name() {
        return name;
    }

    public ShaderUniformType type() {
        return type;
    }

    public int binding() {
        return binding;
    }

    public int bufferSize() {
        return bufferSize;
    }

    public static class Builder {
        private final String name;
        private final ShaderUniformType type;
        private int binding = -1;
        private int bufferSize = -1;
        private ShaderUniformAccess access = ShaderUniformAccess.Both;

        public Builder(String name, ShaderUniformType type) {
            this.name = name;
            this.type = type;
        }

        public Builder access(ShaderUniformAccess access) {
            this.access = access;
            return this;
        }

        public Builder binding(int binding) {
            this.binding = binding;
            return this;
        }

        public Builder bufferSize(int size) {
            if (type != ShaderUniformType.UniformBuffer) {
                throw new IllegalArgumentException("Buffer size only applicable to uniform blocks");
            }
            this.bufferSize = size;
            return this;
        }

        public ShaderUniformDescription build() {
            return new ShaderUniformDescription(this);
        }
    }
}