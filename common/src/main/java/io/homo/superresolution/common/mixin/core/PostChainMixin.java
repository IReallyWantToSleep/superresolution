package io.homo.superresolution.common.mixin.core;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Mixin(PostChain.class)
public abstract class PostChainMixin {
    #if MC_VER < MC_1_21_4
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
    #else
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void onInitPostChain(
            TextureManager textureManager,
            ResourceProvider resourceProvider,
            RenderTarget screenTarget,
            ResourceLocation resourceLocation,
            CallbackInfo ci
    ) throws IOException, JsonSyntaxException {
        if (onBlackList())return;

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
    #endif

    #if MC_VER < MC_1_21_1
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void onInitPostChain(
            TextureManager textureManager,
            ResourceManager resourceManager,
            RenderTarget screenTarget,
            ResourceLocation name,
            CallbackInfo ci
    ) throws IOException, JsonSyntaxException {
        if (onBlackList()) return;

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

    @Shadow
    public abstract void resize(int width, int height);

    @Shadow
    protected abstract void updateOrthoMatrix();

    @Shadow
    protected abstract void load(TextureManager textureManager, ResourceLocation resourceLocation) throws IOException, JsonSyntaxException;

    @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    public void onResize(int width, int height, CallbackInfo ci) {
        if (onBlackList()) return;

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
        if (onBlackList()) return;
        MinecraftRenderHandle.fixPostChain((PostChain) (Object) this);
    }

    private boolean onBlackList() {
        return Config.getInjectPostChainBlackList().contains(name);
    }
    #endif
}
