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

package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.sampler.ISampler;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.ITextureView;

import java.util.HashMap;
import java.util.Map;

public abstract class PipelineDescriptorSet {
    protected final IShaderProgram shader;
    protected final Map<String, ResourceBinding> bindings = new HashMap<>();
    protected boolean dirty = false;

    protected PipelineDescriptorSet(IShaderProgram shader) {
        this.shader = shader;
    }

    public DescriptorSnapshot createSnapshot() {
        return new DescriptorSnapshot(new HashMap<>(bindings));
    }

    public void applySnapshot(DescriptorSnapshot snapshot) {
        bindings.clear();
        bindings.putAll(snapshot.bindings);
        dirty = true;
    }

    public PipelineDescriptorSet uniformBuffer(String name, int binding, IBuffer buffer) {
        bindings.put(name, new ResourceBinding(ResourceType.UNIFORM_BUFFER, binding, buffer, null, 0L, buffer.getSize()));
        dirty = true;
        return this;
    }

    public PipelineDescriptorSet uniformBufferRange(String name, int binding, IBuffer buffer, long offset, long range) {
        bindings.put(name, new ResourceBinding(ResourceType.UNIFORM_BUFFER, binding, buffer, null, offset, range));
        dirty = true;
        return this;
    }

    public PipelineDescriptorSet samplerTexture(String name, int binding, ITexture texture) {
        bindings.put(name, new ResourceBinding(ResourceType.SAMPLER_TEXTURE, binding, texture, null, 0L, 0L));
        dirty = true;
        return this;
    }

    public PipelineDescriptorSet samplerTexture(String name, int binding, ITexture texture, ISampler sampler) {
        bindings.put(name, new ResourceBinding(ResourceType.SAMPLER_TEXTURE, binding, texture, sampler, 0L, 0L));
        dirty = true;
        return this;
    }

    public PipelineDescriptorSet storageImage(String name, int binding, ITexture texture) {
        bindings.put(name, new ResourceBinding(ResourceType.STORAGE_IMAGE, binding, texture, null, 0L, 0L));
        dirty = true;
        return this;
    }

    private int getBinding(String name) {
        if (!shader.getDescription().resourcesLayout().hasResource(name)) {
            throw new IllegalArgumentException();
        }
        return shader.getDescription().resourcesLayout().getResource(name).binding();
    }

    public PipelineDescriptorSet uniformBuffer(String name, IBuffer buffer) {
        return uniformBuffer(name, getBinding(name), buffer);
    }

    public PipelineDescriptorSet uniformBufferRange(String name, IBuffer buffer, long offset, long range) {
        return uniformBufferRange(name, getBinding(name), buffer, offset, range);
    }

    public PipelineDescriptorSet samplerTexture(String name, ITexture texture) {
        return samplerTexture(name, getBinding(name), texture);
    }

    public PipelineDescriptorSet samplerTexture(String name, ITexture texture, ISampler sampler) {
        return samplerTexture(name, getBinding(name), texture, sampler);
    }

    public PipelineDescriptorSet storageImage(String name, ITexture texture) {
        return storageImage(name, getBinding(name), texture);
    }

    public PipelineDescriptorSet samplerTexture(String name, int binding, ITextureView view) {
        return samplerTexture(name, binding, (ITexture) view);
    }

    public PipelineDescriptorSet samplerTexture(String name, int binding, ITextureView view, ISampler sampler) {
        return samplerTexture(name, binding, (ITexture) view, sampler);
    }

    public PipelineDescriptorSet storageImage(String name, int binding, ITextureView view) {
        return storageImage(name, binding, (ITexture) view);
    }

    public PipelineDescriptorSet samplerTexture(String name, ITextureView view) {
        return samplerTexture(name, getBinding(name), (ITexture) view);
    }

    public PipelineDescriptorSet samplerTexture(String name, ITextureView view, ISampler sampler) {
        return samplerTexture(name, getBinding(name), (ITexture) view, sampler);
    }

    public PipelineDescriptorSet storageImage(String name, ITextureView view) {
        return storageImage(name, getBinding(name), (ITexture) view);
    }

    public void update() {
        if (dirty) {
            updateImpl();
            dirty = false;
        }
    }

    public Map<String, ResourceBinding> getBindings() {
        return java.util.Collections.unmodifiableMap(bindings);
    }

    public IShaderProgram getShader() {
        return shader;
    }

    public abstract void apply();

    protected abstract void updateImpl();

    public enum ResourceType {
        UNIFORM_BUFFER,
        SAMPLER_TEXTURE,
        STORAGE_IMAGE
    }

    public static class ResourceBinding {
        final ResourceType type;
        final GpuObject resource;
        final int bindingPoint;
        final ISampler sampler;
        final long offset;
        final long range;

        ResourceBinding(ResourceType type, int bindingPoint, GpuObject resource, ISampler sampler, long offset, long range) {
            this.type = type;
            this.resource = resource;
            this.bindingPoint = bindingPoint;
            this.sampler = sampler;
            this.offset = offset;
            this.range = range;
        }

        public ResourceType type() {
            return type;
        }

        public GpuObject resource() {
            return resource;
        }

        public int bindingPoint() {
            return bindingPoint;
        }

        public ISampler sampler() {
            return sampler;
        }

        public long offset() {
            return offset;
        }

        public long range() {
            return range;
        }
    }

    public static class DescriptorSnapshot {
        private final Map<String, ResourceBinding> bindings;

        private DescriptorSnapshot(Map<String, ResourceBinding> bindings) {
            this.bindings = bindings;
        }

        public Map<String, ResourceBinding> getBindings() {
            return new HashMap<>(bindings);
        }
    }
}
