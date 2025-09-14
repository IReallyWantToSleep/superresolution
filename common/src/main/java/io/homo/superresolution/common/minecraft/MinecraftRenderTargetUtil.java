/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.common.minecraft;


import com.mojang.blaze3d.pipeline.RenderTarget;

#if MC_VER > MC_1_21_4
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Objects;

public class MinecraftRenderTargetUtil {
    public static int getFboId(RenderTarget renderTarget) {
        return ((GlTexture) Objects.requireNonNull(renderTarget.getColorTexture())).getFbo(((GlDevice) RenderSystem.getDevice()).directStateAccess(), renderTarget.getDepthTexture());
    }

    public static int getColorTexId(RenderTarget renderTarget) {
        return ((GlTexture) Objects.requireNonNull(renderTarget.getColorTexture())).glId();
    }

    public static int getDepthTexId(RenderTarget renderTarget) {
        return ((GlTexture) Objects.requireNonNull(renderTarget.getDepthTexture())).glId();
    }
}
#else
public class MinecraftRenderTargetUtil {
    public static int getFboId(RenderTarget renderTarget) {
        return renderTarget.frameBufferId;
    }

    public static int getColorTexId(RenderTarget renderTarget) {
        return renderTarget.getColorTextureId();
    }

    public static int getDepthTexId(RenderTarget renderTarget) {
        return renderTarget.getDepthTextureId();
    }
}
#endif