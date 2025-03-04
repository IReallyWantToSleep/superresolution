package io.homo.superresolution.common.render.gl.framebuffer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;

public class MotionVectorsFrameBuffer extends GlFrameBuffer {

    public MotionVectorsFrameBuffer(boolean useDepth) {
        super(useDepth);
    }

    @Override
    public void createBuffers(int width, int height, boolean clearError) {
        RenderSystem.assertOnRenderThreadOrInit();
        this.viewWidth = width;
        this.viewHeight = height;
        this.width = width;
        this.height = height;
        this.frameBufferId = GlStateManager.glGenFramebuffers();
        this.colorTextureId = TextureUtil.generateTextureId();
        this.setFilterMode(GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, this.colorTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RG16F, this.width, this.height);
        glBindFramebuffer(GL_FRAMEBUFFER, this.frameBufferId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorTextureId, 0);
        this.checkStatus();
        this.clear(clearError);
        this.unbindRead();
    }
}