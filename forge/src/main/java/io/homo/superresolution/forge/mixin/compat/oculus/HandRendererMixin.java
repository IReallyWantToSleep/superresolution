package io.homo.superresolution.forge.mixin.compat.oculus;

import com.mojang.blaze3d.vertex.PoseStack;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HandRenderer.class, remap = false)
public class HandRendererMixin {
    /*
    @Inject(method = "renderSolid", at = @At("HEAD"))
    public void renderSolidB(PoseStack poseStack, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderHandBegin();
        }
    }

    @Inject(method = "renderSolid", at = @At("RETURN"))
    public void renderSolidE(PoseStack poseStack, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderHandEnd();
        }
    }

    @Inject(method = "renderTranslucent", at = @At("HEAD"))
    public void renderTranslucentB(PoseStack poseStack, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderHandBegin();
        }
    }

    @Inject(method = "renderTranslucent", at = @At("RETURN"))
    public void renderTranslucentE(PoseStack poseStack, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderHandEnd();
        }
    }*/
}