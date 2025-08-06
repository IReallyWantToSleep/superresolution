package io.homo.superresolution.srapi;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public class SRTextureResource {
    public SRTextureResourceDescription description;
    public ITexture texture;
    public long handle;

    public SRTextureResource(ITexture texture) {
        this.texture = texture;
        this.description = new SRTextureResourceDescription(texture);
        this.handle = texture.handle();
    }

    public long getHandle() {
        this.handle = texture.handle();
        return handle;
    }
}
