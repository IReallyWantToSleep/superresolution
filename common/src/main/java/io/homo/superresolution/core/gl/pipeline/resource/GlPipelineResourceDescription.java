package io.homo.superresolution.core.gl.pipeline.resource;

import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.core.gl.texture.GlSampler;
import io.homo.superresolution.core.impl.texture.ITexture;

public record GlPipelineResourceDescription(
        GlPipelineResourceType type,
        String name,
        ITexture src,
        GlUniformBuffer<?> ubo,
        GlPipelineResourceAccess access,
        GlSampler sampler,
        int unit
) {
    public GlPipelineResourceDescription {
        switch (type) {
            case Image2D, Sampler2D -> {
                if (src == null) throw new IllegalArgumentException("必须提供纹理");
                if (ubo != null) throw new IllegalArgumentException("不能同时包含UBO");
            }
            case UniformBuffer -> {
                if (ubo == null) throw new IllegalArgumentException("必须提供UBO");
                if (src != null) throw new IllegalArgumentException("不能包含纹理");
            }
        }
    }

    public static GlPipelineResourceDescription createUBOResource(
            String name,
            GlUniformBuffer<?> ubo,
            int bindingPoint
    ) {
        return new GlPipelineResourceDescription(
                GlPipelineResourceType.UniformBuffer,
                name,
                null,
                ubo,
                GlPipelineResourceAccess.READ,
                null,
                bindingPoint
        );
    }

    public static GlPipelineResourceDescription createTextureResource(
            GlPipelineResourceType type, String name, ITexture src,
            GlPipelineResourceAccess access, GlSampler sampler, int unit
    ) {
        return new GlPipelineResourceDescription(type, name, src, null, access, sampler, unit);
    }
}