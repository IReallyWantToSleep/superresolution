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

package io.homo.superresolution.fabric.mixin.compat.iris;

import io.homo.superresolution.common.compat.iris.IrisFramebufferUtils;
import net.irisshaders.iris.compat.dh.DHCompatInternal;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DHCompatInternal.class, remap = false)
public class DHCompatInternalMixin {
    @Shadow
    private GlFramebuffer dhWaterFramebuffer;

    @Shadow
    private GlFramebuffer dhTerrainFramebuffer;

    @Inject(method = "reconnectDHTextures", at = @At("RETURN"))
    public void fixDHDepth(int depthTex, CallbackInfo ci) {
        if (dhTerrainFramebuffer != null && IrisFramebufferUtils.getFramebufferDepthAttachment(dhWaterFramebuffer.getId()) != depthTex) {
            reconnectTextures(depthTex);
        }

        if (dhWaterFramebuffer != null && IrisFramebufferUtils.getFramebufferDepthAttachment(dhWaterFramebuffer.getId()) != depthTex) {
            reconnectTextures(depthTex);
        }
    }

    private void reconnectTextures(int depthTex) {
        #if MC_VER < MC_1_21_5
        if (dhTerrainFramebuffer != null) {
            dhTerrainFramebuffer.addDepthAttachment(depthTex);
        }
        if (dhWaterFramebuffer != null) {
            dhWaterFramebuffer.addDepthAttachment(depthTex);
        }
        #else
        if (dhTerrainFramebuffer != null) {
            dhTerrainFramebuffer.addDepthAttachmentBypass(depthTex);
        }
        if (dhWaterFramebuffer != null) {
            dhWaterFramebuffer.addDepthAttachmentBypass(depthTex);
        }
        #endif
    }
}
