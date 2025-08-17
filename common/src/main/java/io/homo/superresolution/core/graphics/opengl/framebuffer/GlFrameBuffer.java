package io.homo.superresolution.core.graphics.opengl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.minecraft.RenderTargetCache;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.IDebuggableObject;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL30.*;

public class GlFrameBuffer implements IBindableFrameBuffer, IDebuggableObject {
    private final float[] clearColor = {0, 0, 0, 0};
    private final ArrayList<GlFrameBufferAttachment> attachments = new ArrayList<>();
    private GlFrameBufferAttachment colorAttachment = null;
    private GlFrameBufferAttachment depthAttachment = null;
    private GlFrameBufferAttachment depthStencilAttachment = null;

    private int frameBufferId = Gl.DSA.createFramebuffer();
    private int width;
    private int height;

    public void label(String label) {
        this.label = label;
    }

    private String label;

    public GlFrameBuffer() {

    }

    public static @NotNull GlFrameBuffer create(TextureFormat colorTextureFormat, TextureFormat depthTextureFormat, int width, int height) {
        GlFrameBuffer frameBuffer = new GlFrameBuffer();
        frameBuffer.width = width;
        frameBuffer.height = height;
        frameBuffer.addAttachment(new GlFrameBufferAttachment(
                        GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                        RenderSystems.current().device().createTexture(
                                TextureDescription.create()
                                        .type(TextureType.Texture2D)
                                        .format(colorTextureFormat)
                                        .width(width)
                                        .height(height)
                                        .usages(TextureUsages.create().storage().sampler().attachmentColor())
                                        .build()
                        )
                )
        );
        if (depthTextureFormat != null) frameBuffer.addAttachment(new GlFrameBufferAttachment(
                depthTextureFormat.isStencil() ?
                        GlFrameBufferAttachment.FrameBufferAttachmentType.DEPTH_STENCIL :
                        GlFrameBufferAttachment.FrameBufferAttachmentType.DEPTH,
                RenderSystems.current().device().createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .format(depthTextureFormat)
                                .width(width)
                                .height(height)
                                .usages(TextureUsages.create().storage().sampler().attachmentDepth())
                                .build()
                )
        ));
        frameBuffer.validate();
        return frameBuffer;
    }

    public static @NotNull GlFrameBuffer create(ITexture colorTexture, ITexture depthTexture, int width, int height) {
        GlFrameBuffer frameBuffer = new GlFrameBuffer();
        frameBuffer.width = width;
        frameBuffer.height = height;
        frameBuffer.addAttachment(new GlFrameBufferAttachment(
                GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                colorTexture
        ));
        if (depthTexture != null) frameBuffer.addAttachment(new GlFrameBufferAttachment(
                depthTexture.getTextureFormat().isStencil() ?
                        GlFrameBufferAttachment.FrameBufferAttachmentType.DEPTH_STENCIL :
                        GlFrameBufferAttachment.FrameBufferAttachmentType.DEPTH,
                depthTexture
        ));
        frameBuffer.validate();
        return frameBuffer;
    }

    public static @NotNull GlFrameBuffer create(int width, int height) {
        return create(
                RenderSystems.current().device().createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .format(TextureFormat.RGBA8)
                                .width(width)
                                .height(height)
                                .usages(TextureUsages.create().storage().sampler().attachmentColor())
                                .build()
                ),
                RenderSystems.current().device().createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .format(TextureFormat.DEPTH24)
                                .width(width)
                                .height(height)
                                .usages(TextureUsages.create().storage().sampler().attachmentDepth())
                                .build()
                ),
                width,
                height
        );
    }

    public static @NotNull GlFrameBuffer create() {
        return create(
                RenderSystems.current().device().createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .format(TextureFormat.RGBA8)
                                .width(1)
                                .height(1)
                                .usages(TextureUsages.create().storage().sampler().attachmentColor())
                                .build()
                ),
                RenderSystems.current().device().createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .format(TextureFormat.DEPTH24)
                                .width(1)
                                .height(1)
                                .usages(TextureUsages.create().storage().sampler().attachmentDepth())
                                .build()
                ),
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

    public static int resolveBindTarget(FrameBufferBindPoint point) {
        return switch (point) {
            case Read -> GL_READ_FRAMEBUFFER;
            case Write -> GL_DRAW_FRAMEBUFFER;
            case All -> GL_FRAMEBUFFER;
        };
    }

    public void addAttachment(GlFrameBufferAttachment attachment) {
        if (attachment.type == GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR) {
            colorAttachment = attachment;
        } else if (attachment.type == GlFrameBufferAttachment.FrameBufferAttachmentType.DEPTH) {
            depthAttachment = attachment;
        } else {
            depthStencilAttachment = attachment;
        }
        Gl.DSA.framebufferTexture(
                this.frameBufferId,
                attachment.type.attachmentId(),
                (int) attachment.texture.handle(),
                0
        );
        attachments.add(attachment);
        updateDebugLabel(getDebugLabel());
    }

    @Override
    public void destroy() {
        if (frameBufferId != -1) {
            Gl.DSA.deleteFramebuffer(frameBufferId);
            frameBufferId = -1;
        }
    }

    public void validate() {
        int status = Gl.DSA.checkNamedFramebufferStatus(
                frameBufferId,
                GL_FRAMEBUFFER
        );
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
        if (colorAttachment != null) {
            Gl.DSA.clearNamedFramebufferfv(
                    frameBufferId,
                    GL_COLOR,
                    0,
                    clearColor
            );
        }
        if (depthAttachment != null) {
            Gl.DSA.clearNamedFramebufferfv(
                    frameBufferId,
                    GL_DEPTH,
                    0,
                    new float[]{1.0f}
            );
        }
        if (depthStencilAttachment != null) {
            Gl.DSA.clearNamedFramebufferfi(
                    frameBufferId,
                    GL_DEPTH_STENCIL,
                    0,
                    1.0f,
                    0
            );
        }
    }

    @Override
    public void resizeFrameBuffer(int width, int height) {
        if (width < 1 || height < 1) {
            throw new RuntimeException("%s %s".formatted(width, height));
        }

        for (GlFrameBufferAttachment attachment : attachments) {
            attachment.texture.resize(width, height);
        }
        Gl.DSA.deleteFramebuffer(frameBufferId);

        this.frameBufferId = Gl.DSA.createFramebuffer();
        this.width = width;
        this.height = height;
        ArrayList<GlFrameBufferAttachment> temp = new ArrayList<>(attachments);
        attachments.clear();
        for (GlFrameBufferAttachment attachment : temp) {
            addAttachment(attachment);
        }
        validate();
        updateDebugLabel(getDebugLabel());
    }

    @Override
    public void bind(FrameBufferBindPoint bindPoint, boolean setViewport) {
        int target = resolveBindTarget(bindPoint);
        glBindFramebuffer(target, frameBufferId);
        if (setViewport) glViewport(0, 0, width, height);
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
    public int getTextureId(FrameBufferAttachmentType attachmentType) {
        return (int) switch (attachmentType) {
            case Color -> colorAttachment != null ? colorAttachment.texture.handle() : -1;
            case Depth -> depthAttachment != null ? depthAttachment.texture.handle() : -1;
            case DepthStencil -> depthStencilAttachment != null ? depthStencilAttachment.texture.handle() : -1;
            case AnyDepth -> depthStencilAttachment != null ?
                    depthStencilAttachment.texture.handle() :
                    depthAttachment != null ? depthAttachment.texture.handle() : -1;
        };
    }

    @Override
    public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
        return switch (attachmentType) {
            case Color -> colorAttachment != null ? colorAttachment.texture : null;
            case Depth -> depthAttachment != null ? depthAttachment.texture : null;
            case DepthStencil -> depthStencilAttachment != null ? depthStencilAttachment.texture : null;
            case AnyDepth -> depthStencilAttachment != null ?
                    depthStencilAttachment.texture :
                    depthAttachment != null ? depthAttachment.texture : null;
        };
    }

    @Override
    public void setClearColorRGBA(float r, float g, float b, float a) {
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
    public long handle() {
        return frameBufferId;
    }

    @Override
    public String getDebugLabel() {
        return label != null ? label : "FrameBuffer-%s|Color-%s|Depth-%s|DepthStencil-%s"
                .formatted(
                        handle(),
                        colorAttachment != null ? colorAttachment.texture.string() : "None",
                        depthAttachment != null ? depthAttachment.texture.string() : "None",
                        depthStencilAttachment != null ? depthStencilAttachment.texture.string() : "None"
                );
    }

    @Override
    public void updateDebugLabel(String newLabel) {
        GlDebug.objectLabel(GL_FRAMEBUFFER, (int) handle(), newLabel);
    }
}
