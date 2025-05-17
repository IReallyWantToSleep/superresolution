package io.homo.superresolution.fsr2;

import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.gl.texture.GlSampler;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.impl.texture.TextureSupplier;

import java.util.UUID;
import java.util.function.Supplier;

public class Fsr2ShaderResource {
    public GlPipelineResourceAccess access = GlPipelineResourceAccess.READ;
    public int binding = -1;
    public Supplier<Fsr2PipelineResources.Fsr2ResourceEntry> resourceEntry = null;
    public Supplier<Fsr2PipelineResourceType> resourceType = null;
    public String resourceName;


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
                            (GlUniformBuffer<?>) resourceEntry.get().getResource(),
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
                                    return GlTexture2D.create(
                                            1, 1, TextureFormat.RGBA8
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
