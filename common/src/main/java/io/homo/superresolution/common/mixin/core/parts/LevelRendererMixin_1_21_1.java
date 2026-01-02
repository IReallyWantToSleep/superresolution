package io.homo.superresolution.common.mixin.core.parts;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

#if MC_VER == MC_1_21_1
import io.homo.superresolution.common.upscale.AlgorithmManager;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin_1_21_1 {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevel(
            net.minecraft.client.DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            net.minecraft.client.Camera camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
}
#else
@Mixin(LevelRenderer.class)
public class LevelRendererMixin_1_21_1 {}
#endif