package io.homo.superresolution.common.render.gl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.utils.RenderTargetBindPoint;
import io.homo.superresolution.common.render.impl.IDebuggableObject;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.RenderTargetCache;
import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_UNDEFINED;
import static org.lwjgl.opengl.GL30C.glCheckFramebufferStatus;

public class GlFrameBuffer implements IFrameBuffer, IDebuggableObject {
    private final float[] clearColor = {0, 0, 0, 0};
    private final ArrayList<FrameBufferAttachment> attachments = new ArrayList<>();
    private FrameBufferAttachment colorAttachment = null;
    private FrameBufferAttachment depthAttachment = null;
    private FrameBufferAttachment depthStencilAttachment = null;

    private int frameBufferId = glGenFramebuffers();
    private int width;
    private int height;

    public GlFrameBuffer() {

    }

    public static @NotNull GlFrameBuffer create(TextureFormat colorTextureFormat, TextureFormat depthTextureFormat, int width, int height) {
        GlFrameBuffer frameBuffer = new GlFrameBuffer();
        frameBuffer.width = width;
        frameBuffer.height = height;
        frameBuffer.addAttachment(new FrameBufferAttachment(
                FrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                GlTexture.create(width, height, colorTextureFormat)
        ));
        frameBuffer.addAttachment(new FrameBufferAttachment(
                depthTextureFormat.isStencil() ?
                        FrameBufferAttachment.FrameBufferAttachmentType.DEPTH_STENCIL :
                        FrameBufferAttachment.FrameBufferAttachmentType.DEPTH,
                GlTexture.create(width, height, depthTextureFormat)
        ));
        frameBuffer.validate();
        return frameBuffer;
    }

    public static @NotNull GlFrameBuffer create(ITexture colorTexture, ITexture depthTexture, int width, int height) {
        GlFrameBuffer frameBuffer = new GlFrameBuffer();
        frameBuffer.width = width;
        frameBuffer.height = height;
        frameBuffer.addAttachment(new FrameBufferAttachment(
                FrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                colorTexture
        ));
        frameBuffer.addAttachment(new FrameBufferAttachment(
                depthTexture.getTextureFormat().isStencil() ?
                        FrameBufferAttachment.FrameBufferAttachmentType.DEPTH_STENCIL :
                        FrameBufferAttachment.FrameBufferAttachmentType.DEPTH,
                depthTexture
        ));
        frameBuffer.validate();
        return frameBuffer;
    }

    public static @NotNull GlFrameBuffer create(int width, int height) {
        return create(
                GlTexture.create(width, height, TextureFormat.RGBA8),
                GlTexture.create(width, height, TextureFormat.DEPTH24),
                width,
                height
        );
    }

    public static @NotNull GlFrameBuffer create() {
        return create(
                GlTexture.create(1, 1, TextureFormat.RGBA8),
                GlTexture.create(1, 1, TextureFormat.DEPTH24),
                1,
                1
        );
    }

    public static @NotNull GlFrameBuffer create(ITexture colorTexture, ITexture depthTexture) {
        return create(
                colorTexture,
                depthTexture,
                colorTexture.getWidth(),
                colorTexture.getHeight()
        );
    }

    public static int resolveBindTarget(RenderTargetBindPoint point) {
        return switch (point) {
            case READ -> GL_READ_FRAMEBUFFER;
            case WRITE -> GL_DRAW_FRAMEBUFFER;
            case ALL -> GL_FRAMEBUFFER;
        };
    }

    public void addAttachment(FrameBufferAttachment attachment) {
        bind(RenderTargetBindPoint.ALL);
        if (attachment.type == FrameBufferAttachment.FrameBufferAttachmentType.COLOR) {
            colorAttachment = attachment;
        } else if (attachment.type == FrameBufferAttachment.FrameBufferAttachmentType.DEPTH) {
            depthAttachment = attachment;
        } else {
            depthStencilAttachment = attachment;
        }
        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                attachment.type.attachmentId(),
                GL_TEXTURE_2D,
                attachment.texture.getTextureId(),
                0
        );
        attachments.add(attachment);
        updateDebugLabel(getDebugLabel());
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

    @Override
    public int getTextureId(FrameBufferAttachmentType attachmentType) {
        return switch (attachmentType) {
            case COLOR -> colorAttachment != null ? colorAttachment.texture.getTextureId() : -1;
            case DEPTH -> depthAttachment != null ? depthAttachment.texture.getTextureId() : -1;
            case DEPTH_STENCIL -> depthStencilAttachment != null ? depthStencilAttachment.texture.getTextureId() : -1;
        };
    }

    @Override
    public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
        return switch (attachmentType) {
            case COLOR -> colorAttachment != null ? colorAttachment.texture : null;
            case DEPTH -> depthAttachment != null ? depthAttachment.texture : null;
            case DEPTH_STENCIL -> depthStencilAttachment != null ? depthStencilAttachment.texture : null;
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
            String errorDesc = switch (status) {
                case GL_FRAMEBUFFER_UNDEFINED -> "UNDEFINED";
                case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "INCOMPLETE_ATTACHMENT";
                case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "MISSING_ATTACHMENT";
                case GL_FRAMEBUFFER_UNSUPPORTED -> "UNSUPPORTED_FORMAT";
                default -> "UNKNOWN_ERROR";
            };
            throw new IllegalStateException("FBO validation failed: " + errorDesc + " (0x" + Integer.toHexString(status) + ")");
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
    public void clearFrameBuffer() {
        bind(RenderTargetBindPoint.ALL);
        glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
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
    public TextureFormat getColorTextureFormat() {
        if (colorAttachment == null) return null;
        return colorAttachment.texture.getTextureFormat();
    }

    @Override
    public TextureFormat getDepthTextureFormat() {
        if (depthAttachment != null) {
            return depthAttachment.texture.getTextureFormat();
        } else if (depthStencilAttachment != null) {
            return depthStencilAttachment.texture.getTextureFormat();
        }
        return null;
    }

    @Override
    public RenderTarget asMcRenderTarget() {
        return RenderTargetCache.cacheOf(this);
    }

    @Override
    public void resizeFrameBuffer(int width, int height) {
        for (FrameBufferAttachment attachment : attachments) {
            attachment.texture.resize(width, height);
        }
        glDeleteFramebuffers(this.frameBufferId);
        this.frameBufferId = glGenFramebuffers();
        this.width = width;
        this.height = height;
        ArrayList<FrameBufferAttachment> temp = new ArrayList<>(attachments);
        attachments.clear();
        for (FrameBufferAttachment attachment : temp) {
            addAttachment(attachment);
        }
        validate();
        updateDebugLabel(getDebugLabel());
    }

    @Override
    public String getDebugLabel() {
        return "FrameBuffer-%s|Color-%s|Depth-%s|DepthStencil-%s"
                .formatted(
                        getFrameBufferId(),
                        colorAttachment != null ? colorAttachment.texture.string() : "None",
                        depthAttachment != null ? depthAttachment.texture.string() : "None",
                        depthStencilAttachment != null ? depthStencilAttachment.texture.string() : "None"
                );
    }

    @Override
    public void updateDebugLabel(String newLabel) {
        glSafeObjectLabel(GL_FRAMEBUFFER, getFrameBufferId(), newLabel);
    }
}