package io.homo.superresolution.core.graphics.opengl.pipeline.resource;

import java.util.HashMap;
import java.util.Map;

public class GlPipelineResourceDescriptions {
    public Map<String, GlPipelineResourceDescription> resource = new HashMap<>();

    public GlPipelineResourceDescriptions addResource(GlPipelineResourceDescription description) {
        if (description.type() == GlPipelineResourceType.Sampler2D && description.access() != GlPipelineResourceAccess.READ) {
            throw new RuntimeException("管线资源类型为Sampler2D但访问类型不为只读");
        }
        resource.put(description.name(), description);
        return this;
    }

    public Map<String, GlPipelineResourceDescription> resource() {
        return resource;
    }

    public GlPipelineResourceDescriptions clone() {
        GlPipelineResourceDescriptions n = new GlPipelineResourceDescriptions();
        n.resource.putAll(this.resource);
        return n;
    }

}