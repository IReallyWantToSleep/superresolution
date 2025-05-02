package io.homo.superresolution.common.minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;
#if MC_VER < MC_1_21_4
import net.minecraft.client.Minecraft;
#endif

import static io.homo.superresolution.core.gl.Gl.*;
import static io.homo.superresolution.core.gl.GlConst.*;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;

#if MC_VER < MC_1_21_5
public class LegacyStorageFrameBuffer extends RenderTarget implements IFrameBuffer {
    private int colorAttachment1 = -1;
    private boolean stencilEnabled = false;

    public LegacyStorageFrameBuffer(boolean useDepth) {
        super(useDepth);
    }

    @Override
    #if MC_VER > MC_1_21_1
    public void createBuffers(int width, int height)
    #else
    public void createBuffers(int width, int height, boolean clearError)
    #endif {
        RenderSystem.assertOnRenderThreadOrInit();
        int maxSupportedTextureSize = RenderSystem.maxSupportedTextureSize();
        if (width > 0 && width <= maxSupportedTextureSize && height > 0 && height <= maxSupportedTextureSize) {
            this.viewWidth = width;
            this.viewHeight = height;
            this.width = width;
            this.height = height;
            this.filterMode = GL_NEAREST;

            this.frameBufferId = glGenFramebuffers();
            this.colorTextureId = TextureUtil.generateTextureId();
            this.depthBufferId = TextureUtil.generateTextureId();
            this.colorAttachment1 = TextureUtil.generateTextureId();

            glBindFramebuffer(GL_FRAMEBUFFER, this.frameBufferId);
            //depth
            glBindTexture(GL_TEXTURE_2D, this.depthBufferId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_NONE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            if (stencilEnabled) {
                glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH24_STENCIL8, this.width, this.height);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_TEXTURE_2D, this.depthBufferId, 0);
            } else {
                glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH_COMPONENT24, this.width, this.height);
            }
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.depthBufferId, 0);
            //color0
            glBindTexture(GL_TEXTURE_2D, this.colorTextureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, this.width, this.height);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorTextureId, 0);

            //color1 运动矢量用
            glBindTexture(GL_TEXTURE_2D, this.colorAttachment1);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexStorage2D(GL_TEXTURE_2D, 1, TextureFormat.RG16F.gl(), this.width, this.height);

            this.checkStatus();
            #if MC_VER > MC_1_21_1
            this.clear();
            #else
            this.clear(clearError);
            #endif
            this.unbindRead();
        } else {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + maxSupportedTextureSize + ")");
        }
    }

    public void enableStencil() {
        if (!this.stencilEnabled) {
            this.stencilEnabled = true;
            #if MC_VER > MC_1_21_1
            this.resize(this.viewWidth, this.viewHeight);
            #else
            this.resize(this.viewWidth, this.viewHeight, Minecraft.ON_OSX);
            #endif
        }
    }

    public boolean isStencilEnabled() {
        return this.stencilEnabled;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void destroy() {
        this.destroyBuffers();
    }

    public void clearFrameBuffer() {
        #if MC_VER  < MC_1_21_4
        this.clear(Minecraft.ON_OSX);
        #else
        this.clear();
        #endif
    }

    public void resizeFrameBuffer(int width, int height) {
        #if MC_VER  < MC_1_21_4
        this.resize(width, height, Minecraft.ON_OSX);
        #else
        this.resize(width, height);
        #endif
    }

    public void bind(FrameBufferBindPoint bindPoint, boolean setViewport) {
        if (bindPoint == FrameBufferBindPoint.READ) {
            this.bindRead();
        } else {
            this.bindWrite(setViewport);
        }
    }

    public void bind(FrameBufferBindPoint bindPoint) {
        bind(bindPoint, true);
    }

    public void unbind(FrameBufferBindPoint bindPoint) {
        if (bindPoint == FrameBufferBindPoint.READ) {
            this.unbindRead();
        } else if (bindPoint == FrameBufferBindPoint.WRITE) {
            this.unbindWrite();
        } else {
            this.unbindRead();
            this.unbindWrite();
        }
    }

    @Override
    public int getTextureId(FrameBufferAttachmentType attachmentType) {
        return switch (attachmentType) {
            case COLOR -> attachmentType.getIndex() == 0 ? this.colorTextureId : this.colorAttachment1;
            case DEPTH -> this.depthBufferId;
            case DEPTH_STENCIL -> stencilEnabled ? this.depthBufferId : -1;
        };
    }

    @Override
    public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
        return FrameBufferTextureAdapter.of(this, attachmentType);
    }

    @Override
    public int getFrameBufferId() {
        return this.frameBufferId;
    }

    @Override
    public TextureFormat getColorTextureFormat() {
        return TextureFormat.RGBA8;
    }

    @Override
    public TextureFormat getDepthTextureFormat() {
        return stencilEnabled ? TextureFormat.DEPTH24_STENCIL8 : TextureFormat.DEPTH24;
    }

    @Override
    public void setClearColor(float red, float green, float blue, float alpha) {
        super.setClearColor(red, green, blue, alpha);
    }

    @Override
    public RenderTarget asMcRenderTarget() {
        return this;
    }
}
#else
public class LegacyStorageFrameBuffer {
    public LegacyStorageFrameBuffer(boolean useDepth) {
        throw new RuntimeException();
    }
}
#endif