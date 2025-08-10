package io.homo.superresolution.shadercompat.mixin.core;

import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IrisRenderingPipeline.class)
public interface IrisRenderingPipelineAccessor {
    @Accessor(value = "compositeRenderer", remap = false)
    CompositeRenderer getCompositeRenderer();
}
