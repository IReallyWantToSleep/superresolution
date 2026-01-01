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

package io.homo.superresolution.fabric.mixin.compat.iris;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.graphics.opengl.Gl;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
#if MC_VER > MC_1_20_6
import net.irisshaders.iris.pbr.TextureInfoCache;
#else
import net.irisshaders.iris.texture.TextureInfoCache;
#endif
#if MC_VER > MC_1_21_4
import com.mojang.blaze3d.textures.GpuTexture;
#endif
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL45.*;

@Mixin(value = GlFramebuffer.class, remap = false)
public abstract class GlFramebufferMixin extends GlResource {
    @Unique
    private int super_resolution$currentDepthAttachmentType = 0;

    public GlFramebufferMixin() {
        super(0);
    }

    @Shadow
    public abstract int getStatus();

    @Inject(method = "addDepthAttachment", at = @At("RETURN"))
    #if MC_VER < MC_1_21_5
    private void checkFboCompleteness(int texture, CallbackInfo ci) {
    #else
    private void checkFboCompleteness(GpuTexture texture, CallbackInfo ci) {
    #endif
        /*
        int status = Gl.DSA.checkNamedFramebufferStatus(
                getGlId(),
                GL_FRAMEBUFFER
        );
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            SuperResolution.LOGGER.error("FBO不完整 CODE:{}", status);
        }
        */
    }

    @Inject(method = "addDepthAttachment", at = @At("HEAD"))
    #if MC_VER < MC_1_21_5
    public void addDepthAttachment(int texture, CallbackInfo ci)
    #else
    public void addDepthAttachment(GpuTexture texture, CallbackInfo ci)
    #endif {
        /*
        if (super_resolution$currentDepthAttachmentType != 0) {
            IrisRenderSystem.framebufferTexture2D(
                    getGlId(),
                    GL_FRAMEBUFFER,
                    super_resolution$currentDepthAttachmentType,
                    GL_TEXTURE_2D,
                    0,
                    0
            );
        }
        */
        #if MC_VER < MC_1_21_5
        super_resolution$currentDepthAttachmentType = super_resolution$detectAttachmentType(TextureInfoCache.INSTANCE.getInfo(texture).getInternalFormat());
        #else
        super_resolution$currentDepthAttachmentType = super_resolution$detectAttachmentType(TextureInfoCache.INSTANCE.getInfo(((com.mojang.blaze3d.opengl.GlTexture) texture).glId()).getInternalFormat());
        #endif
    }

    @Unique
    private int super_resolution$detectAttachmentType(int format) {
        return DepthBufferFormat.fromGlEnumOrDefault(format).isCombinedStencil() ? GL_DEPTH_STENCIL_ATTACHMENT : GL_DEPTH_ATTACHMENT;
    }
}