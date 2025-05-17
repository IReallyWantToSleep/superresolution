package io.homo.superresolution.fsr2;

import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.gl.texture.GlSampler;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureSupplier;

public class Fsr2ShaderResource {
    public GlPipelineResourceAccess access = GlPipelineResourceAccess.READ;
    public int binding = -1;
    public Fsr2PipelineResources.Fsr2ResourceEntry resourceEntry = null;
    public Fsr2PipelineResourceType resourceType = null;


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

    public Fsr2PipelineResources.Fsr2ResourceEntry resourceEntry() {
        return resourceEntry;
    }

    public Fsr2ShaderResource resourceEntry(Fsr2PipelineResources.Fsr2ResourceEntry resourceEntry) {
        this.resourceEntry = resourceEntry;
        return this;
    }

    public Fsr2ShaderResource resourceType(Fsr2PipelineResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public GlPipelineResourceDescription getResourceDescription(Fsr2Context context) {
        if (this.resourceType != null) {
            this.resourceEntry = context.resources.resource(resourceType);
        }
        Fsr2PipelineResourceType resourceType = context.resources.resourceEntriesMap().get(resourceEntry);
        if (resourceType == null) throw new RuntimeException();
        if (resourceEntry.type() == Fsr2PipelineResources.Fsr2ResourceType.UBO) {
            return
                    GlPipelineResourceDescription.createUBOResource(
                            access == GlPipelineResourceAccess.READ ? resourceType.srvShaderName() : resourceType.uavShaderName(),
                            (GlUniformBuffer<?>) resourceEntry.getResource(),
                            binding
                    );
        } else {
            return
                    GlPipelineResourceDescription.createTextureResource(
                            access != GlPipelineResourceAccess.READ ? GlPipelineResourceType.Image2D : GlPipelineResourceType.Sampler2D,
                            access == GlPipelineResourceAccess.READ ? resourceType.srvShaderName() : resourceType.uavShaderName(),
                            TextureSupplier.of(() -> (ITexture) resourceEntry.getResource()),
                            access,
                            GlSampler.create(GlSampler.SamplerType.LinearClamp),
                            binding
                    );

        }
    }
}
