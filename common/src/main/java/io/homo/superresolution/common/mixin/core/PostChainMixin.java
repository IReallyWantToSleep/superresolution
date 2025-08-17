package io.homo.superresolution.common.mixin.core;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(PostChain.class)
public abstract class PostChainMixin {
    #if MC_VER < MC_1_21_4
    @Unique
    private List<String> super_resolution$blackList = null;
    @Shadow
    @Final
    private List<PostPass> passes;
    @Shadow
    @Final
    private Map<String, RenderTarget> customRenderTargets;
    @Shadow
    @Final
    private RenderTarget screenTarget;
    @Shadow
    @Final
    private String name;

    #if MC_VER >= MC_1_20_6
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void onInitPostChain(
            TextureManager textureManager, net.minecraft.server.packs.resources.ResourceProvider resourceProvider, RenderTarget screenTarget, ResourceLocation resourceLocation, CallbackInfo ci
    ) throws IOException, JsonSyntaxException {
        if (super_resolution$onBlackList()) return;
        if (SuperResolution.isShaderPackCompatSuperResolution()) return;

        if (!screenTarget.equals(MinecraftRenderHandle.getOriginRenderTarget().asMcRenderTarget())) {
            return;
        }
        this.passes.forEach(PostPass::close);
        this.passes.clear();
        this.customRenderTargets.values().forEach(RenderTarget::destroyBuffers);
        this.customRenderTargets.clear();
        ((PostChainAccessor) this).setScreenTarget(MinecraftRenderHandle.getRenderTarget().asMcRenderTarget());
        this.updateOrthoMatrix();
        this.load(textureManager, resourceLocation);
        SuperResolution.LOGGER.info("已注入PostChain {}", this.name);
    }

    #else
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void onInitPostChain(
            TextureManager textureManager,
            net.minecraft.server.packs.resources.ResourceManager resourceManager,
            RenderTarget screenTarget,
            ResourceLocation name,
            CallbackInfo ci
    ) throws IOException, JsonSyntaxException {
        if (super_resolution$onBlackList()) return;
        if (SuperResolution.isShaderPackCompatSuperResolution()) return;

        if (!screenTarget.equals(MinecraftRenderHandle.getOriginRenderTarget().asMcRenderTarget())) {
            return;
        }
        this.passes.forEach(PostPass::close);
        this.passes.clear();
        this.customRenderTargets.values().forEach(RenderTarget::destroyBuffers);
        this.customRenderTargets.clear();
        ((PostChainAccessor) this).setScreenTarget(MinecraftRenderHandle.getRenderTarget().asMcRenderTarget());
        this.updateOrthoMatrix();
        this.load(textureManager, name);
        SuperResolution.LOGGER.info("已注入PostChain {}", this.name);
    }
    #endif

    @Shadow
    public abstract void resize(int width, int height);

    @Shadow
    protected abstract void updateOrthoMatrix();

    @Shadow
    protected abstract void load(TextureManager textureManager, ResourceLocation resourceLocation) throws IOException, JsonSyntaxException;

    @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    public void onResize(int width, int height, CallbackInfo ci) {
        if (super_resolution$onBlackList()) return;
        if (SuperResolution.isShaderPackCompatSuperResolution()) return;
        if (
                width != MinecraftRenderHandle.getRenderWidth() ||
                        height != MinecraftRenderHandle.getRenderHeight()
        ) {
            this.resize(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight());
            ci.cancel();
        }
    }

    @Inject(method = "process", at = @At("HEAD"))
    public void onProcess(float partialTicks, CallbackInfo ci) {
        if (super_resolution$onBlackList()) return;
        if (SuperResolution.isShaderPackCompatSuperResolution()) {
            ((PostChainAccessor) this).setScreenTarget(MinecraftRenderHandle.getOriginRenderTarget().asMcRenderTarget());
            return;
        }
        MinecraftRenderHandle.fixPostChain((PostChain) (Object) this);
    }

    @Unique
    private boolean super_resolution$onBlackList() {
        if (super_resolution$blackList == null) {
            super_resolution$blackList = new ArrayList<>();

            super_resolution$blackList.add("minecraft:shaders/post/modern_gaussian_blur.json");
            super_resolution$blackList.add("minecraft:shaders/post/blur.json");
            super_resolution$blackList.add("colorblindness:shaders/post/achromatomaly.json");
            super_resolution$blackList.add("colorblindness:shaders/post/achromatopsia.json");
            super_resolution$blackList.add("colorblindness:shaders/post/deuteranomaly.json");
            super_resolution$blackList.add("colorblindness:shaders/post/deuteranopia.json");
            super_resolution$blackList.add("colorblindness:shaders/post/protanomaly.json");
            super_resolution$blackList.add("colorblindness:shaders/post/protanopia.json");
            super_resolution$blackList.add("colorblindness:shaders/post/tritanomaly.json");
            super_resolution$blackList.add("colorblindness:shaders/post/tritanopia.json");

            super_resolution$blackList.addAll(SuperResolutionConfig.getInjectPostChainBlackList());
        }

        return super_resolution$blackList.contains(name);
    }
    #endif
}
