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

package io.homo.irisapi.mixin.composite.before1_21_1;

import io.homo.irisapi.IrisCompositePassType;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import org.spongepowered.asm.mixin.Mixin;

#if MC_VER < MC_1_19_2
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.BufferFlipper;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.spongepowered.asm.mixin.Final;

import java.util.ListIterator;
import java.util.Set;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.irisapi.IrisReflectionUtils;
import io.homo.irisapi.NamedCompositePass;
import io.homo.irisapi.PassEventHandler;
import io.homo.irisapi.handlers.IrisRenderingPipelineHandler;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.pathways.CenterDepthSampler;

#endif
@Mixin(CompositeRenderer.class)
public class CompositeRendererMixin {
    #if MC_VER < MC_1_19_2
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

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void superresolution$assignPassNames(
            net.irisshaders.iris.pipeline.WorldRenderingPipeline pipeline,
            PackDirectives packDirectives,
            ProgramSource[] sources,
            ComputeSource[][] computes,
            RenderTargets renderTargets,
            ShaderStorageBufferHolder holder,
            TextureAccess noiseTexture,
            FrameUpdateNotifier updateNotifier,
            CenterDepthSampler centerDepthSampler,
            BufferFlipper bufferFlipper,
            Supplier<ShadowRenderTargets> shadowTargetsSupplier,
            TextureStage textureStage,
            Object2ObjectMap<String, TextureAccess> customTextureIds,
            Object2ObjectMap<String, TextureAccess> irisCustomTextures,
            Set<GlImage> customImages,
            com.google.common.collect.ImmutableMap<Integer, Boolean> explicitPreFlips,
            CustomUniforms customUniforms,
            CallbackInfo ci
    ) {
        for (int i = 0; i < sources.length && i < this.passes.size(); ++i) {
            ProgramSource source = sources[i];
            String name = null;

            if (source != null && source.isValid()) {
                name = source.getName();
            } else if (computes != null && computes.length > i && computes[i] != null && computes[i].length > 0 && computes[i][0] != null) {
                name = computes[i][0].getName();
            }

            if (name == null) {
                name = "composite" + i;
            }

            ((NamedCompositePass) this.passes.get(i)).superresolution$setName(name);
        }
    }

    //===========PassStart============//
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Ljava/util/Iterator;next()Ljava/lang/Object;",
            ordinal = 0,
            shift = At.Shift.AFTER
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onPassStart(
            CallbackInfo ci,
            RenderTarget main,
            UnmodifiableIterator<?> var2
    ) {
        int passIndex = Math.max(((ListIterator<?>) var2).previousIndex(), 0);
        superresolution$handlePassEvent(passIndex, IrisRenderingPipelineHandler::onCompositePassStart);
    }

    //===========BeforeRender============//
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Ljava/util/Iterator;next()Ljava/lang/Object;",
            ordinal = 0,
            shift = At.Shift.AFTER
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onBeforeRender(
            CallbackInfo ci,
            RenderTarget main,
            UnmodifiableIterator<?> var2
    ) {
        int passIndex = Math.max(((ListIterator<?>) var2).previousIndex(), 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) != IrisCompositePassType.Common) {
            superresolution$handlePassEvent(passIndex, IrisRenderingPipelineHandler::onCompositePassDispatchBefore);
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
            UnmodifiableIterator<?> var2
    ) {
        int passIndex = Math.max(((ListIterator<?>) var2).previousIndex(), 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) == IrisCompositePassType.Common) {
            superresolution$handlePassEvent(passIndex, IrisRenderingPipelineHandler::onCompositePassDispatchBefore);
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
            UnmodifiableIterator<?> var2
    ) {
        int passIndex = Math.max(((ListIterator<?>) var2).previousIndex(), 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) == IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(passIndex, IrisRenderingPipelineHandler::onCompositePassDispatchAfter);
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
            UnmodifiableIterator<?> var2
    ) {
        int passIndex = Math.max(((ListIterator<?>) var2).previousIndex(), 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) != IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(passIndex, IrisRenderingPipelineHandler::onCompositePassDispatchAfter);
        }
    }

    //===========PassEnd============//
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/program/Program;unbind()V",
            shift = At.Shift.AFTER,
            ordinal = 0
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onPassEnd(
            CallbackInfo ci,
            RenderTarget main,
            UnmodifiableIterator<?> var2
    ) {
        int passIndex = Math.max(((java.util.ListIterator<?>) var2).previousIndex(), 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) == IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(passIndex, IrisRenderingPipelineHandler::onCompositePassEnd);
        }
    }

    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/blending/BlendModeOverride;restore()V",
            shift = At.Shift.AFTER,
            ordinal = 0
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    private void onPassEndA(
            CallbackInfo ci,
            RenderTarget main,
            UnmodifiableIterator<?> var2
    ) {
        int passIndex = Math.max(((java.util.ListIterator<?>) var2).previousIndex(), 0);
        if (IrisReflectionUtils.getCompositePassType(this.passes.get(passIndex)) != IrisCompositePassType.ComputeOnly) {
            superresolution$handlePassEvent(passIndex, IrisRenderingPipelineHandler::onCompositePassEnd);
        }
    }
    #endif
}
