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

package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.sampler.ISampler;
import io.homo.superresolution.core.graphics.impl.sampler.SamplerDescription;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.impl.texture.*;

import java.util.function.Supplier;

public class Fsr2ShaderResource {
    private static ITexture nullTexture;
    public ShaderResourceAccess access = ShaderResourceAccess.Read;
    public int binding = -1;
    public Supplier<Fsr2PipelineResources.Fsr2ResourceEntry> resourceEntry = null;
    public Supplier<Fsr2PipelineResourceType> resourceType = null;
    public String resourceName;
    public ISampler sampler = RenderSystems.current().device().createSampler(
            SamplerDescription.create()
                    .filterMode(TextureFilterMode.Nearest)
                    .build()
    );

    public ISampler sampler() {
        return sampler;
    }

    public Fsr2ShaderResource sampler(ISampler sampler) {
        this.sampler = sampler;
        return this;
    }

    public ShaderResourceAccess access() {
        return access;
    }

    public Fsr2ShaderResource access(ShaderResourceAccess access) {
        this.access = access;
        return this;
    }

    public int binding() {
        return binding;
    }

    public Fsr2ShaderResource binding(int binding) {
        this.binding = binding;
        return this;
    }


    public Fsr2ShaderResource resourceEntry(Fsr2PipelineResources.Fsr2ResourceEntry resourceEntry) {
        this.resourceEntry = () -> resourceEntry;
        return this;
    }

    public Fsr2ShaderResource resourceType(Fsr2PipelineResourceType resourceType) {
        this.resourceType = () -> resourceType;
        return this;
    }

    public Fsr2ShaderResource resourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public Fsr2ShaderResource resourceEntrySupplier(Supplier<Fsr2PipelineResources.Fsr2ResourceEntry> resourceEntry) {
        this.resourceEntry = resourceEntry;
        return this;
    }

    public Fsr2ShaderResource resourceTypeSupplier(Supplier<Fsr2PipelineResourceType> resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public void bindToDescriptorSet(String name, PipelineDescriptorSet ds, Fsr2Context context) {
        if (this.resourceType != null) {
            this.resourceEntry = () -> context.resources.resource(resourceType.get());
        }

        if (this.resourceEntry == null) {
            throw new IllegalStateException(
                    "FSR2 shader resource '" + name + "' has no resourceEntry or resourceType set"
            );
        }

        Fsr2PipelineResources.Fsr2ResourceEntry entry = resourceEntry.get();

        if (entry == null) {
            throw new IllegalStateException(
                    "FSR2 shader resource '" + name + "' resolved to a null resource entry"
            );
        }

        if (entry.type() == Fsr2PipelineResources.Fsr2ResourceType.UBO) {
            ds.uniformBuffer(name, (IBuffer) entry.getResource());
        } else {
            ITexture texture = (ITexture) entry.getResource();

            if (texture == null) {
                if (nullTexture == null) {
                    nullTexture = RenderSystems.current().device().createTexture(
                            TextureDescription.create()
                                    .width(1)
                                    .height(1)
                                    .type(TextureType.Texture2D)
                                    .format(TextureFormat.RGBA8)
                                    .usages(TextureUsages.create().storage().sampler())
                                    .label("SRFSR2NullTexture")
                                    .build()
                    );
                }
                texture = nullTexture;
            }

            if (access != ShaderResourceAccess.Read) {
                ds.storageImage(name, texture);
            } else {
                ds.samplerTexture(name, texture);
            }
        }
    }
}
