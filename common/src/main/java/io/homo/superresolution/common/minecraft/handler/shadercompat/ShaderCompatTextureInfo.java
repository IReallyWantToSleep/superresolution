/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.minecraft.handler.shadercompat;

import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;

import java.util.List;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL43.glCopyImageSubData;

public class ShaderCompatTextureInfo {
    private final Supplier<ITexture> sourceTextureSupplier;
    private final TextureRegion region;
    private final boolean isOutput;
    private final String name;
    private GlTexture2D internalTexture;

    public ShaderCompatTextureInfo(Supplier<ITexture> sourceTextureSupplier, TextureRegion region, boolean isOutput, String name) {
        this.sourceTextureSupplier = sourceTextureSupplier;
        this.region = region;
        this.isOutput = isOutput;
        this.name = name;
    }

    public static int resolveRegionValue(int value, boolean isWidth) {
        if (value == -1) return isWidth ?
                RenderHandlerManager.getRenderWidth() :
                RenderHandlerManager.getRenderHeight();
        if (value == -2) return isWidth ?
                RenderHandlerManager.getScreenWidth() :
                RenderHandlerManager.getScreenHeight();
        return value;
    }

    public ITexture getSourceTexture() {
        return sourceTextureSupplier.get();
    }

    public ITexture getInternalTexture() {
        return internalTexture;
    }

    public void updateTexture() {
        ITexture sourceTexture = sourceTextureSupplier.get();
        if (sourceTexture == null) return;

        int width = region.resolve(RenderHandlerManager.getRenderSize(), RenderHandlerManager.getScreenSize())[2];
        int height = region.resolve(RenderHandlerManager.getRenderSize(), RenderHandlerManager.getScreenSize())[3];

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
                    sourceTexture, region.getX(), region.getY()
            );
        } else {
            copyTextureRegion(
                    sourceTexture, region.getX(), region.getY(), width, height,
                    internalTexture, 0, 0
            );
        }
    }

    public void createInternalTexture(int width, int height) {
        ITexture sourceTexture = sourceTextureSupplier.get();
        if (sourceTexture == null) return;
        internalTexture = GlTexture2D.create(
                TextureDescription.create()
                        .width(width)
                        .height(height)
                        .type(TextureType.Texture2D)
                        .mipmapsDisabled()
                        .usages(TextureUsages.create().sampler())
                        .format(sourceTexture.getTextureFormat())
                        .label("SRIrisCompatInternalTexture-%s".formatted(this.name))
                        .build()
        );
    }

    public void replaceInternalTexture(ITexture newTexture) {
        if (internalTexture != null) {
            internalTexture.destroy();
        }
        this.internalTexture = (GlTexture2D) newTexture;
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
