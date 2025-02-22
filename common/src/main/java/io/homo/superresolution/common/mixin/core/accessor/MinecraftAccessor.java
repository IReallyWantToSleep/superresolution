package io.homo.superresolution.common.mixin.core.accessor;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Minecraft.class)
public interface MinecraftAccessor {
    @Mutable
    @Accessor(value = "mainRenderTarget")
    void setRenderTarget(RenderTarget renderTarget);
}
