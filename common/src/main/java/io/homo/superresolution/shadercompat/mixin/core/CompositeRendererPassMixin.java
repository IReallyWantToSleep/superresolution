package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.shadercompat.NamedCompositePass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = {"net.irisshaders.iris.pipeline.CompositeRenderer$Pass"})
public class CompositeRendererPassMixin implements NamedCompositePass {
    @Unique
    private String superresolution$name0;

    #if MC_VER < MC_1_21_1
    @Override
    public String superresolution$getName() {
        return superresolution$name0;
    }

    @Override
    public void superresolution$setName(String name) {
        superresolution$name0 = name;
    }
    #else
    @Shadow
    String name;

    @Override
    public String superresolution$getName() {
        return name;
    }

    @Override
    public void superresolution$setName(String name) {
        this.name = name;
    }
    #endif
}
