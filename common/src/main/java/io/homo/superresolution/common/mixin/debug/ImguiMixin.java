/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.common.mixin.debug;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.debug.imgui.ImguiMain;
import io.homo.superresolution.api.platform.Platform;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ImguiMixin {
    #if MC_VER < MC_1_21_5
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;unbindWrite()V"), method = "runTick")
    private void onRender(CallbackInfo ci) {
                if (!(Platform.currentPlatform.isDevelopmentEnvironment() && SuperResolutionConfig.isEnableImgui())) return;
        if (ImguiMain.getInstance() != null) {
            ImguiMain.getInstance().render();
        }
    }
    #elif MC_VER > MC_1_21_11
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"), method = "renderFrame")
    private void onRender(CallbackInfo ci) {
        if (!(Platform.currentPlatform.isDevelopmentEnvironment() && SuperResolutionConfig.isEnableImgui())) return;
        if (ImguiMain.getInstance() != null) {
            ImguiMain.getInstance().render();
        }
    }
    #else
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"), method = "runTick")
    private void onRender(CallbackInfo ci) {
        if (!(Platform.currentPlatform.isDevelopmentEnvironment() && SuperResolutionConfig.isEnableImgui())) return;
        if (ImguiMain.getInstance() != null) {
            ImguiMain.getInstance().render();
        }
    }
    #endif

    @Inject(at = @At(value = "HEAD"), method = "close")
    private void onExit(CallbackInfo ci) {
        if (!(Platform.currentPlatform.isDevelopmentEnvironment() && SuperResolutionConfig.isEnableImgui())) return;
        if (ImguiMain.getInstance() != null) {
            ImguiMain.getInstance().destroy();
        }
    }
}
