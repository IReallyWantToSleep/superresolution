/*
 * Anemone Mod
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

package io.homo.superresolution.common.mixin.framegeneration.v1_21_11;

#if MC_VER >= MC_1_21_11 && MC_VER < MC_26_1
import io.homo.superresolution.common.framegeneration.constants.FGConstantsFeature;
import io.homo.superresolution.common.framegeneration.constants.MinecraftCameraState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = GameRenderer.class)
public class GameRendererConstantsMixin{
	@Inject(
			method = "renderLevel",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(F)Lorg/joml/Matrix4f;",ordinal = 0),
			require = 1,
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void anemone$fov(
			DeltaTracker deltaTracker,
			CallbackInfo ci,
			float f,
			LocalPlayer localplayer,
			ProfilerFiller profilerfiller,
			boolean flag,
			float f1 //fov
	) {
		MinecraftCameraState.fov = f1;
	}

	@Inject(
		method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
		at = @At("HEAD"),
		require = 1
	)
	private void anemone$beginConstantsFrame(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
		FGConstantsFeature.beginRenderFrame();
	}

	@Inject(
		method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
		at = @At("RETURN"),
		require = 1
	)
	private void anemone$endConstantsFrame(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
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

