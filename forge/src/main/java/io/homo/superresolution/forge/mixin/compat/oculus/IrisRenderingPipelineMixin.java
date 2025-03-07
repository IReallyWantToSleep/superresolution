package io.homo.superresolution.forge.mixin.compat.oculus;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class IrisRenderingPipelineMixin {
    //@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    //public RenderTarget IrisRenderingPipeline(Minecraft instance) {
    //    return MinecraftRenderHandle.getOriginRenderTarget();
    //}
}
