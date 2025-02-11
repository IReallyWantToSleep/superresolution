package io.homo.superresolution.common.render.gl.framebuffer;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;

public class StorageFrameBuffer extends FrameBuffer {
    public StorageFrameBuffer(boolean useDepth) {
        super(useDepth);
    }
    /*
    @Override
    public void createBuffers(int width, int height, boolean clearError) {
        RenderSystem.assertOnRenderThreadOrInit();
        int maxSupportedTextureSize = RenderSystem.maxSupportedTextureSize();
        if (width > 0 && width <= maxSupportedTextureSize && height > 0 && height <= maxSupportedTextureSize) {
            this.viewWidth = width;
            this.viewHeight = height;
            this.width = width;
            this.height = height;
            this.frameBufferId = glGenFramebuffers();
            this.colorTextureId = TextureUtil.generateTextureId();
            this.filterMode = GL_NEAREST;
            glBindFramebuffer(GL_FRAMEBUFFER, this.frameBufferId);

            if (this.useDepth) {
                this.depthBufferId = TextureUtil.generateTextureId();
                glBindTexture(GL_TEXTURE_2D, this.depthBufferId);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_NONE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH32F_STENCIL8, this.width, this.height);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.depthBufferId, 0);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_TEXTURE_2D, this.depthBufferId, 0);

            }

            glBindTexture(GL_TEXTURE_2D, this.colorTextureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, this.width, this.height);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorTextureId, 0);

            this.checkStatus();
            this.clear(clearError);
            this.unbindRead();
        } else {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + maxSupportedTextureSize + ")");
        }
    }*/
}