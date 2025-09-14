/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.neoforge.mixin.compat.simpleclouds;

import org.spongepowered.asm.mixin.Mixin;

#if MC_VER == MC_1_21_1
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.ShaderSupportPipeline;
import io.homo.superresolution.api.SuperResolutionAPI;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ShaderSupportPipeline.class, remap = false)
public class ShaderSupportPipelineMixin {
    @Redirect(method = "afterSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V"))
    public void bindWrite(RenderTarget instance, boolean setViewport) {
        instance.bindWrite(true);
    }

    @Redirect(method = "afterLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V"))
    public void afterLevel_blitToScreen(RenderTarget instance, int width, int height, boolean disableBlend) {
        instance.blitToScreen(
                SuperResolutionAPI.getScreenWidth(),
                SuperResolutionAPI.getScreenHeight(),
                disableBlend
        );
        GlStateManager._viewport(0, 0, SuperResolutionAPI.getRenderWidth(), SuperResolutionAPI.getRenderHeight());
    }

    @Redirect(method = "afterSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V"))
    public void afterSky_blitToScreen(RenderTarget instance, int width, int height, boolean disableBlend) {
        instance.blitToScreen(
                SuperResolutionAPI.getScreenWidth(),
                SuperResolutionAPI.getScreenHeight(),
                disableBlend
        );
        GlStateManager._viewport(0, 0, SuperResolutionAPI.getRenderWidth(), SuperResolutionAPI.getRenderHeight());
    }
}
#else
import net.minecraft.client.Minecraft;
@Mixin(value = Minecraft.class)
public class ShaderSupportPipelineMixin {
}
#endif