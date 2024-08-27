package io.homo.superresolution.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.homo.superresolution.SuperResolution;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Unique
    public Matrix4f curMatrix4f = new Matrix4f();
    @Unique
    public Matrix4f lastMatrix4f = new Matrix4f();
    @Inject(method = "resize",at = @At(value = "HEAD"))
    private void onResize(int i,int j,CallbackInfo ci){
        if (SuperResolution.isInit&&SuperResolution.gameIsLoad){
            SuperResolution.getInstance().resize(SuperResolution.getMinecraftWidth(),SuperResolution.getMinecraftHeight());
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
