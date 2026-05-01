/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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