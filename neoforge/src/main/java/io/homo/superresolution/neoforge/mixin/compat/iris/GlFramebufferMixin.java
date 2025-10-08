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

import io.homo.superresolution.common.SuperResolution;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.pbr.TextureInfoCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL46.*;

@Mixin(value = GlFramebuffer.class, remap = false)
public abstract class GlFramebufferMixin extends GlResource {
    @Unique
    private int super_resolution$currentDepthAttachmentType = 0;

    public GlFramebufferMixin() {
        super(0);
    }

    @Shadow
    public abstract int getStatus();

    #if MC_VER < MC_1_21_5
    @Inject(method = "addDepthAttachment", at = @At("RETURN"))
    private void checkFboCompleteness(int texture, CallbackInfo ci) {
        int status = io.homo.superresolution.core.graphics.opengl.Gl.DSA.checkNamedFramebufferStatus(
                getGlId(),
                GL_FRAMEBUFFER
        );
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            SuperResolution.LOGGER.error("FBO不完整 CODE:{}", status);
        }
    }

    @Inject(method = "addDepthAttachment", at = @At("HEAD"))
    public void addDepthAttachment(int texture, CallbackInfo ci) {
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

        super_resolution$currentDepthAttachmentType = super_resolution$detectAttachmentType(TextureInfoCache.INSTANCE.getInfo(texture).getInternalFormat());
    }
    #else
    @Inject(method = "addDepthAttachment", at = @At("RETURN"))
    private void checkFboCompleteness(com.mojang.blaze3d.textures.GpuTexture texture, CallbackInfo ci) {
        int status = getStatus();
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            SuperResolution.LOGGER.error("FBO不完整 CODE:{}", status);
        }
    }

    @Inject(method = "addDepthAttachment", at = @At("HEAD"))
    public void addDepthAttachment(com.mojang.blaze3d.textures.GpuTexture texture, CallbackInfo ci) {
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

        super_resolution$currentDepthAttachmentType = super_resolution$detectAttachmentType(TextureInfoCache.INSTANCE.getInfo(((com.mojang.blaze3d.opengl.GlTexture) texture).glId()).getInternalFormat());
    }
    #endif

    @Unique
    private int super_resolution$detectAttachmentType(int format) {
        return DepthBufferFormat.fromGlEnumOrDefault(format).isCombinedStencil() ? GL_DEPTH_STENCIL_ATTACHMENT : GL_DEPTH_ATTACHMENT;
    }
}