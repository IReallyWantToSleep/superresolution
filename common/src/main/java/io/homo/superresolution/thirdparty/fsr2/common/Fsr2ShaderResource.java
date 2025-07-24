package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.graphics.opengl.texture.GlSampler;

import java.util.UUID;
import java.util.function.Supplier;

public class Fsr2ShaderResource {
    public GlPipelineResourceAccess access = GlPipelineResourceAccess.READ;
    public int binding = -1;
    public Supplier<Fsr2PipelineResources.Fsr2ResourceEntry> resourceEntry = null;
    public Supplier<Fsr2PipelineResourceType> resourceType = null;
    public String resourceName;

    public GlSampler sampler() {
        return sampler;
    }

    public Fsr2ShaderResource sampler(GlSampler sampler) {
        this.sampler = sampler;
        return this;
    }

    public GlSampler sampler = GlSampler.create(GlSampler.SamplerType.NearestClamp);


    public GlPipelineResourceAccess access() {
        return access;
    }

    public Fsr2ShaderResource access(GlPipelineResourceAccess access) {
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

    public GlPipelineResourceDescription getResourceDescription(Fsr2Context context) {
        if (this.resourceType != null) {
            this.resourceEntry = () -> context.resources.resource(resourceType.get());
        }
        Fsr2PipelineResourceType resourceType = context.resources.resourceEntriesMap().get(resourceEntry.get());
        if (resourceType == null) throw new RuntimeException();
        String name = resourceName != null ? resourceName : access == GlPipelineResourceAccess.READ ? resourceType.srvShaderName() : resourceType.uavShaderName();
        if (name == null) {
            name = "RESOURCE+" + UUID.randomUUID() + "+" + binding;
        } else if (resourceName == null) {
            name = name + "+" + binding;
        }
        if (resourceEntry.get().type() == Fsr2PipelineResources.Fsr2ResourceType.UBO) {
            return
                    GlPipelineResourceDescription.createUBOResource(
                            name,
                            (GlBuffer) resourceEntry.get().getResource(),
                            binding
                    );
        } else {
            return
                    GlPipelineResourceDescription.createTextureResource(
                            access != GlPipelineResourceAccess.READ ? GlPipelineResourceType.Image2D : GlPipelineResourceType.Sampler2D,
                            name,
                            TextureSupplier.of(() -> {
                                ITexture texture = (ITexture) resourceEntry.get().getResource();
                                if (texture == null) {
                                    Fsr2Context.LOGGER.error("%s %s".formatted(resourceEntry.get().type().name(), resourceEntry.get().getDescription().label));
                                    return RenderSystems.current().device().createTexture(
                                            TextureDescription.create()
                                                    .width(1)
                                                    .height(1)
                                                    .type(TextureType.Texture2D)
                                                    .format(TextureFormat.RGBA8)
                                                    .usages(TextureUsages.create().storage().sampler())
                                                    .build()
                                    );
                                }
                                return texture;
                            }),
                            access,
                            null,
                            binding
                    );

        }
    }
}
