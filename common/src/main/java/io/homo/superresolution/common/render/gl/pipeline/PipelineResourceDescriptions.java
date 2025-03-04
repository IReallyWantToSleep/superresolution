package io.homo.superresolution.common.render.gl.pipeline;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.gl.texture.ITextureWrapper;

import java.util.Map;

public class PipelineResourceDescriptions {
    protected Map<String, PipelineResourceDescription> resource;

    public PipelineResourceDescriptions addResource(PipelineResourceDescription description) {
        resource.put(description.name, description);
        return this;
    }

    public Map<String, PipelineResourceDescription> resource() {
        return resource;
    }

    public record PipelineResourceDescription(PipelineResourceType type, String name, ITextureWrapper src) {

    }
}

