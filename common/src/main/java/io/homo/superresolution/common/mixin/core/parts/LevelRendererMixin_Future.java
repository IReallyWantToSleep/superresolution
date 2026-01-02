package io.homo.superresolution.common.mixin.core.parts;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.renderer.LevelRenderer;

#if MC_VER >= MC_1_21_9
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin_Future {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevel(
            GraphicsResourceAllocator allocator, 
            DeltaTracker deltaTracker, 
            boolean renderBlockOutline, 
            Camera camera, 
            Matrix4f matrix4f1, 
            Matrix4f projectionMatrix, 
            Matrix4f frustumMatrix, 
            GpuBufferSlice bufferSlice, 
            Vector4f vector4f, 
            boolean bool2, 
            CallbackInfo ci
    ) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
}
#endif