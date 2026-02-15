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

package io.homo.superresolution.core.graphics.impl.buffer;

import java.util.Objects;

public class BufferDescription {
    private final long size;
    private final BufferUsage usage;

    protected BufferDescription(long size, BufferUsage usage) {
        this.size = size;
        this.usage = usage;
    }

    public static Builder create() {
        return new Builder();
    }

    public long size() {
        return size;
    }

    public BufferUsage usage() {
        return usage;
    }

    @Override
    public String toString() {
        return "BufferDescription{" +
                "size=" + size +
                ", usage=" + usage +
                '}';
    }

    public static class Builder {
        private long size;
        private BufferUsage usage;

        public Builder size(long size) {
            if (size <= 0) {
                throw new IllegalArgumentException("Size must be positive");
            }
            this.size = size;
            return this;
        }

        public Builder usage(BufferUsage usage) {
            this.usage = Objects.requireNonNull(usage, "Usage cannot be null");
            return this;
        }

        public BufferDescription build() {
            if (size <= 0) {
                throw new IllegalStateException("Size must be set to a positive value");
            }
            if (usage == null) {
                throw new IllegalStateException("Usage must be set");
            }
            return new BufferDescription(size, usage);
        }
    }
}