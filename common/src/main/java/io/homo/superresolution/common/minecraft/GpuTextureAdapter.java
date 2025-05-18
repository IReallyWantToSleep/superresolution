package io.homo.superresolution.common.minecraft;


import io.homo.superresolution.core.impl.texture.ITexture;

#if MC_VER > MC_1_21_4
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;

import javax.annotation.Nullable;

public class GpuTextureAdapter extends GlTexture {
    private final ITexture texture;
    private IFrameBuffer frameBuffer;

    GpuTextureAdapter(ITexture texture) {
        super(texture.getTextureId() + "--" + texture.getTextureFormat(),
                texture.getTextureFormat() == TextureFormat.RGBA8 ?
                        com.mojang.blaze3d.textures.TextureFormat.RGBA8 :
                        com.mojang.blaze3d.textures.TextureFormat.DEPTH32,
                texture.getWidth(),
                texture.getHeight(),
                1,
                texture.getTextureId()
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
        return frameBuffer != null ? frameBuffer.getFrameBufferId() : -1;
    }

    public void flushModeChanges() {

    }

    public int glId() {
        return this.texture.getTextureId();
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
