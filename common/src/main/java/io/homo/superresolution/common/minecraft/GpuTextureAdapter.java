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
        #if MC_VER > MC_1_21_5
        super(GpuTexture.USAGE_COPY_DST & GpuTexture.USAGE_COPY_SRC & GpuTexture.USAGE_TEXTURE_BINDING & GpuTexture.USAGE_RENDER_ATTACHMENT,
                texture.handle() + "--" + texture.getTextureFormat(),
                texture.getTextureFormat() == TextureFormat.RGBA8 ?
                        com.mojang.blaze3d.textures.TextureFormat.RGBA8 :
                        com.mojang.blaze3d.textures.TextureFormat.DEPTH32,
                texture.getWidth(),
                texture.getHeight(),
                1,
                1,
                (int) texture.handle()
        );
        #else
        super(texture.handle() + "--" + texture.getTextureFormat(),
                texture.getTextureFormat() == TextureFormat.RGBA8 ?
                        com.mojang.blaze3d.textures.TextureFormat.RGBA8 :
                        com.mojang.blaze3d.textures.TextureFormat.DEPTH32,
                texture.getWidth(),
                texture.getHeight(),
                1,
                (int) texture.handle()
        );
        #endif
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
        return Math.toIntExact(frameBuffer != null ? frameBuffer.handle() : -1);
    }

    public void flushModeChanges() {

    }

    public int glId() {
        return Math.toIntExact(this.texture.handle());
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
