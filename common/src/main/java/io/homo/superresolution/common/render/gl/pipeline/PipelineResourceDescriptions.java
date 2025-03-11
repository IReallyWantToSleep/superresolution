package io.homo.superresolution.common.render.gl.pipeline;

import io.homo.superresolution.common.render.impl.ITexture;

import java.util.HashMap;
import java.util.Map;

public class PipelineResourceDescriptions {
    protected Map<String, PipelineResourceDescription> resource = new HashMap<>();

    public PipelineResourceDescriptions addResource(PipelineResourceDescription description) {
        if (description.type == PipelineResourceType.Sampler2D && description.access != PipelineResourceAccess.READ) {
            throw new RuntimeException("管线资源类型为Sampler2D但访问类型不为只读");
        }
        resource.put(description.name, description);
        return this;
    }

    public Map<String, PipelineResourceDescription> resource() {
        return resource;
    }

    public PipelineResourceDescriptions clone() {
        PipelineResourceDescriptions n = new PipelineResourceDescriptions();
        n.resource.putAll(this.resource);
        return n;
    }

    public enum PipelineResourceAccess {
        READ, WRITE, BOTH
    }

    public record PipelineResourceDescription(
            PipelineResourceType type,
            String name,
            ITexture src,
            PipelineResourceAccess access
    ) {

    }
}

