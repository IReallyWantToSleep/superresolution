/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.mixin.core;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
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
        if (ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) return;

        if (!screenTarget.equals(RenderHandlerManager.getOriginRenderTarget().asMcRenderTarget())) {
            return;
        }
        this.passes.forEach(PostPass::close);
        this.passes.clear();
        this.customRenderTargets.values().forEach(RenderTarget::destroyBuffers);
        this.customRenderTargets.clear();
        ((PostChainAccessor) this).setScreenTarget(RenderHandlerManager.getRenderTarget().asMcRenderTarget());
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
        if (ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) return;

        if (!screenTarget.equals(RenderHandlerManager.getOriginRenderTarget().asMcRenderTarget())) {
            return;
        }
        this.passes.forEach(PostPass::close);
        this.passes.clear();
        this.customRenderTargets.values().forEach(RenderTarget::destroyBuffers);
        this.customRenderTargets.clear();
        ((PostChainAccessor) this).setScreenTarget(RenderHandlerManager.getRenderTarget().asMcRenderTarget());
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
        if (ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) return;
        if (
                width != RenderHandlerManager.getRenderWidth() ||
                        height != RenderHandlerManager.getRenderHeight()
        ) {
            this.resize(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight());
            ci.cancel();
        }
    }

    @Inject(method = "process", at = @At("HEAD"))
    public void onProcess(float partialTicks, CallbackInfo ci) {
        if (super_resolution$onBlackList()) return;
        if (ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) {
            ((PostChainAccessor) this).setScreenTarget(RenderHandlerManager.getOriginRenderTarget().asMcRenderTarget());
            return;
        }
        RenderHandlerManager.onProcessPostChain((PostChain) (Object) this);
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
