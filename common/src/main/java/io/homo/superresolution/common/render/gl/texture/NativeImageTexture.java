package io.homo.superresolution.common.render.gl.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;

public class NativeImageTexture extends Texture{
    public NativeImageTexture(int width, int height, int format, NativeImage image) {
        super(width, height, format);
        this.id = TextureUtil.generateTextureId();
        TextureUtil.prepareImage(this.id, 0, image.getWidth(), image.getHeight());
        image.flipY();
        image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), false, false, false, true);
    }
}
