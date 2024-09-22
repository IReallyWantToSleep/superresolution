package io.homo.superresolution.mixin;

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
    public float super_resolution$lastRenderTime_fsr = -1;
    @Inject(method = "resize",at = @At(value = "HEAD"))
    private void onResize(int i,int j,CallbackInfo ci){
        if (SuperResolution.isInit&&SuperResolution.gameIsLoad){
            SuperResolution.getInstance().resize(SuperResolution.getMinecraftWidth(),SuperResolution.getMinecraftHeight());

        }
    }

    @Inject(at = @At(value = "RETURN"), method = "render")
    private void onRenderEnd(CallbackInfo ci) {
        if (minecraft.level != null && ResolutionControl.getInstance().getWorldFramebuffer() != null){
            super_resolution$lastRenderTime_fsr = Util.getMillis();
            SuperResolution.FSR.CallFSR2(SuperResolution.frameTimeDelta);
            super_resolution$frameTimeDelta_fsr = Util.getMillis()-super_resolution$lastRenderTime_fsr;
            DebugInfo.setFrameTimeDelta_fsr(super_resolution$frameTimeDelta_fsr);
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
