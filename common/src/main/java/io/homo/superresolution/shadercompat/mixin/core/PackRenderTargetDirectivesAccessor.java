package io.homo.superresolution.shadercompat.mixin.core;

import net.irisshaders.iris.shaderpack.properties.PackRenderTargetDirectives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(PackRenderTargetDirectives.class)
public interface PackRenderTargetDirectivesAccessor {
    //NMD 1.21.1不用32，1.21.11才用
    @Accessor(value = "BASELINE_SUPPORTED_RENDER_TARGETS")
    @Mutable
    static void fuckingIris(Set<Integer> shit) {

    }
}
