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

package io.homo.superresolution.neoforge.mixin.compat.iris;


import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

#if MC_VER > MC_1_21_5
import net.minecraft.client.Minecraft;
import net.irisshaders.iris.targets.Blaze3dRenderTargetExt;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

#endif
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class IrisRenderingPipelineMixin {
    @Shadow
    @Final
    private PackDirectives packDirectives;

    @Shadow
    @Final
    private RenderTargets renderTargets;

    #if MC_VER > MC_1_21_5
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void replaceRenderTarget(ProgramSet programSet, CallbackInfo ci) {
        RenderTarget main = RenderHandlerManager.getRenderTarget().asMcRenderTarget();
        GpuTexture depthTexture = main.getDepthTexture();
        DepthBufferFormat depthBufferFormat = DepthBufferFormat.fromGlEnumOrDefault(GlConst.toGlInternalId(main.getDepthTexture().getFormat()));
        this.renderTargets.resizeIfNeeded(((Blaze3dRenderTargetExt) main).iris$getDepthBufferVersion(), depthTexture, main.width, main.height, depthBufferFormat, this.packDirectives);
    }

    @Redirect(method = "beginLevelRendering", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    public RenderTarget replaceRenderTarget_(Minecraft instance) {
        return RenderHandlerManager.getRenderTarget().asMcRenderTarget();
    }

    @Redirect(method = "finalizeGameRendering", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    public RenderTarget replaceRenderTarget__(Minecraft instance) {
        return RenderHandlerManager.getRenderTarget().asMcRenderTarget();
    }
    #endif
}
