package io.homo.superresolution.mixin.core;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.debug.DebugInfo;
import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final
    Minecraft minecraft;
    @Unique
    public Matrix4f super_resolution$curMatrix4f = new Matrix4f();
    @Unique
    public Matrix4f super_resolution$lastMatrix4f = new Matrix4f();

    @Unique
    public float super_resolution$frameTimeDelta_fsr = 16.6f;
    @Unique
    public float super_resolution$lastRenderTime_algo = -1;
    @Unique
    private boolean super_resolution$shouldResize = true;
    @Inject(method = "resize",at = @At(value = "HEAD"))
    private void onResize(int i,int j,CallbackInfo ci){
        if (SuperResolution.isInit&&SuperResolution.gameIsLoad){
            SuperResolution.getInstance().resize(i,j);
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "render")
    private void onRenderStart(float partialTicks, long nanoTime, boolean renderLevel,CallbackInfo ci) {
        //Minecraft.getInstance().getMainRenderTarget().clear(Minecraft.ON_OSX);
        if (renderLevel && this.minecraft.level != null){
            SuperResolution.isRenderingWorld = true;
            if (super_resolution$shouldResize){
                super_resolution$shouldResize = false;
                SuperResolution.getInstance().resize(
                        SuperResolution.getMinecraftWidth(),
                        SuperResolution.getMinecraftHeight()
                );
                ResolutionControl.getInstance().onResolutionChanged();
            }
        }else{
            super_resolution$shouldResize = true;
        }
    }

    //在渲染世界的开始调整帧缓冲区大小
    //@Inject(at = @At(value = "HEAD"), method = "renderLevel")
    //private void onRenderLevelStart(CallbackInfo ci) {
    //    if (Minecraft.getInstance().level != null){
    //        ResolutionControl.getInstance().setShouldScale(true);
    //    }
    //}
    ////在渲染世界后运行fsr
    //@Inject(at = @At(value = "TAIL"), method = "renderLevel")
    //private void onRenderLevelEnd(CallbackInfo ci) {
    //    if (Minecraft.getInstance().level != null){
    //        ResolutionControl.getInstance().setShouldScale(false);
    //        super_resolution$lastRenderTime_fsr = Util.getMillis();
    //        SuperResolution.FSR.CallFSR2(SuperResolution.frameTimeDelta);
    //        super_resolution$frameTimeDelta_fsr = Util.getMillis()-super_resolution$lastRenderTime_fsr;
    //        DebugInfo.setFrameTimeDelta_fsr(super_resolution$frameTimeDelta_fsr);
    //        SuperResolution.FSR.getWorldFramebuffer().blitToScreen(
    //                minecraft.getWindow().getScreenWidth(),
    //                minecraft.getWindow().getScreenHeight()
    //        );
    //    }
    //}

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"), method = "render")
    private void onRenderBegin(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null){
            ResolutionControl.getInstance().setShouldScale(true);
        }
    }
    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null){
            SuperResolution.isRenderingWorld = false;
            ResolutionControl.getInstance().setShouldScale(false);
            super_resolution$lastRenderTime_algo = Util.getMillis();
            SuperResolution.currentAlgorithm.run(SuperResolution.frameTimeDelta);
            super_resolution$frameTimeDelta_fsr = Util.getMillis()- super_resolution$lastRenderTime_algo;
            DebugInfo.setFrameTimeDelta_algo(super_resolution$frameTimeDelta_fsr);
            SuperResolution.mainTarget.bindWrite(true);
            SuperResolution.currentAlgorithm.blitToScreen(
                    minecraft.getWindow().getScreenWidth(),
                    minecraft.getWindow().getScreenHeight()
            );
        }
    }
    /*
    @Inject(method = "renderLevel",
            at= @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void getMatrix4f(float f, long l, PoseStack poseStack1, CallbackInfo ci, PoseStack poseStack, boolean bl, Camera camera, PoseStack poseStack2, double d, float g, float h, Matrix4f matrix4f, Matrix3f matrix3f){
        lastMatrix4f = curMatrix4f;
        curMatrix4f = matrix4f;
    }*/
}
