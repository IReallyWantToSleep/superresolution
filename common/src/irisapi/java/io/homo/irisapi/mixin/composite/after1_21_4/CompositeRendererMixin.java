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

package io.homo.irisapi.mixin.composite.after1_21_4;

import io.homo.irisapi.IrisCompositePassType;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

#if MC_VER > MC_1_21_4 && MC_VER < MC_26_1
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.homo.irisapi.IrisReflectionUtils;
import io.homo.irisapi.NamedCompositePass;
import io.homo.irisapi.PassEventHandler;
import io.homo.irisapi.handlers.IrisRenderingPipelineHandler;

#endif
@Mixin(CompositeRenderer.class)
public class CompositeRendererMixin {
    #if MC_VER > MC_1_21_4 && MC_VER < MC_26_1
    @Shadow(remap = false)
    @Final
    private ImmutableList<Object> passes;

    @Unique
    private void superresolution$handlePassEvent(int passIndex, PassEventHandler handler) {
        if (passIndex >= 0 && passIndex < this.passes.size()) {
            Object pass = this.passes.get(passIndex);
            handler.handle(
                    (CompositeRenderer) (Object) this,
                    (NamedCompositePass) pass,
                    IrisReflectionUtils.getCompositePassType(pass)
            );
        }
    }

    //===========PassStart============//
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/GLDebug;pushGroup(ILjava/lang/String;)V",
            ordinal = 1
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onPassStart(
            CallbackInfo ci,
            RenderTarget main,
            GpuBuffer indices,
            VertexFormat.IndexType type,
            RenderPass renderPass,
            int i,
            int passesSize
    ) {
        superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassStart);
    }

    //===========BeforeRender============//
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/GLDebug;pushGroup(ILjava/lang/String;)V",
            ordinal = 1,
            shift = At.Shift.AFTER
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onBeforeRender(
            CallbackInfo ci,
            RenderTarget main,
            GpuBuffer indices,
            VertexFormat.IndexType type,
            RenderPass renderPass,
            int i,
            int passesSize
    ) {
        int passIndex = Math.max(i, 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) != IrisCompositePassType.Common) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchBefore);
        }
    }

    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/program/Program;unbind()V",
            ordinal = 0,
            shift = At.Shift.AFTER
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onBeforeRenderA(
            CallbackInfo ci,
            RenderTarget main,
            GpuBuffer indices,
            VertexFormat.IndexType type,
            RenderPass renderPass,
            int i,
            int passesSize
    ) {
        int passIndex = Math.max(i, 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) == IrisCompositePassType.Common) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchBefore);
        }
    }

    //===========AfterRender============//
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/program/Program;unbind()V"
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onAfterRender(
            CallbackInfo ci,
            RenderTarget main,
            GpuBuffer indices,
            VertexFormat.IndexType type,
            RenderPass renderPass,
            int i,
            int passesSize
    ) {
        int passIndex = Math.max(i, 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) == IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchAfter);
        }
    }

    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/blending/BlendModeOverride;restore()V",
            ordinal = 0
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onAfterRenderA(
            CallbackInfo ci,
            RenderTarget main,
            GpuBuffer indices,
            VertexFormat.IndexType type,
            RenderPass renderPass,
            int i,
            int passesSize
    ) {
        int passIndex = Math.max(i, 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) != IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchAfter);
        }
    }

    //===========PassEnd============//
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/GLDebug;popGroup()V",
            ordinal = 0
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onPassEnd(
            CallbackInfo ci,
            RenderTarget main,
            GpuBuffer indices,
            VertexFormat.IndexType type,
            RenderPass renderPass,
            int i,
            int passesSize
    ) {
        int passIndex = Math.max(i, 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) == IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassEnd);
        }
    }

    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/GLDebug;popGroup()V",
            ordinal = 1,
            shift = At.Shift.AFTER
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onPassEndA(
            CallbackInfo ci,
            RenderTarget main,
            GpuBuffer indices,
            VertexFormat.IndexType type,
            RenderPass renderPass,
            int i,
            int passesSize
    ) {
        int passIndex = Math.max(i, 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) == IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassEnd);
        }
    }
    #endif
}
