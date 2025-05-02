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