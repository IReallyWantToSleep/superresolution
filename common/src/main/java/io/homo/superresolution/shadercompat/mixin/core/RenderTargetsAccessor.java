package io.homo.superresolution.shadercompat.mixin.core;

import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTargets.class)
public interface RenderTargetsAccessor {
    @Accessor(value = "destroyed", remap = false)
    boolean isDestroyed();
}
