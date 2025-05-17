package io.homo.superresolution.core.gl.texture;

import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;

import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL43.glTextureView;

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
        if (parent.getTextureId() == 0) {
            throw new IllegalStateException("Parent texture is not initialized");
        }
        int viewId = glGenTextures();
        glTextureView(
                viewId,
                type,
                parent.getTextureId(),
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
    public int getTextureId() {
        return id;
    }

    @Override
    public TextureFormat getTextureFormat() {
        return parent.getTextureFormat();
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
        glDeleteTextures(this.id);
        this.id = -1;
    }

    @Override
    public void resize(int width, int height) {
        throw new RuntimeException("GlTextureView不可更改大小");
    }
}
