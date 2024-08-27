package io.homo.superresolution.resolutioncontrol.mixin;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Minecraft.class)
public interface MinecraftAccessor{
    @Mutable
    @Accessor(value = "mainRenderTarget")
    void setFramebuffer(RenderTarget framebuffer);
}
