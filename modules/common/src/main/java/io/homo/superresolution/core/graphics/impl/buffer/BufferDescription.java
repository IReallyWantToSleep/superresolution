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
    private final BufferUsages usages;

    protected BufferDescription(long size, BufferUsages usages) {
        this.size = size;
        this.usages = usages;
    }

    public static Builder create() {
        return new Builder();
    }

    public long size() {
        return size;
    }

    public BufferUsages usage() {
        return usages;
    }

    @Override
    public String toString() {
        return "BufferDescription{" +
                "size=" + size +
                ", usage=" + usages +
                '}';
    }

    public static class Builder {
        private final BufferUsages usages = BufferUsages.create();
        private long size;

        public Builder size(long size) {
            if (size <= 0) {
                throw new IllegalArgumentException("Size must be positive");
            }
            this.size = size;
            return this;
        }

        public Builder usage(BufferUsage usage) {
            this.usages.add(Objects.requireNonNull(usage, "Usage cannot be null"));
            return this;
        }

        public Builder usages(BufferUsages usages) {
            this.usages.add(usages.getUsages().toArray(new BufferUsage[0]));
            return this;
        }

        public BufferDescription build() {
            if (size <= 0) {
                throw new IllegalStateException("Size must be set to a positive value");
            }
            if (usages.isEmpty()) {
                throw new IllegalStateException("Usage must be set");
            }
            return new BufferDescription(size, usages);
        }
    }
}