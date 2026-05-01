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

package io.homo.irisapi.mixin.composite.v1_21_1;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.irisapi.IrisCompositePassType;
import io.homo.irisapi.IrisReflectionUtils;
import io.homo.irisapi.NamedCompositePass;
import io.homo.irisapi.PassEventHandler;
import io.homo.irisapi.handlers.IrisRenderingPipelineHandler;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Objects;

@Mixin(CompositeRenderer.class)
public class CompositeRendererMixin {
    #if MC_VER >= MC_1_20_1 && MC_VER <= MC_1_21_4
    @Shadow(remap = false)
    @Final
    private ImmutableList<Object> passes;

    @Unique
    private Object superresolution$getPass(int passIndex) {
        if (passIndex >= 0 && passIndex < this.passes.size()) {
            Object pass = this.passes.get(passIndex);
            return pass;
        }
        return null;
    }

    @Unique
    private void superresolution$handlePassEvent(int passIndex, PassEventHandler handler) {
        Object pass = superresolution$getPass(passIndex);
        Objects.requireNonNull(pass);
        handler.handle(
                (CompositeRenderer) (Object) this,
                (NamedCompositePass) pass,
                IrisReflectionUtils.getCompositePassType(pass)
        );
    }

    //===========PassStart============//
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/ImmutableList;get(I)Ljava/lang/Object;",
            ordinal = 0
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onPassStart(
            CallbackInfo ci,
            RenderTarget main,
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
            int i
    ) {
        //当运行计算着色器时在调用计算着色器前触发BeforeRender
        IrisCompositePassType passType = IrisReflectionUtils.getCompositePassType(superresolution$getPass(i));
        if (passType != IrisCompositePassType.Common) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchBefore);
        }
    }

    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/pathways/FullScreenQuadRenderer;renderQuad()V",
            ordinal = 0
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onBeforeRenderA(
            CallbackInfo ci,
            RenderTarget main,
            int i
    ) {
        //当运行不计算着色器时在绘制全屏三角形前触发BeforeRender
        IrisCompositePassType passType = IrisReflectionUtils.getCompositePassType(superresolution$getPass(i));
        if (passType == IrisCompositePassType.Common) {
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
            int i
    ) {
        IrisCompositePassType passType = IrisReflectionUtils.getCompositePassType(superresolution$getPass(i));
        if (passType == IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchAfter);
        }
    }

    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/pathways/FullScreenQuadRenderer;renderQuad()V",
            shift = At.Shift.AFTER
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onAfterRenderA(
            CallbackInfo ci,
            RenderTarget main,
            int i
    ) {
        IrisCompositePassType passType = IrisReflectionUtils.getCompositePassType(superresolution$getPass(i));
        if (passType != IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchAfter);
        }
    }

    //===========PassEnd============//
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/GLDebug;popGroup()V",
            shift = At.Shift.AFTER,
            ordinal = 0
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onPassEnd(
            CallbackInfo ci,
            RenderTarget main,
            int i,
            int passesSize
    ) {
        IrisCompositePassType passType = IrisReflectionUtils.getCompositePassType(superresolution$getPass(i));
        if (passType == IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassEnd);
        }
    }

    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/GLDebug;popGroup()V",
            shift = At.Shift.AFTER,
            ordinal = 1
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onPassEndA(
            CallbackInfo ci,
            RenderTarget main,
            int i,
            int passesSize
    ) {
        IrisCompositePassType passType = IrisReflectionUtils.getCompositePassType(superresolution$getPass(i));
        if (passType != IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassEnd);
        }
    }
    #endif
}
