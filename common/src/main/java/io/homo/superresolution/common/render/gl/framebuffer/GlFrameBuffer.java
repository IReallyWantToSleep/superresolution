package io.homo.superresolution.common.render.gl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.RenderTargetBindPoint;
import io.homo.superresolution.common.render.gl.GlConst;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;

import java.util.ArrayList;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;
import static org.lwjgl.opengl.GL30C.glCheckFramebufferStatus;

public class GlFrameBuffer implements IFrameBuffer {
    private final float[] clearColor = {0, 0, 0, 0};
    private final ArrayList<FrameBufferAttachment> attachments = new ArrayList<>();
    private FrameBufferAttachment colorAttachment;
    private FrameBufferAttachment depthAttachment;
    private int frameBufferId = glGenFramebuffers();
    private int width;
    private int height;

    public void addAttachment(FrameBufferAttachment attachment) {
        bind(RenderTargetBindPoint.ALL);
        if (attachment.type == FrameBufferAttachment.FrameBufferAttachmentType.COLOR) {
            colorAttachment = attachment;
        } else {
            depthAttachment = attachment;
        }
        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                attachment.type.attachmentId(),
                GL_TEXTURE_2D,
                attachment.texture.getTextureId(),
                0
        );
    }


    @Override
    public void bind(RenderTargetBindPoint bindPoint, boolean setViewport) {
        int target = resolveBindTarget(bindPoint);
        glBindFramebuffer(target, frameBufferId);
        if (setViewport) glViewport(0, 0, width, height);
    }

    @Override
    public void bind(RenderTargetBindPoint bindPoint) {
        bind(bindPoint, true);
    }

    @Override
    public void unbind(RenderTargetBindPoint bindPoint) {
        glBindFramebuffer(resolveBindTarget(bindPoint), 0);
    }

    private int resolveBindTarget(RenderTargetBindPoint point) {
        return switch (point) {
            case READ -> GlConst.GL_READ_FRAMEBUFFER;
            case WRITE -> GlConst.GL_DRAW_FRAMEBUFFER;
            case ALL -> GL_FRAMEBUFFER;
        };
    }

    @Override
    public void destroy() {
        if (frameBufferId != -1) {
            glDeleteFramebuffers(frameBufferId);
            frameBufferId = -1;
        }
    }

    public void validate() {
        bind(RenderTargetBindPoint.WRITE);
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Incomplete FBO: 0x" +
                    Integer.toHexString(status));
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void clear() {
        bind(RenderTargetBindPoint.ALL);
        glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
    }

    @Override
    public int getColorTextureId() {
        if (colorAttachment == null) throw new RuntimeException();
        return colorAttachment.texture.getTextureId();
    }

    @Override
    public int getDepthTextureId() {
        if (depthAttachment == null) throw new RuntimeException();
        return depthAttachment.texture.getTextureId();
    }

    @Override
    public int getFrameBufferId() {
        return frameBufferId;
    }

    @Override
    public void setClearColor(float r, float g, float b, float a) {
        clearColor[0] = r;
        clearColor[1] = g;
        clearColor[2] = b;
        clearColor[3] = a;
    }

    @Override
    public RenderTarget asMcRenderTarget() {
        return null;
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}