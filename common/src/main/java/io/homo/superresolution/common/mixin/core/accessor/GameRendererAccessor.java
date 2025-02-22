package io.homo.superresolution.common.mixin.core.accessor;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor(value = "postEffect")
    PostChain getPostEffect();

    @Accessor(value = "blurEffect")
    PostChain getBlurEffect();
}
