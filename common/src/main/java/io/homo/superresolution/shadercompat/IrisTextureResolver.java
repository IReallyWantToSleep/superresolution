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

package io.homo.superresolution.shadercompat;

import com.google.common.collect.ImmutableSet;
import io.homo.irisapi.ICompositeRendererAccessor;
import io.homo.irisapi.IrisReflectionUtils;
import io.homo.irisapi.NamedCompositePass;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlOnlyNameTexture;
import io.homo.superresolution.shadercompat.mixin.core.CompositeRendererAccessor;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.targets.RenderTarget;
import net.irisshaders.iris.targets.RenderTargets;

import java.util.function.Function;

import static org.lwjgl.opengl.GL11.*;

public class IrisTextureResolver {
    private static final String AUTO_PREFIX = "autotex";
    private static final String COLOR_PREFIX = "colortex";
    private static final String ALT_PREFIX = "alttex";
    private static final String DEPTH_TEX = "depthtex";
    private static final String NO_HAND_DEPTH_TEX = "noHandDepthtex";
    private static final String NO_TRANSLUCENT_DEPTH_TEX = "noTranslucentDepthtex";

    public static GlOnlyNameTexture getIrisTexture(
            ICompositeRendererAccessor renderer,
            String name,
            NamedCompositePass pass
    ) {
        return getIrisTexture(renderer, name, pass, false);
    }
    public static GlOnlyNameTexture getIrisTexture(
            ICompositeRendererAccessor renderer,
            String name,
            NamedCompositePass pass,
            boolean useStageWritesToMain
    ) {
        if (getIrisTextureByName(renderer, name,pass,useStageWritesToMain)< 1) return null;
        return new GlOnlyNameTexture(
                () -> {
                    if (!isColorTextureName(name)){
                        int internalFormat = GlTextureInfoGetter.getInternalFormat(GL_TEXTURE_2D,getIrisTextureByName(renderer, name, pass, useStageWritesToMain));
                        return TextureFormat.fromGl(internalFormat);
                    }
                    InternalTextureFormat irisFormat = getIrisColorTextureByName(renderer, name, pass, useStageWritesToMain).getInternalFormat();
                    int format = irisFormat != null && irisFormat.getGlFormat() != GL_RGBA ? irisFormat.getGlFormat() : GL_RGBA8;
                    return TextureFormat.fromGl(format);
                },
                () -> {
                    if (!isColorTextureName(name)){
                        return GlTextureInfoGetter.getWidth(GL_TEXTURE_2D, getIrisTextureByName(renderer, name,pass,useStageWritesToMain));
                    }
                    return getIrisColorTextureByName(renderer, name, pass, useStageWritesToMain).getWidth();
                },
                () -> {
                    if (!isColorTextureName(name)){
                        return GlTextureInfoGetter.getHeight(GL_TEXTURE_2D, getIrisTextureByName(renderer, name,pass,useStageWritesToMain));
                    }
                    return getIrisColorTextureByName(renderer, name, pass, useStageWritesToMain).getHeight();
                },
                () -> (long) getIrisTextureByName(renderer, name,pass,useStageWritesToMain)
        );
    }

    public static boolean isColorTextureName(String name) {
        return name.startsWith(COLOR_PREFIX) || name.startsWith(ALT_PREFIX) || name.startsWith(AUTO_PREFIX);
    }

    public static RenderTarget getIrisColorTextureByName(ICompositeRendererAccessor renderer, String name, NamedCompositePass pass, boolean useStageWritesToMain) {
        return resolveColorTexture(renderer, name,
                texId -> getCompositeRendererRenderTargets(renderer)
                        .getOrCreate(texId),
                texId -> getCompositeRendererRenderTargets(renderer)
                        .getOrCreate(texId),
                pass,
                useStageWritesToMain
        );
    }

    public static int getIrisTextureByName(ICompositeRendererAccessor renderer, String name,NamedCompositePass pass,boolean useStageWritesToMain) {
        return resolveTexture(renderer, name,
                texId -> getCompositeRendererRenderTargets(renderer)
                        .getOrCreate(texId)
                        .getMainTexture(),
                texId -> getCompositeRendererRenderTargets(renderer)
                        .getOrCreate(texId)
                        .getAltTexture(),
                d->d,
                -1,
                pass,
                useStageWritesToMain
        );
    }

    public static RenderTargets getCompositeRendererRenderTargets(ICompositeRendererAccessor renderer) {
        return renderer.getRenderTargets();
    }

    private static <T> T resolveColorTexture(
            ICompositeRendererAccessor renderer,
            String name,
            Function<Integer, T> colorResolver,
            Function<Integer, T> colorAltResolver,
            NamedCompositePass pass,
            boolean useStageWritesToMain
    ) {
        try {
            if (name.startsWith(COLOR_PREFIX)) {
                return colorResolver.apply(Integer.parseInt(name.substring(COLOR_PREFIX.length())));
            } else if (name.startsWith(ALT_PREFIX)) {
                return colorAltResolver.apply(Integer.parseInt(name.substring(ALT_PREFIX.length())));
            } else if (name.startsWith(AUTO_PREFIX)) {
                ImmutableSet<Integer> stateReadsFromAlt = IrisReflectionUtils.getCompositePassStateReadsFromAlt(
                        pass
                );
                int index = Integer.parseInt(name.substring(AUTO_PREFIX.length()));
                if (stateReadsFromAlt.contains(index)){
                    return useStageWritesToMain ? colorResolver.apply(index): colorAltResolver.apply(index);
                } else {
                    return useStageWritesToMain ? colorAltResolver.apply(index): colorResolver.apply(index);
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static <T> T resolveTexture(
            ICompositeRendererAccessor renderer,
            String name,
            Function<Integer, T> colorResolver,
            Function<Integer, T> colorAltResolver,
            Function<Integer, T> depthResolver,
            T defaultValue,
            NamedCompositePass pass,
            boolean useStageWritesToMain
    ) {
        try {
            if (name.startsWith(COLOR_PREFIX)) {
                return colorResolver.apply(Integer.parseInt(name.substring(COLOR_PREFIX.length())));
            } else if (name.startsWith(ALT_PREFIX)) {
                return colorAltResolver.apply(Integer.parseInt(name.substring(ALT_PREFIX.length())));
            } else if (name.equals(DEPTH_TEX)) {
                return depthResolver.apply(getDepthTexId(renderer));
            } else if (name.equals(NO_HAND_DEPTH_TEX)) {
                return depthResolver.apply(getNoHandDepthTexId(renderer));
            } else if (name.equals(NO_TRANSLUCENT_DEPTH_TEX)) {
                return depthResolver.apply(getNoTranslucentDepthTexId(renderer));
            } else if (name.startsWith(AUTO_PREFIX)) {
                ImmutableSet<Integer> stateReadsFromAlt = IrisReflectionUtils.getCompositePassStateReadsFromAlt(
                        pass
                );
                int index = Integer.parseInt(name.substring(AUTO_PREFIX.length()));
                if (stateReadsFromAlt.contains(index)){
                    return useStageWritesToMain ? colorResolver.apply(index): colorAltResolver.apply(index);
                } else {
                    return useStageWritesToMain ? colorAltResolver.apply(index): colorResolver.apply(index);
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return defaultValue;
    }

    #if MC_VER < MC_1_21_5
    private static int getDepthTexId(ICompositeRendererAccessor renderer) {
        return renderer.getRenderTargets().getDepthTexture();
    }

    private static int getNoHandDepthTexId(ICompositeRendererAccessor renderer) {
        return renderer.getRenderTargets().getDepthTextureNoHand().getTextureId();
    }

    private static int getNoTranslucentDepthTexId(ICompositeRendererAccessor renderer) {
        return renderer.getRenderTargets().getDepthTextureNoTranslucents().getTextureId();
    }
    #else
    private static int getDepthTexId(ICompositeRendererAccessor renderer) {
        return ((com.mojang.blaze3d.opengl.GlTexture) (renderer)
                .getRenderTargets().getDepthTexture()).glId();
    }

    private static int getNoHandDepthTexId(ICompositeRendererAccessor renderer) {
        return ((com.mojang.blaze3d.opengl.GlTexture) (renderer)
                .getRenderTargets().getDepthTextureNoHand()).glId();
    }

    private static int getNoTranslucentDepthTexId(ICompositeRendererAccessor renderer) {
        return ((com.mojang.blaze3d.opengl.GlTexture) (renderer)
                .getRenderTargets().getDepthTextureNoTranslucents()).glId();
    }
    #endif
}

