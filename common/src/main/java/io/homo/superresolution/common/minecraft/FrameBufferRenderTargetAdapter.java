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

package io.homo.superresolution.common.minecraft;


import com.mojang.blaze3d.pipeline.RenderTarget;
#if MC_VER >= MC_1_21_6
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
#endif
import io.homo.superresolution.core.graphics.opengl.utils.GlBlitRenderer;


#if MC_VER > MC_1_21_4
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL46;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;

public class FrameBufferRenderTargetAdapter extends RenderTarget {
    private IFrameBuffer frameBuffer;
    private GpuTextureAdapter colorTextureAdapter;
    private GpuTextureAdapter depthTextureAdapter;
    private long cachedColorTextureHandle = -1;
    private long cachedDepthTextureHandle = -1;
    private long cachedFrameBufferHandle = -1;

    #if MC_VER >= MC_1_21_6
    private GpuTextureView colorTextureView;
    private GpuTextureView depthTextureView;

    #endif
    FrameBufferRenderTargetAdapter(IFrameBuffer frameBuffer) {
        super(frameBuffer.handle() + "-IFrameBuffer-" + frameBuffer.getTextureId(FrameBufferAttachmentType.Color), frameBuffer.getDepthTextureFormat() != null);
        this.frameBuffer = frameBuffer;
        updateState();
    }

    protected static FrameBufferRenderTargetAdapter ofRenderTarget(IFrameBuffer frameBuffer) {
        return new FrameBufferRenderTargetAdapter(frameBuffer);
    }

    #if MC_VER >= MC_1_21_6
    @javax.annotation.Nullable
    public GpuTextureView getColorTextureView() {
        return this.colorTextureView;
    }

    @javax.annotation.Nullable
    public GpuTextureView getDepthTextureView() {
        return this.depthTextureView;
    }

    #endif
    private void updateState() {
        this.width = frameBuffer.getWidth();
        this.height = frameBuffer.getHeight();
        #if !(MC_VER > MC_1_21_6)
        this.viewWidth = frameBuffer.getWidth();
        this.viewHeight = frameBuffer.getHeight();
        #endif

        long currentFbHandle = frameBuffer.handle();
        ITexture colorTex = frameBuffer.getTexture(FrameBufferAttachmentType.Color);
        long colorTexHandle = colorTex.handle();

        if (colorTextureAdapter == null || cachedColorTextureHandle != colorTexHandle || cachedFrameBufferHandle != currentFbHandle) {
            colorTextureAdapter = (GpuTextureAdapter) GpuTextureAdapter.ofTexture(colorTex);
            colorTextureAdapter.bindFramebuffer(frameBuffer);
            cachedColorTextureHandle = colorTexHandle;
            #if MC_VER >= MC_1_21_6
            this.colorTextureView = RenderSystem.getDevice().createTextureView(colorTextureAdapter);
            #endif
        }
        this.colorTexture = colorTextureAdapter;

        ITexture depthTex = frameBuffer.getTexture(FrameBufferAttachmentType.DepthStencil);
        if (depthTex == null) {
            depthTex = frameBuffer.getTexture(FrameBufferAttachmentType.Depth);
        }

        if (depthTex != null) {
            long depthTexHandle = depthTex.handle();
            if (depthTextureAdapter == null || cachedDepthTextureHandle != depthTexHandle || cachedFrameBufferHandle != currentFbHandle) {
                depthTextureAdapter = (GpuTextureAdapter) GpuTextureAdapter.ofTexture(depthTex);
                depthTextureAdapter.bindFramebuffer(frameBuffer);
                cachedDepthTextureHandle = depthTexHandle;
                #if MC_VER >= MC_1_21_6
                this.depthTextureView = RenderSystem.getDevice().createTextureView(depthTextureAdapter);
                #endif
            }
            this.depthTexture = depthTextureAdapter;
        } else {
            depthTextureAdapter = null;
            cachedDepthTextureHandle = -1;
            this.depthTexture = null;
        }

        cachedFrameBufferHandle = currentFbHandle;
    }


    public void resize(int i, int j) {
        updateState();
    }

    public void destroyBuffers() {
        updateState();
    }

    public void copyDepthFrom(@NotNull RenderTarget renderTarget) {
        updateState();
        super.copyDepthFrom(renderTarget);
    }

    public void createBuffers(int i, int j) {
        updateState();
    }

    public void setFilterMode(FilterMode filterMode) {
        updateState();
    }

    private void setFilterMode(FilterMode filterMode, boolean bl) {
        updateState();
    }

    public void blitToScreen() {
        updateState();
        Gl.DSA.blitFramebuffer(
                (int) frameBuffer.handle(),
                new GlState(GlState.STATE_DRAW_FBO).wFbo,
                0, 0, frameBuffer.getWidth(), frameBuffer.getHeight(),
                0, 0, frameBuffer.getWidth(), frameBuffer.getHeight(),
                GL46.GL_COLOR_BUFFER_BIT, GL46.GL_NEAREST
        );
    }

    public void blitAndBlendToTexture(GpuTexture gpuTexture) {
        updateState();
        #if MC_VER > MC_1_21_6
        GlBlitRenderer.blitToScreen(
                frameBuffer.getTexture(FrameBufferAttachmentType.Color),
                this.width,
                this.height
        );
        #else
        GlBlitRenderer.blitToScreen(
                frameBuffer.getTexture(FrameBufferAttachmentType.Color),
                this.viewWidth,
                this.viewHeight
        );
        #endif
    }

    @Nullable
    public GpuTexture getColorTexture() {
        updateState();
        return this.colorTexture;
    }

    @Nullable
    public GpuTexture getDepthTexture() {
        updateState();
        return this.depthTexture;
    }

    public FrameBufferRenderTargetAdapter bindFrameBuffer(IFrameBuffer frameBuffer) {
        if (this.frameBuffer != frameBuffer) {
            this.frameBuffer = frameBuffer;
            this.cachedFrameBufferHandle = -1;
        }
        return this;
    }


}
#else
import com.mojang.blaze3d.platform.GlStateManager;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;

class FrameBufferRenderTargetAdapter extends RenderTarget {
    private IBindableFrameBuffer frameBuffer;

    FrameBufferRenderTargetAdapter(IBindableFrameBuffer frameBuffer) {
        super(frameBuffer.getDepthTextureFormat() != null);
        this.frameBuffer = frameBuffer;
        updateState();
    }

    protected static FrameBufferRenderTargetAdapter ofRenderTarget(IBindableFrameBuffer frameBuffer) {
        return new FrameBufferRenderTargetAdapter(frameBuffer);
    }

    public FrameBufferRenderTargetAdapter bindFrameBuffer(IBindableFrameBuffer frameBuffer) {
        this.frameBuffer = frameBuffer;
        return this;
    }

    private void updateState() {
        this.width = frameBuffer.getWidth();
        this.height = frameBuffer.getHeight();
        this.viewWidth = frameBuffer.getWidth();
        this.viewHeight = frameBuffer.getHeight();
        this.frameBufferId = Math.toIntExact(frameBuffer.handle());
        this.colorTextureId = frameBuffer.getTextureId(FrameBufferAttachmentType.Color);
        this.depthBufferId = frameBuffer.getTextureId(FrameBufferAttachmentType.DepthStencil) == -1 ? frameBuffer.getTextureId(FrameBufferAttachmentType.Depth) : frameBuffer.getTextureId(FrameBufferAttachmentType.DepthStencil);
    }


    public void bindRead() {
        updateState();
        frameBuffer.bind(FrameBufferBindPoint.Read);
    }

    public void unbindRead() {
        updateState();
        frameBuffer.unbind(FrameBufferBindPoint.Read);

    }

    public void bindWrite(boolean setViewport) {
        updateState();
        frameBuffer.bind(FrameBufferBindPoint.Write, setViewport);

    }

    public void unbindWrite() {
        updateState();
        frameBuffer.unbind(FrameBufferBindPoint.Write);
    }

    public void setClearColor(float red, float green, float blue, float alpha) {
        updateState();
        frameBuffer.setClearColorRGBA(red, green, blue, alpha);
    }

    public void blitToScreen(int width, int height) {
        updateState();
        GlBlitRenderer.blitToScreen(
                frameBuffer.getTexture(FrameBufferAttachmentType.Color),
                this.viewWidth,
                this.viewHeight
        );
    }

    public void blitAndBlendToScreen(int width, int height) {
        updateState();
        blitToScreen(width, height);
    }

    #if MC_VER < MC_1_21_4
    public void clear(boolean a) {
        updateState();
        frameBuffer.clearFrameBuffer();
    }

    public void resize(int width, int height, boolean clearError) {
    }

    public void createBuffers(int width, int height, boolean clearError) {
        updateState();
    }
    #else
    public void clear() {
        updateState();
        frameBuffer.clearFrameBuffer();
    }

    public void resize(int width, int height) {
    }

    public void createBuffers(int width, int height) {
        updateState();
    }
    #endif

    public int getColorTextureId() {
        updateState();
        return frameBuffer.getTextureId(FrameBufferAttachmentType.Color);
    }

    public int getDepthTextureId() {
        updateState();
        return frameBuffer.getTextureId(FrameBufferAttachmentType.DepthStencil) == -1 ? frameBuffer.getTextureId(FrameBufferAttachmentType.Depth) : frameBuffer.getTextureId(FrameBufferAttachmentType.DepthStencil);
    }

    public void destroyBuffers() {
        updateState();
    }

    public void copyDepthFrom(RenderTarget otherTarget) {
        updateState();
        GlStateManager._glBindFramebuffer(36008, otherTarget.frameBufferId);
        GlStateManager._glBindFramebuffer(36009, this.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, otherTarget.width, otherTarget.height, 0, 0, this.width, this.height, 256, 9728);
        GlStateManager._glBindFramebuffer(36160, 0);
    }

}
#endif
