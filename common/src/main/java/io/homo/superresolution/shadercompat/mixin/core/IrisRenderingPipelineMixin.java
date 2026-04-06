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

package io.homo.superresolution.shadercompat.mixin.core;

import com.mojang.blaze3d.textures.GpuTexture;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.pathways.colorspace.ColorSpaceConverter;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IrisRenderingPipeline.class)
public class IrisRenderingPipelineMixin {
    @Inject(method = "beginLevelRendering", at = @At("HEAD"), remap = false)
    private void beginLevelRendering(CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(
                (Matrix4f) CapturedRenderingState.INSTANCE.getGbufferProjection(),
                (Matrix4f) CapturedRenderingState.INSTANCE.getGbufferModelView()
        );
    }

    @Redirect(method = "beginLevelRendering", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/pathways/colorspace/ColorSpaceConverter;rebuildProgram(IILnet/irisshaders/iris/pathways/colorspace/ColorSpace;)V"), remap = false)
    private void replaceRebuildColorSpaceConvertShaderParameters(
            ColorSpaceConverter instance,
            int width,
            int height,
            ColorSpace colorSpace
    ) {
        instance.rebuildProgram(
                SuperResolutionAPI.getScreenWidth(),
                SuperResolutionAPI.getScreenHeight(),
                colorSpace
        );
    }

    @Unique
    private Object lastDepthTexture = null;

    @Redirect(method = "beginLevelRendering", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/targets/RenderTargets;resizeIfNeeded(ILcom/mojang/blaze3d/textures/GpuTexture;IILnet/irisshaders/iris/gl/texture/DepthBufferFormat;Lnet/irisshaders/iris/shaderpack/properties/PackDirectives;)Z"), remap = false)
    private boolean patch(
            RenderTargets instance,
            int newDepthBufferVersion,
            GpuTexture newDepthTextureId,
            int newWidth,
            int newHeight,
            DepthBufferFormat newDepthFormat,
            PackDirectives packDirectives
    ) {
        //由于SR在窗口大小更改后会resize framebuffer（sr自己的）
        //然后内部逻辑是销毁先前的texture对象并创建一个新的texture对象（GlTexture2D wrap GpuTextureAdapter）
        //但是framebuffer对象不会变化，iris依靠newDepthBufferVersion检测变化，最后的结果就是iris并不知道深度纹理变了
        //于是它获取到的depthTexture.id就是-1（SR销毁老的纹理后自动设置的）
        //最后创烂游戏渲染
        //这个patch是补充了：
        //framebuffer对象不会变化，但它的depthTexture会变的情况下的检测逻辑

        //首次beginLevelRendering，lastDepthTexture为null，直接调用resizeIfNeeded并记录newDepthTextureId（实为GpuTexture obj）
        if (lastDepthTexture == null){
            instance.resizeIfNeeded(newDepthBufferVersion, newDepthTextureId, newWidth, newHeight, newDepthFormat, packDirectives);
            lastDepthTexture = newDepthTextureId;
            return true;
        }
        //后续beginLevelRendering，如果和当前的newDepthTextureId不一样了，说明SR重新创建了depthTexture
        //我们就调用resizeIfNeeded并存一下这个新的的depthTexture，否则就是原版逻辑
        if (!lastDepthTexture.equals(newDepthTextureId)){
            instance.resizeIfNeeded(newDepthBufferVersion, newDepthTextureId, newWidth, newHeight, newDepthFormat, packDirectives);
            lastDepthTexture = newDepthTextureId;
            return true;
        }
        return instance.resizeIfNeeded(newDepthBufferVersion, newDepthTextureId, newWidth, newHeight, newDepthFormat, packDirectives);
    }
}
