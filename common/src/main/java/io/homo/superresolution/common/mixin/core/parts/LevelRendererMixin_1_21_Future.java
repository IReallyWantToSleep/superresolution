package io.homo.superresolution.common.mixin.core.parts;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

#if MC_VER == MC_1_21_9 || MC_VER == MC_1_21_10
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
public class LevelRendererMixin_1_21_Future {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevel(GraphicsResourceAllocator p_361796_, DeltaTracker p_348530_, boolean p_109603_, Camera p_109604_, Matrix4f p_254120_, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice p_425977_, Vector4f p_425544_, boolean p_426302_, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
}
#else
@Mixin(LevelRenderer.class)
public class LevelRendererMixin_1_21_Future {}
#endif