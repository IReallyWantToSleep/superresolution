package io.homo.superresolution.resolutioncontrol.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import static io.homo.superresolution.render.gl.GlConst.*;
import static io.homo.superresolution.render.gl.Gl.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderTarget.class)
public abstract class FramebufferMixin {
    @Unique private float super_resolution$scaleMultiplier;

    @Shadow
    public abstract int getDepthTextureId();


    @Inject(method = "createBuffers", at = @At("HEAD"))
    private void onInitFbo(int width, int height, boolean getError, CallbackInfo ci) {
        super_resolution$scaleMultiplier = (float) width / Minecraft.getInstance().getWindow().getWidth();
    }

    @Redirect(method = {"createBuffers","setFilterMode"}, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texParameter(III)V"))
    private void onSetTexFilter(int target, int pname, int param) {

        if(false) {
            if (pname == GL_TEXTURE_MIN_FILTER) {
                //GlStateManager._texParameter(target, pname,
                //        ResolutionControlMod.getInstance().getUpscaleAlgorithm().getId(isMipmapped));
            } else if (pname == GL_TEXTURE_WRAP_S || pname == GL_TEXTURE_WRAP_T) {
                GlStateManager._texParameter(target, pname, GL_CLAMP_TO_EDGE);
            } else {
                GlStateManager._texParameter(target, pname, param);
            }
        }else{
            GlStateManager._texParameter(target, pname, param);
        }
    }

    @Inject(method = "blitToScreen(IIZ)V", at = @At("HEAD"))
    private void onDraw(int width, int height, boolean bl, CallbackInfo ci) {
        if (super_resolution$scaleMultiplier > 2.0f) {
            GlStateManager._bindTexture(this.getDepthTextureId());
            glGenerateMipmap(GL_TEXTURE_2D);
        }
    }
}
