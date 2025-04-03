package io.homo.superresolution.common.render.impl.framebuffer;


import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.utils.RenderTargetBindPoint;
import io.homo.superresolution.common.render.gl.utils.BlitRenderer;


#if MC_VER > MC_1_21_4
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import io.homo.superresolution.common.render.impl.texture.GpuTextureAdapter;
import io.homo.superresolution.common.render.impl.texture.ITexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrameBufferRenderTargetAdapter extends RenderTarget {
    private IFrameBuffer frameBuffer;

    FrameBufferRenderTargetAdapter(IFrameBuffer frameBuffer) {
        super(frameBuffer.getFrameBufferId() + "-IFrameBuffer-" + frameBuffer.getTextureId(FrameBufferAttachmentType.COLOR), frameBuffer.getDepthTextureFormat() != null);
        this.frameBuffer = frameBuffer;
        updateState();
    }

    protected static FrameBufferRenderTargetAdapter ofRenderTarget(IFrameBuffer frameBuffer) {
        return new FrameBufferRenderTargetAdapter(frameBuffer);
    }

    private void updateState() {
        this.width = frameBuffer.getWidth();
        this.height = frameBuffer.getHeight();
        this.viewWidth = frameBuffer.getWidth();
        this.viewHeight = frameBuffer.getHeight();
        this.colorTexture = GpuTextureAdapter.ofTexture(frameBuffer.getTexture(FrameBufferAttachmentType.COLOR));
        ((GpuTextureAdapter) this.colorTexture).bindFramebuffer(frameBuffer);
        ITexture texture = frameBuffer.getTexture(FrameBufferAttachmentType.DEPTH_STENCIL);
        if (texture != null) {
            this.depthTexture = GpuTextureAdapter.ofTexture(texture);
        } else {
            texture = frameBuffer.getTexture(FrameBufferAttachmentType.DEPTH);
            if (texture != null) {
                this.depthTexture = GpuTextureAdapter.ofTexture(texture);
            }
        }
        if (this.depthTexture != null) {
            ((GpuTextureAdapter) this.depthTexture).bindFramebuffer(frameBuffer);
        }
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
        BlitRenderer.blitToScreen(
                frameBuffer.getTextureId(FrameBufferAttachmentType.COLOR),
                this.viewWidth,
                this.viewHeight
        );
    }

    public void blitAndBlendToTexture(GpuTexture gpuTexture) {
        updateState();
        blitToScreen();
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
        this.frameBuffer = frameBuffer;
        return this;
    }


}
#else
import com.mojang.blaze3d.platform.GlStateManager;

public class FrameBufferRenderTargetAdapter extends RenderTarget {
    private IFrameBuffer frameBuffer;

    FrameBufferRenderTargetAdapter(IFrameBuffer frameBuffer) {
        super(frameBuffer.getDepthTextureFormat() != null);
        this.frameBuffer = frameBuffer;
        updateState();
    }

    protected static FrameBufferRenderTargetAdapter ofRenderTarget(IFrameBuffer frameBuffer) {
        return new FrameBufferRenderTargetAdapter(frameBuffer);
    }

    public FrameBufferRenderTargetAdapter bindFrameBuffer(IFrameBuffer frameBuffer) {
        this.frameBuffer = frameBuffer;
        return this;
    }

    private void updateState() {
        this.width = frameBuffer.getWidth();
        this.height = frameBuffer.getHeight();
        this.viewWidth = frameBuffer.getWidth();
        this.viewHeight = frameBuffer.getHeight();
        this.frameBufferId = frameBuffer.getFrameBufferId();
        this.colorTextureId = frameBuffer.getTextureId(FrameBufferAttachmentType.COLOR);
        this.depthBufferId = frameBuffer.getTextureId(FrameBufferAttachmentType.DEPTH_STENCIL) == -1 ? frameBuffer.getTextureId(FrameBufferAttachmentType.DEPTH) : frameBuffer.getTextureId(FrameBufferAttachmentType.DEPTH_STENCIL);
    }


    public void bindRead() {
        updateState();
        frameBuffer.bind(RenderTargetBindPoint.READ);
    }

    public void unbindRead() {
        updateState();
        frameBuffer.unbind(RenderTargetBindPoint.READ);

    }

    public void bindWrite(boolean setViewport) {
        updateState();
        frameBuffer.bind(RenderTargetBindPoint.WRITE, setViewport);

    }

    public void unbindWrite() {
        updateState();
        frameBuffer.unbind(RenderTargetBindPoint.WRITE);
    }

    public void setClearColor(float red, float green, float blue, float alpha) {
        updateState();
        frameBuffer.setClearColor(red, green, blue, alpha);
    }

    public void blitToScreen(int width, int height) {
        updateState();
        BlitRenderer.blitToScreen(
                frameBuffer.getTextureId(FrameBufferAttachmentType.COLOR),
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
        return frameBuffer.getTextureId(FrameBufferAttachmentType.COLOR);
    }

    public int getDepthTextureId() {
        updateState();
        return frameBuffer.getTextureId(FrameBufferAttachmentType.DEPTH_STENCIL) == -1 ? frameBuffer.getTextureId(FrameBufferAttachmentType.DEPTH) : frameBuffer.getTextureId(FrameBufferAttachmentType.DEPTH_STENCIL);
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
