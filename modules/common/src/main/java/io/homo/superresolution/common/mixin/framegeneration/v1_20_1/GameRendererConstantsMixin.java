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

package io.homo.superresolution.common.mixin.framegeneration.v1_20_1;

#if MC_VER == MC_1_20_1
import com.mojang.blaze3d.vertex.PoseStack;
import io.homo.superresolution.common.framegeneration.constants.FGConstantsFeature;
import io.homo.superresolution.common.framegeneration.constants.MinecraftCameraState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = GameRenderer.class, priority = 900)
public abstract class GameRendererConstantsMixin {
    @Inject(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(D)Lorg/joml/Matrix4f;",ordinal = 0),
            require = 1,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void super_resolution$captureFov(
            float partialTicks,
            long finishTimeNano,
            PoseStack poseStack,
            CallbackInfo ci,
            boolean flag,
            Camera camera,
            PoseStack posestack,
            double d0
            //fov
    ) {
        MinecraftCameraState.fov = (float) d0;
    }

	@Inject(
		method = "render",
		at = @At("HEAD"),
		require = 1
	)
	private void super_resolution$beginConstantsFrame(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
		FGConstantsFeature.beginRenderFrame();
	}

	@Inject(
		method = "render",
		at = @At("RETURN"),
		require = 1
	)
	private void super_resolution$endConstantsFrame(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
		FGConstantsFeature.endRenderFrame();
	}
}

#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class GameRendererConstantsMixin {
}
#endif
