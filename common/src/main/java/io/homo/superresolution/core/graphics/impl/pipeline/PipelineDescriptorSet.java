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
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

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
        bindings.put(name, new ResourceBinding(ResourceType.UNIFORM_BUFFER, binding, buffer));
        dirty = true;
        return this;
    }

    public PipelineDescriptorSet samplerTexture(String name, int binding, ITexture texture) {
        bindings.put(name, new ResourceBinding(ResourceType.SAMPLER_TEXTURE, binding, texture));
        dirty = true;
        return this;
    }

    public PipelineDescriptorSet storageImage(String name, int binding, ITexture texture) {
        bindings.put(name, new ResourceBinding(ResourceType.STORAGE_IMAGE, binding, texture));
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

    public PipelineDescriptorSet samplerTexture(String name, ITexture texture) {
        return samplerTexture(name, getBinding(name), texture);
    }

    public PipelineDescriptorSet storageImage(String name, ITexture texture) {
        return storageImage(name, getBinding(name), texture);
    }

    public void update() {
        if (dirty) {
            updateImpl();
            dirty = false;
        }
    }

    public abstract void apply();

    protected abstract void updateImpl();

    protected enum ResourceType {
        UNIFORM_BUFFER,
        SAMPLER_TEXTURE,
        STORAGE_IMAGE
    }

    public static class ResourceBinding {
        final ResourceType type;
        final GpuObject resource;
        final int bindingPoint;

        ResourceBinding(ResourceType type, int bindingPoint, GpuObject resource) {
            this.type = type;
            this.resource = resource;
            this.bindingPoint = bindingPoint;
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
