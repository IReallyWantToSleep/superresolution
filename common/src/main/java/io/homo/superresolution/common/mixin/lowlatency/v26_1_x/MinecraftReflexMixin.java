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

package io.homo.superresolution.common.mixin.lowlatency.v26_1_x;

#if MC_VER >= MC_26_1 && MC_VER < MC_26_2
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.lowlatency.LowLatencyMode;
import net.minecraft.client.FramerateLimiter;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftReflexMixin {
	@Inject(
		method = "renderFrame(Z)V",
		at = @At("HEAD"),
		require = 1
	)
	private void super_resolution$beginRenderSubmit(boolean advanceGameTime, CallbackInfo ci) {
		if (!LowLatency.isAvailable()) {
			return;
		}
		LowLatency.endSimulation();
	}

	@Redirect(
		method = "renderFrame(Z)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/FramerateLimiter;limitDisplayFPS(I)V",
			ordinal = 0
		),
		require = 1
	)
	private void super_resolution$limitDisplayFps(int framerateLimit) {
		if (!(LowLatency.isAvailable() && LowLatency.frameLimitUs() != 0 && LowLatency.mode() != LowLatencyMode.None) || !SuperResolution.gameIsLoaded) {
			FramerateLimiter.limitDisplayFPS(framerateLimit);
		}
	}
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class MinecraftReflexMixin {
}
#endif
