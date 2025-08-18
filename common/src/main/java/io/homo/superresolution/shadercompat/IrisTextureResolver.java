package io.homo.superresolution.shadercompat;

import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.shadercompat.mixin.core.CompositeRendererAccessor;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.targets.RenderTargets;

import java.util.function.Function;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class IrisTextureResolver {
    private static final String COLOR_PREFIX = "colortex";
    private static final String ALT_PREFIX = "alttex";
    private static final String DEPTH_TEX = "depthtex";
    private static final String NO_HAND_DEPTH_TEX = "noHandDepthtex";
    private static final String NO_TRANSLUCENT_DEPTH_TEX = "noTranslucentDepthtex";

    public static OnlyNameTexture getIrisTexture(CompositeRenderer renderer, String name) {
        int id = getIrisTextureByName(renderer, name);
        if (id < 1) return null;
        return new OnlyNameTexture(
                () -> {
                    int format = GlTextureInfoGetter.getInternalFormat(GL_TEXTURE_2D, id);
                    return format == GL_DEPTH_COMPONENT ? TextureFormat.DEPTH32 : TextureFormat.fromGl(format);
                },
                () -> GlTextureInfoGetter.getWidth(GL_TEXTURE_2D, id),
                () -> GlTextureInfoGetter.getHeight(GL_TEXTURE_2D, id),
                () -> (long) id
        );
    }

    public static int getIrisTextureByName(CompositeRenderer renderer, String name) {
        return resolveTexture(renderer, name,
                texId -> getCompositeRendererRenderTargets(renderer)
                        .getOrCreate(texId)
                        .getMainTexture(),
                texId -> getCompositeRendererRenderTargets(renderer)
                        .getOrCreate(texId)
                        .getAltTexture(),
                depthId -> depthId,
                -1
        );
    }

    public static RenderTargets getCompositeRendererRenderTargets(CompositeRenderer renderer) {
        return ((CompositeRendererAccessor) renderer).getRenderTargets();
    }

    private static <T> T resolveTexture(
            CompositeRenderer renderer,
            String name,
            Function<Integer, T> colorResolver,
            Function<Integer, T> colorAltResolver,
            Function<Integer, T> depthResolver,
            T defaultValue
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
            }
        } catch (NumberFormatException ignored) {
        }
        return defaultValue;
    }

    #if MC_VER < MC_1_21_5
    private static int getDepthTexId(CompositeRenderer renderer) {
        return ((CompositeRendererAccessor) renderer).getRenderTargets().getDepthTexture();
    }

    private static int getNoHandDepthTexId(CompositeRenderer renderer) {
        return ((CompositeRendererAccessor) renderer).getRenderTargets().getDepthTextureNoHand().getTextureId();
    }

    private static int getNoTranslucentDepthTexId(CompositeRenderer renderer) {
        return ((CompositeRendererAccessor) renderer).getRenderTargets().getDepthTextureNoTranslucents().getTextureId();
    }
    #else
    private static int getDepthTexId(CompositeRenderer renderer) {
        return ((com.mojang.blaze3d.opengl.GlTexture) ((CompositeRendererAccessor) renderer)
                .getRenderTargets().getDepthTexture()).glId();
    }

    private static int getNoHandDepthTexId(CompositeRenderer renderer) {
        return ((com.mojang.blaze3d.opengl.GlTexture) ((CompositeRendererAccessor) renderer)
                .getRenderTargets().getDepthTextureNoHand()).glId();
    }

    private static int getNoTranslucentDepthTexId(CompositeRenderer renderer) {
        return ((com.mojang.blaze3d.opengl.GlTexture) ((CompositeRendererAccessor) renderer)
                .getRenderTargets().getDepthTextureNoTranslucents()).glId();
    }
    #endif
}
