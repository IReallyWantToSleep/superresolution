package io.homo.superresolution.fsr2.pipelines;

import io.homo.superresolution.core.gl.pipeline.GlPipeline;
import io.homo.superresolution.fsr2.Fsr2Context;
import io.homo.superresolution.fsr2.Fsr2Dimensions;
import io.homo.superresolution.fsr2.Fsr2PipelineResources;
import io.homo.superresolution.fsr2.Fsr2PipelineResourcesDescription;

import java.util.HashMap;
import java.util.Map;

public abstract class Fsr2BasePipeline {
    protected final Fsr2Context context;
    protected final Map<String, Integer> shaderBindingMap = new HashMap<>();
    public GlPipeline pipeline = new GlPipeline();

    public Fsr2BasePipeline(Fsr2Context context) {
        this.context = context;
        this.shaderBindings();
    }

    public Map<String, Integer> getShaderBindings() {
        return shaderBindingMap;
    }

    public abstract void resize(Fsr2Dimensions size);

    public abstract void destroy();

    public abstract void init();

    protected abstract void shaderBindings();

    protected Fsr2PipelineResourcesDescription getResourcesDescription(String name) {
        if (!shaderBindingMap.containsKey(name)) throw new RuntimeException(name);
        if (!context.resources.getShaderNameResourceMap().containsKey(name)) throw new RuntimeException(name);
        return context.resources.getShaderNameResourceMap().get(name);
    }
}
