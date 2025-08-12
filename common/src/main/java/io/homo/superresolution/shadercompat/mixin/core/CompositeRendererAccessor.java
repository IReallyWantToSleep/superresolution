package io.homo.superresolution.shadercompat.mixin.core;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompositeRenderer.class)
public interface CompositeRendererAccessor {
    @Accessor(value = "renderTargets", remap = false)
    RenderTargets getRenderTargets();

    @Accessor(value = "passes", remap = false)
    ImmutableList<Object> getPasses();
}
