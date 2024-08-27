package io.homo.superresolution.resolutioncontrol.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import io.homo.superresolution.config.Config;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL45;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

@Mixin(value = RenderTarget.class)
public abstract class FramebufferMixin {
    @Unique private float scaleMultiplier;

    @Shadow
    public abstract int getDepthTextureId();

    @Inject(method = "createBuffers", at = @At("HEAD"))
    private void onInitFbo(int width, int height, boolean getError, CallbackInfo ci) {
        scaleMultiplier = (float) width / Minecraft.getInstance().getWindow().getWidth();
    }

    @Redirect(method = "createBuffers", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V")
    )
    private void onTexImage(int target, int level, int internalFormat, int width, int height, int border, int format,
                            int type, IntBuffer pixels) {
        if (scaleMultiplier > 2.0f) {
            int mipmapLevel = Mth.ceil(Math.log(scaleMultiplier) / Math.log(2));
            for (int i = 0; i < mipmapLevel; i++) {
                GlStateManager._texImage2D(target, i, internalFormat,
                       width << i, height << i,
                        border, format, type, pixels);
            }
        } else {
            GlStateManager._texImage2D(target, 0, internalFormat, width, height, border, format, type, pixels);
        }

    }

    @Inject(method = "blitToScreen(IIZ)V", at = @At("HEAD"))
    private void onDraw(int width, int height, boolean bl, CallbackInfo ci) {
        if (scaleMultiplier > 2.0f) {
            GlStateManager._bindTexture(this.getDepthTextureId());
            GL45.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }
    }
}
