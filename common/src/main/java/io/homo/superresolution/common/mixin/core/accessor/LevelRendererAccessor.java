package io.homo.superresolution.common.mixin.core.accessor;


import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    #if MC_VER < MC_1_21_4
    @Accessor(value = "entityEffect")
    PostChain getEntityEffect();

    #endif
    #if MC_VER < MC_1_21_4
    @Accessor(value = "entityTarget")
    #else
    @Accessor(value = "entityOutlineTarget")
    #endif
    RenderTarget getEntityRenderTarget();

}
