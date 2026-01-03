package io.homo.superresolution.common.mixin.core.parts;


import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.renderer.LevelRenderer;
#if MC_VER > MC_1_21_5 && MC_VER < MC_1_21_9
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.joml.Vector4f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin_1_21_678 {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevel(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f frustumMatrix, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
}

#else
@Mixin(LevelRenderer.class)
public class LevelRendererMixin_1_21_678 {
}
#endif