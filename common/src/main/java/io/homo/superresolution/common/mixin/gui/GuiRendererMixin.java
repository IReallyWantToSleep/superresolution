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

package io.homo.superresolution.common.mixin.gui;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.homo.superresolution.common.gui.CustomActionRenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

#if MC_VER > MC_1_21_5
@Mixin(net.minecraft.client.gui.render.GuiRenderer.class)
public class GuiRendererMixin {
    @Inject(method = "executeDraw", at = @At("HEAD"), cancellable = true)
    public void executeDraw(
            @Coerce
            Object draw,
            com.mojang.blaze3d.systems.RenderPass renderPass,
            com.mojang.blaze3d.buffers.GpuBuffer buffer,
            VertexFormat.IndexType indexType,
            CallbackInfo ci
    ) {
        if (((GuiRendererDrawAccessor) draw).getPipeline() instanceof CustomActionRenderPipeline) {
            ((CustomActionRenderPipeline) ((GuiRendererDrawAccessor) draw).getPipeline()).getAction().run();
            ci.cancel();
        }
    }
}

#else
@Mixin(net.minecraft.client.Minecraft.class)
public class GuiRendererMixin {

}
#endif