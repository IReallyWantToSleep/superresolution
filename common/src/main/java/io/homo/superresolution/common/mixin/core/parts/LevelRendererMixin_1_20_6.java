package io.homo.superresolution.common.mixin.core.parts;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

#if MC_VER == MC_1_20_6
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin_1_20_6 {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevel(float partialTick, long nanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
}
#else
@Mixin(LevelRenderer.class)
public class LevelRendererMixin_1_20_6 {}
#endif