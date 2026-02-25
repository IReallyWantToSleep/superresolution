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

package io.homo.superresolution.common.upscale.none;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.impl.framebuffer.*;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.opengl.Gl;
import org.lwjgl.opengl.GL43;

import java.util.List;

import static io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer.resolveBindTarget;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.*;

public class None extends AbstractAlgorithm {
    private static int cachedFrameBufferId = -1;
    private static OnlyNameFramebuffer cachedFrameBuffer;


    @Override
    public void initialize(InitializationDescription desc) {
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        if (cachedFrameBufferId < 0 || Gl.DSA.checkNamedFramebufferStatus(cachedFrameBufferId, GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            cachedFrameBufferId = Gl.DSA.createFramebuffer();
        }
        Gl.DSA.framebufferTexture(
                cachedFrameBufferId,
                GL43.GL_COLOR_ATTACHMENT0,
                (int) dispatchResource.resources().colorTexture().handle(),
                0
        );
        cachedFrameBuffer = new OnlyNameFramebuffer(
                cachedFrameBufferId,
                dispatchResource.resources().colorTexture()
        );
        return true;
    }

    public void destroy() {
        if (cachedFrameBufferId > 0) {
            Gl.DSA.deleteFramebuffer(cachedFrameBufferId);
        }
    }

    public void resize(int width, int height) {
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return cachedFrameBuffer;
    }

    public int getOutputTextureId() {
        return cachedFrameBuffer == null ? 0 : cachedFrameBuffer.getTextureId(FrameBufferAttachmentType.Color);
    }

    private static class OnlyNameFramebuffer implements IBindableFrameBuffer {
        private final int fboId;
        private final ITexture colorTex;

        public OnlyNameFramebuffer(
                int fboId,
                ITexture colorTex
        ) {
            this.fboId = fboId;
            this.colorTex = colorTex;
        }

        @Override
        public void bind(FrameBufferBindPoint bindPoint, boolean setViewport) {
            int target = resolveBindTarget(bindPoint);
            glBindFramebuffer(target, fboId);
            if (setViewport) {
                glViewport(0, 0, colorTex.getWidth(), colorTex.getHeight());
            }
        }

        @Override
        public void bind(FrameBufferBindPoint bindPoint) {
            bind(bindPoint, true);
        }

        @Override
        public void unbind(FrameBufferBindPoint bindPoint) {
            glBindFramebuffer(resolveBindTarget(bindPoint), 0);
        }

        @Override
        public int getWidth() {
            return colorTex.getWidth();
        }

        @Override
        public int getHeight() {
            return colorTex.getHeight();
        }

        @Override
        public void clearFrameBuffer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ColorAttachment> getColorAttachments() {
            return List.of(new ColorAttachment(0, colorTex));
        }

        @Override
        public DepthStencilAttachment getDepthStencilAttachment() {
            return null;
        }

        @Override
        public void resizeFrameBuffer(int width, int height) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getTextureId(FrameBufferAttachmentType attachmentType) {
            if (attachmentType == FrameBufferAttachmentType.Color) {
                return Math.toIntExact(colorTex.handle());
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
            if (attachmentType == FrameBufferAttachmentType.Color) {
                return colorTex;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public void setClearColorRGBA(float red, float green, float blue, float alpha) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TextureFormat getColorTextureFormat() {
            return colorTex.getTextureFormat();
        }

        @Override
        public TextureFormat getDepthTextureFormat() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long handle() {
            return fboId;
        }

        @Override
        public void destroy() {
        }
    }
}
