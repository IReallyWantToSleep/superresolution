package io.homo.superresolution.shadercompat;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import net.irisshaders.iris.pipeline.CompositeRenderer;

import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL43.glCopyImageSubData;

public class TextureConfigResolver {

    public static class TextureInfo {
        private final ITexture sourceTexture;
        private final List<Integer> region;
        private final boolean isOutput;
        private GlTexture2D internalTexture;

        public TextureInfo(ITexture sourceTexture, List<Integer> region, boolean isOutput) {
            this.sourceTexture = sourceTexture;
            this.region = region;
            this.isOutput = isOutput;
        }

        public ITexture getSourceTexture() {
            return sourceTexture;
        }

        public ITexture getInternalTexture() {
            return internalTexture;
        }

        public void updateTexture() {
            if (sourceTexture == null) return;

            int width = resolveRegionValue(region.get(2), true);
            int height = resolveRegionValue(region.get(3), false);

            if (internalTexture == null) {
                createInternalTexture(width, height);
            } else if (internalTexture.getWidth() != width ||
                    internalTexture.getHeight() != height ||
                    internalTexture.getTextureFormat() != sourceTexture.getTextureFormat()) {
                internalTexture.destroy();
                createInternalTexture(width, height);
            }

            if (isOutput) {
                copyTextureRegion(
                        internalTexture, 0, 0, width, height,
                        sourceTexture, region.get(0), region.get(1)
                );
            } else {
                copyTextureRegion(
                        sourceTexture, region.get(0), region.get(1), width, height,
                        internalTexture, 0, 0
                );
            }
        }

        public void createInternalTexture(int width, int height) {
            internalTexture = GlTexture2D.create(
                    TextureDescription.create()
                            .width(width)
                            .height(height)
                            .type(TextureType.Texture2D)
                            .mipmapsDisabled()
                            .usages(TextureUsages.create().sampler())
                            .format(sourceTexture.getTextureFormat())
                            .build()
            );
        }

        public int resolveRegionValue(int value, boolean isWidth) {
            if (value == -1) return isWidth ?
                    MinecraftRenderHandle.getRenderWidth() :
                    MinecraftRenderHandle.getRenderHeight();
            if (value == -2) return isWidth ?
                    MinecraftRenderHandle.getScreenWidth() :
                    MinecraftRenderHandle.getScreenHeight();
            return value;
        }

        public void copyTextureRegion(
                ITexture src, int srcX, int srcY, int srcWidth, int srcHeight,
                ITexture dest, int destX, int destY
        ) {
            glCopyImageSubData(
                    (int) src.handle(), GL_TEXTURE_2D, 0,
                    srcX, srcY, 0,
                    (int) dest.handle(), GL_TEXTURE_2D, 0,
                    destX, destY, 0,
                    srcWidth, srcHeight, 1
            );
        }
    }

    public static TextureInfo createForInput(CompositeRenderer renderer, SRShaderCompatConfig.InputTextureConfig config) {
        ITexture source = IrisTextureResolver.getIrisTexture(renderer, config.src);
        return new TextureInfo(source, config.region, false);
    }
}