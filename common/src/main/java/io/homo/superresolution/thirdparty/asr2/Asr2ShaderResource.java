package io.homo.superresolution.thirdparty.asr2;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceType;

import java.util.UUID;
import java.util.function.Supplier;

public class Asr2ShaderResource {
    public GlPipelineResourceAccess access = GlPipelineResourceAccess.READ;
    public int binding = -1;
    public Supplier<Asr2PipelineResources.Fsr2ResourceEntry> resourceEntry = null;
    public Supplier<Asr2PipelineResourceType> resourceType = null;
    public String resourceName;


    public GlPipelineResourceAccess access() {
        return access;
    }

    public Asr2ShaderResource access(GlPipelineResourceAccess access) {
        this.access = access;
        return this;
    }

    public int binding() {
        return binding;
    }

    public Asr2ShaderResource binding(int binding) {
        this.binding = binding;
        return this;
    }


    public Asr2ShaderResource resourceEntry(Asr2PipelineResources.Fsr2ResourceEntry resourceEntry) {
        this.resourceEntry = () -> resourceEntry;
        return this;
    }

    public Asr2ShaderResource resourceType(Asr2PipelineResourceType resourceType) {
        this.resourceType = () -> resourceType;
        return this;
    }

    public Asr2ShaderResource resourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public Asr2ShaderResource resourceEntrySupplier(Supplier<Asr2PipelineResources.Fsr2ResourceEntry> resourceEntry) {
        this.resourceEntry = resourceEntry;
        return this;
    }

    public Asr2ShaderResource resourceTypeSupplier(Supplier<Asr2PipelineResourceType> resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public GlPipelineResourceDescription getResourceDescription(Asr2Context context) {
        if (this.resourceType != null) {
            this.resourceEntry = () -> context.resources.resource(resourceType.get());
        }
        Asr2PipelineResourceType resourceType = context.resources.resourceEntriesMap().get(resourceEntry.get());
        if (resourceType == null) throw new RuntimeException();
        String name = resourceName != null ? resourceName : access == GlPipelineResourceAccess.READ ? resourceType.srvShaderName() : resourceType.uavShaderName();
        if (name == null) {
            name = "RESOURCE+" + UUID.randomUUID() + "+" + binding;
        } else if (resourceName == null) {
            name = name + "+" + binding;
        }
        if (resourceEntry.get().type() == Asr2PipelineResources.Fsr2ResourceType.UBO) {
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
                                    Asr2Context.LOGGER.error("%s %s".formatted(resourceEntry.get().type().name(), resourceEntry.get().getDescription().label));
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
