package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PostChain.class)
public interface PostChainAccessor {
    @Mutable
    @Accessor(value = "screenTarget")
    void setScreenTarget(RenderTarget renderTarget);

    @Mutable
    @Accessor(value = "screenWidth")
    void setScreenWidth(int screenWidth);

    @Mutable
    @Accessor(value = "screenHeight")
    void setScreenHeight(int screenHeight);
}
