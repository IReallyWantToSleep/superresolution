package io.homo.superresolution.core.graphics.opengl.texture;

import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;

public class GlTextureView implements ITexture {
    private final ITexture parent;
    private final int type;
    private final int minLevel;
    private final int numLevels;
    private final int minLayer;
    private final int numLayers;
    private int id;

    private GlTextureView(ITexture parent, int type, int minLevel,
                          int numLevels, int minLayer, int numLayers,
                          int id) {
        this.parent = parent;
        this.type = type;
        this.minLevel = minLevel;
        this.numLevels = numLevels;
        this.minLayer = minLayer;
        this.numLayers = numLayers;
        this.id = id;
    }

    public static GlTextureView create(ITexture parent, int type,
                                       int minLevel, int numLevels,
                                       int minLayer, int numLayers) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent texture cannot be null");
        }
        if (parent.handle() == 0) {
            throw new IllegalStateException("Parent texture is not initialized");
        }
        int viewId = Gl.DSA.createTextureView(
                (int) parent.handle(),
                type,
                parent.getTextureFormat().gl(),
                minLevel,
                numLevels,
                minLayer,
                numLayers
        );

        return new GlTextureView(
                parent,
                type,
                minLevel,
                numLevels,
                minLayer,
                numLayers,
                viewId
        );
    }

    public ITexture getParent() {
        return parent;
    }

    public int getType() {
        return type;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getNumLevels() {
        return numLevels;
    }

    public int getMinLayer() {
        return minLayer;
    }

    public int getNumLayers() {
        return numLayers;
    }

    @Override
    public long handle() {
        return id;
    }

    @Override
    public TextureFormat getTextureFormat() {
        return parent.getTextureFormat();
    }

    @Override
    public TextureUsages getTextureUsages() {
        return parent.getTextureUsages();
    }

    @Override
    public TextureType getTextureType() {
        return parent.getTextureType();
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return parent.getTextureFilterMode();
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return parent.getTextureWrapMode();
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return parent.getMipmapSettings();
    }

    public TextureDescription getTextureDescription() {
        return parent.getTextureDescription();
    }

    @Override
    public int getWidth() {
        return parent.getWidth();
    }

    @Override
    public int getHeight() {
        return parent.getHeight();
    }

    @Override
    public void destroy() {
        Gl.DSA.deleteTexture(this.id);
        this.id = -1;
    }

    @Override
    public void resize(int width, int height) {
        throw new RuntimeException("GlTextureView不可更改大小");
    }
}
