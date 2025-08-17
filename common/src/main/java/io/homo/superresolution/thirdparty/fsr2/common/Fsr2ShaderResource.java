package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobResource;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineResourceAccess;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlSampler;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;

import java.util.UUID;
import java.util.function.Supplier;

public class Fsr2ShaderResource {
    public PipelineResourceAccess access = PipelineResourceAccess.Read;
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


    public PipelineResourceAccess access() {
        return access;
    }

    public Fsr2ShaderResource access(PipelineResourceAccess access) {
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

    public PipelineJobResource<?> getResourceDescription(Fsr2Context context) {
        if (this.resourceType != null) {
            this.resourceEntry = () -> context.resources.resource(resourceType.get());
        }
        Fsr2PipelineResourceType resourceType = context.resources.resourceEntriesMap().get(resourceEntry.get());
        if (resourceType == null) throw new RuntimeException();
        String name = resourceName != null ? resourceName : access == PipelineResourceAccess.Read ? resourceType.srvShaderName() : resourceType.uavShaderName();
        if (name == null) {
            name = "RESOURCE+" + UUID.randomUUID() + "+" + binding;
        } else if (resourceName == null) {
            name = name + "+" + binding;
        }
        if (resourceEntry.get().type() == Fsr2PipelineResources.Fsr2ResourceType.UBO) {
            return
                    PipelineJobResource.UniformBuffer.create((GlBuffer) resourceEntry.get().getResource());
        } else {
            ITexture textureSupplier = TextureSupplier.of(() -> {
                if (this.resourceType != null) {
                    this.resourceEntry = () -> context.resources.resource(this.resourceType.get());
                }
                Fsr2PipelineResourceType _resourceType = context.resources.resourceEntriesMap().get(resourceEntry.get());
                ITexture texture = (ITexture) resourceEntry.get().getResource();
                if (texture == null) {
                    if (_resourceType == Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_5) {
                        if (context.resources.resource(Fsr2PipelineResourceType.SCENE_LUMINANCE).getResource() != null) {
                            GlTexture2D texture2D = ((GlTexture2D) context.resources.resource(Fsr2PipelineResourceType.SCENE_LUMINANCE).getResource());
                            return texture2D.getMipView(Math.min(texture2D.getMipmapLevel(), 5));
                        }
                    }
                    Fsr2Context.LOGGER.error("%s %s".formatted(resourceEntry.get().type().name(), resourceEntry.get().getDescription().label));
                    return RenderSystems.current().device().createTexture(
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
                return texture;
            });
            if (access != PipelineResourceAccess.Read) {
                return PipelineJobResource.StorageTexture.create(textureSupplier, access);
            } else {
                return PipelineJobResource.SamplerTexture.create(textureSupplier);
            }
        }
    }
}
