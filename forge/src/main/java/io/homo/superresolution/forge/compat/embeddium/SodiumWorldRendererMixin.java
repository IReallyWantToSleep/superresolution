package io.homo.superresolution.forge.compat.embeddium;

import com.mojang.blaze3d.vertex.PoseStack;
import io.homo.superresolution.upscale.AlgorithmManager;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {
    @Inject(method = "drawChunkLayer",at= @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT,remap = false)
    public void compatSodium(RenderType renderLayer, PoseStack matrixStack, double x, double y, double z, CallbackInfo ci, ChunkRenderMatrices matrices) {
        AlgorithmManager.param.poseStack = matrixStack;
        AlgorithmManager.setModelViewMatrix((Matrix4f) matrices.modelView());
        AlgorithmManager.setProjectionMatrix((Matrix4f) matrices.projection());
    }
}
