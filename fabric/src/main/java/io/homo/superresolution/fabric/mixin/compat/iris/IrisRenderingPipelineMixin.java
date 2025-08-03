package io.homo.superresolution.fabric.mixin.compat.iris;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class IrisRenderingPipelineMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    public RenderTarget replaceRenderTarget(Minecraft instance) {
        return MinecraftRenderHandle.getRenderTarget().asMcRenderTarget();
    }

}
