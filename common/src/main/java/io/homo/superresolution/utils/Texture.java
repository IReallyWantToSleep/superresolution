package io.homo.superresolution.utils;

import com.mojang.blaze3d.platform.TextureUtil;

public class Texture {
    public int id;
    public Texture(){
        this.id = TextureUtil.generateTextureId();
    }
}
