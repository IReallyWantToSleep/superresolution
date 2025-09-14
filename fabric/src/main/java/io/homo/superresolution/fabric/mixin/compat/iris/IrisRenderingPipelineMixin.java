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

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class IrisRenderingPipelineMixin {
    #if MC_VER > MC_1_21_5
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"), remap = true)
    public RenderTarget replaceRenderTarget(Minecraft instance) {
        return RenderHandlerManager.getRenderTarget().asMcRenderTarget();
    }

    @Redirect(method = "beginLevelRendering", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"), remap = true)
    public RenderTarget replaceRenderTarget_(Minecraft instance) {
        return RenderHandlerManager.getRenderTarget().asMcRenderTarget();
    }

    @Redirect(method = "finalizeGameRendering", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"), remap = true)
    public RenderTarget replaceRenderTarget__(Minecraft instance) {
        return RenderHandlerManager.getRenderTarget().asMcRenderTarget();
    }
    #endif
}
