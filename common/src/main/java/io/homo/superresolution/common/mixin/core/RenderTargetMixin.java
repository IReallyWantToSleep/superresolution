package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.upscale.AlgorithmType;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.homo.superresolution.common.render.gl.Gl.glGenerateMipmap;
import static io.homo.superresolution.common.render.gl.GlConst.*;

@Mixin(value = RenderTarget.class)
public abstract class RenderTargetMixin {
    @Unique
    private float super_resolution$scaleMultiplier;

    @Shadow
    public abstract int getDepthTextureId();


    @Inject(method = "createBuffers", at = @At("HEAD"))
    #if MC_VER > MC_1_21_1
    private void onInitFbo(int width, int height, CallbackInfo ci)
    #else
    private void onInitFbo(int width, int height, boolean getError, CallbackInfo ci)
    #endif {
        super_resolution$scaleMultiplier = (float) width / Minecraft.getInstance().getWindow().getWidth();
    }

    @Redirect(method = {"createBuffers", "setFilterMode*"}, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texParameter(III)V"), remap = false)
    private void onSetTexFilter(int target, int pname, int param) {
        if (AlgorithmType.NONE.equals(SuperResolution.algorithmType)) {
            if (pname == GL_TEXTURE_MIN_FILTER) {
                GlStateManager._texParameter(target, pname, GL_LINEAR);
            } else if (pname == GL_TEXTURE_MAG_FILTER) {
                GlStateManager._texParameter(target, pname, GL_LINEAR);
            } else if (pname == GL_TEXTURE_WRAP_S || pname == GL_TEXTURE_WRAP_T) {
                GlStateManager._texParameter(target, pname, GL_CLAMP_TO_EDGE);
            } else {
                GlStateManager._texParameter(target, pname, param);
            }
        } else {
            GlStateManager._texParameter(target, pname, param);
        }
    }

    #if MC_VER > MC_1_21_1
    @Inject(method = {"blitAndBlendToScreen", "blitToScreen"}, at = @At("HEAD"))
    private void onDraw(int width, int height, CallbackInfo ci)
    #else
    @Inject(method = "blitToScreen(IIZ)V", at = @At("HEAD"))
    private void onDraw(int width, int height, boolean bl, CallbackInfo ci)
    #endif {
        if (super_resolution$scaleMultiplier > 2.0f) {
            GlStateManager._bindTexture(this.getDepthTextureId());
            glGenerateMipmap(GL_TEXTURE_2D);
        }
    }
}
