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
            if (size <= 0) throw new IllegalArgumentException("Size must be positive");
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