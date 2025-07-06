package io.homo.superresolution.common.minecraft;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
#if MC_VER > MC_1_21_4
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

import javax.annotation.Nullable;

public class GpuTextureAdapter extends GlTexture {
    private final ITexture texture;
    private IFrameBuffer frameBuffer;

    GpuTextureAdapter(ITexture texture) {
        super(texture.handle() + "--" + texture.getTextureFormat(),
                texture.getTextureFormat() == TextureFormat.RGBA8 ?
                        com.mojang.blaze3d.textures.TextureFormat.RGBA8 :
                        com.mojang.blaze3d.textures.TextureFormat.DEPTH32,
                texture.getWidth(),
                texture.getHeight(),
                1,
                texture.handle()
        );
        this.texture = texture;
    }

    public static GlTexture ofTexture(ITexture texture) {
        return new GpuTextureAdapter(texture);
    }

    public GpuTextureAdapter bindFramebuffer(IFrameBuffer frameBuffer) {
        this.frameBuffer = frameBuffer;
        return this;
    }

    public void close() {
        if (!this.closed) {
            this.closed = true;
        }
    }

    public boolean isClosed() {
        return this.closed;
    }

    public int getFbo(DirectStateAccess directStateAccess, @Nullable GpuTexture gpuTexture) {
        return frameBuffer != null ? frameBuffer.handle() : -1;
    }

    public void flushModeChanges() {

    }

    public int glId() {
        return this.texture.handle();
    }

    public void setAddressMode(AddressMode addressMode, AddressMode addressMode2) {
    }

    public void setTextureFilter(FilterMode filterMode, FilterMode filterMode2, boolean bl) {
    }
}
#else
public class GpuTextureAdapter {
    private final ITexture texture;

    GpuTextureAdapter(ITexture texture) {

        this.texture = texture;
    }

    public static Object ofTexture(ITexture texture) {
        return new GpuTextureAdapter(texture);
    }
}
#endif
