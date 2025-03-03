package io.homo.superresolution.common.mixin.core.accessor;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor(value = "postEffect")
    PostChain getPostEffect();

    @Invoker(value = "getFov")
    double getFov_(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting);
}
