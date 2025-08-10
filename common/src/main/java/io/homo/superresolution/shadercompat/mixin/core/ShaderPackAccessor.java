package io.homo.superresolution.shadercompat.mixin.core;

import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ShaderPack.class)
public interface ShaderPackAccessor {
    @Accessor(remap = false)
    Map<NamespacedId, String> getDimensionMap();
}
