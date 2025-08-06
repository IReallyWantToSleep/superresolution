package io.homo.superresolution.neoforge.mixin.compat.iris;


import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

#if MC_VER > MC_1_21_5
import net.minecraft.client.Minecraft;
import net.irisshaders.iris.targets.Blaze3dRenderTargetExt;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#endif
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class IrisRenderingPipelineMixin {
    @Shadow
    @Final
    private PackDirectives packDirectives;

    @Shadow
    @Final
    private RenderTargets renderTargets;

    #if MC_VER > MC_1_21_5
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void replaceRenderTarget(ProgramSet programSet, CallbackInfo ci) {
        RenderTarget main = MinecraftRenderHandle.getRenderTarget().asMcRenderTarget();
        GpuTexture depthTexture = main.getDepthTexture();
        DepthBufferFormat depthBufferFormat = DepthBufferFormat.fromGlEnumOrDefault(GlConst.toGlInternalId(main.getDepthTexture().getFormat()));
        this.renderTargets.resizeIfNeeded(((Blaze3dRenderTargetExt) main).iris$getDepthBufferVersion(), depthTexture, main.width, main.height, depthBufferFormat, this.packDirectives);
    }

    @Redirect(method = "beginLevelRendering", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    public RenderTarget replaceRenderTarget_(Minecraft instance) {
        return MinecraftRenderHandle.getRenderTarget().asMcRenderTarget();
    }

    @Redirect(method = "finalizeGameRendering", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    public RenderTarget replaceRenderTarget__(Minecraft instance) {
        return MinecraftRenderHandle.getRenderTarget().asMcRenderTarget();
    }
    #endif
}
