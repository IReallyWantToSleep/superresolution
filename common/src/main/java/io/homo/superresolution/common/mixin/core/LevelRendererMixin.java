package io.homo.superresolution.common.mixin.core;

#if MC_VER > MC_1_20_1
#else
import com.mojang.blaze3d.vertex.PoseStack;
#endif

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(at = @At(value = "HEAD"), method = "initOutline")
    private void onInitOutline(CallbackInfo ci) {
        MinecraftRenderHandle.onInitEntityEffectBegin();
    }

    @Inject(at = @At(value = "RETURN"), method = "initOutline")
    private void onInitOutlineEnd(CallbackInfo ci) {
        MinecraftRenderHandle.onInitEntityEffectEnd();
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V"), method = "doEntityOutline")
    private void fixEntityOutlineEffect(RenderTarget instance, int width, int height, boolean disableBlend) {
        MinecraftRenderHandle.onBlitEntityEffect();
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;resize(II)V"), method = "resize")
    private void onResizePostChain(PostChain instance, int w, int h) {
        instance.resize(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight());
    }
    /*
    @Inject(at = @At(value = "HEAD"), method = "renderLevel")
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldBegin();
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldEnd();
        }
    }*/

    /*
    @Shadow
    private PostChain entityEffect;
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract void resize(int width, int height);

    @Inject(at = @At(value = "HEAD"), method = "initOutline")
    private void onInitOutline(CallbackInfo ci) {
        MinecraftRenderingStates.setClientRenderTarget(MinecraftRenderingStates.getRenderTarget());
    }


    @Inject(at = @At("RETURN"), method = "resize")
    private void onResized(CallbackInfo ci) {
        if (minecraft.level == null) return;
        MinecraftRenderingStates.resizeMinecraftRenderTarget();
        //entityEffect.resize(MinecraftRenderingStates.getRenderWidth(), MinecraftRenderingStates.getRenderHeight());
    }
    @Inject(at = @At(value = "HEAD"), method = "renderLevel")
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldBegin();
        }
    }

    @Inject(at = @At(value = "TAIL"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldEnd();
        }
    }*/

    //@Inject(at = @At(value = "TAIL"), method = "allChanged")
    //private void onReloadDone(CallbackInfo ci) {
    //    if (Minecraft.getInstance().level != null) {
    //        Minecraft.getInstance().resizeDisplay();
    //    }
    //}*/
}
