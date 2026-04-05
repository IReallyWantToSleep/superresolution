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

package io.homo.superresolution.common.minecraft.handler.shadercompat;

import io.homo.superresolution.common.minecraft.CallType;
import io.homo.superresolution.common.minecraft.MinecraftRenderTargetType;
import io.homo.superresolution.common.minecraft.MinecraftRenderTargetWrapper;
import io.homo.superresolution.common.minecraft.handler.IMinecraftRenderHandler;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ShaderCompatHandler implements IMinecraftRenderHandler {
    private final Map<MinecraftRenderTargetType, IBindableFrameBuffer> renderTargets = new HashMap<>();

    private static boolean isLoadingShader;
    public static boolean isLoadingShader() {
        return isLoadingShader;
    }

    public static void setLoadingShader(boolean loadingShader) {
        isLoadingShader = loadingShader;
    }
    public static void irisApiReloadShader() {
        try {
            Class<?> irisApiClazz = Class.forName("net.irisshaders.iris.Iris");
            irisApiClazz.getMethod("reload").invoke(null);
        } catch (Throwable ignored) {
        }
    }

    public static boolean irisApiIsShaderPackInUse() {
        try {
            Class<?> irisApiClazz = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object irisApiInstance = irisApiClazz.getMethod("getInstance").invoke(null);
            return (boolean) irisApiClazz.getMethod("isShaderPackInUse").invoke(irisApiInstance);
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean dontHackMinecraftRenderingPipeline() {
        try {
            Class<?> irisApiClazz = Class.forName("io.homo.superresolution.shadercompat.IrisShaderCompatUtils");
            return (Boolean) irisApiClazz.getMethod("shouldApplySuperResolutionChanges").invoke(null);
        } catch (Throwable e) {
            return false;
        }
    }

    public static Optional<SRShaderCompatData> getShaderPackCompatConfig() {
        try {
            Class<?> irisApiClazz = Class.forName("io.homo.superresolution.shadercompat.IrisShaderCompatUtils");
            return (Optional<SRShaderCompatData>) irisApiClazz.getMethod("getCurrentShaderPackConfig").invoke(null);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public static Optional<SRShaderCompatData.WorldProfile> getCurrentLevelCompatConfig() {
        try {
            Class<?> irisApiClazz = Class.forName("io.homo.superresolution.shadercompat.IrisShaderCompatUtils");
            Object result = irisApiClazz.getMethod("getCurrentConfig").invoke(null);
            // getCurrentConfig() 返回 Optional<WorldProfile>，需要先转换为 Optional 再取内容
            Optional<?> opt = (Optional<?>) result;
            return opt.map(o -> (SRShaderCompatData.WorldProfile) o);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public void updateRenderTarget() {
        renderTargets.clear();
        for (MinecraftRenderTargetType minecraftRenderTargetType : MinecraftRenderTargetType.values()) {
            IBindableFrameBuffer renderTarget = minecraftRenderTargetType.get(Minecraft.getInstance().levelRenderer);
            if (renderTarget != null) {
                renderTargets.put(
                        minecraftRenderTargetType,
                        renderTarget
                );
            }
        }
    }

    public void callOnRenderTargets(Consumer<IFrameBuffer> callback) {
        renderTargets.forEach(((minecraftRenderTargetType, renderTarget) -> {
            if (renderTarget != null && minecraftRenderTargetType != MinecraftRenderTargetType.HAND) {
                callback.accept(renderTarget);
            }
        }));
    }

    public void updateRenderTargetSize() {
        int screenWidth = RenderHandlerManager.getScreenWidth();
        int screenHeight = RenderHandlerManager.getScreenHeight();
        callOnRenderTargets(
                (renderTarget) -> {
                    if (renderTarget.getWidth() != screenWidth || renderTarget.getHeight() != screenHeight) {
                        if (renderTarget instanceof MinecraftRenderTargetWrapper wrapper) {
                            wrapper.resizeFrameBuffer(screenWidth, screenHeight);
                        } else if (renderTarget instanceof GlFrameBuffer glFbo) {
                            glFbo.resizeFrameBuffer(screenWidth, screenHeight);
                        }
                    }
                }
        );
        IFrameBuffer handRenderTarget = getRenderTarget(MinecraftRenderTargetType.HAND);
        if (handRenderTarget != null && (handRenderTarget.getWidth() != screenWidth || handRenderTarget.getHeight() != screenHeight)) {
            if (handRenderTarget instanceof GlFrameBuffer glFbo) {
                glFbo.resizeFrameBuffer(screenWidth, screenHeight);
            }
        }
    }


    public IBindableFrameBuffer getRenderTarget(MinecraftRenderTargetType type) {
        return renderTargets.get(type);
    }


    @Override
    public void onRenderWorldBegin(CallType type) {
        updateRenderTarget();
        updateRenderTargetSize();
    }


    @Override
    public void onRenderWorldEnd(CallType type) {
        updateRenderTarget();
        updateRenderTargetSize();
    }

    @Override
    public void onRenderHandBegin() {

    }

    @Override
    public void onRenderHandEnd() {

    }

    @Override
    public void onProcessPostChain(PostChain postChain) {
        #if MC_VER < MC_1_21_4
        int renderWidth = RenderHandlerManager.getScreenWidth();
        int renderHeight = RenderHandlerManager.getScreenHeight();
        //修复PostChain中的RenderTarget大小不正确
        for (com.mojang.blaze3d.pipeline.RenderTarget renderTarget : ((PostChainAccessor) postChain).getFullSizedTargets()) {
            if (renderTarget.width != renderWidth ||
                    renderTarget.height != renderHeight ||
                    ((PostChainAccessor) postChain).getScreenWidth() != renderWidth ||
                    ((PostChainAccessor) postChain).getScreenHeight() != renderHeight) {
                postChain.resize(renderWidth, renderHeight);
                break;
            }
        }
        #endif
    }

    @Override
    public IBindableFrameBuffer getFullSizeRenderTarget() {
        return RenderHandlerManager.getOriginRenderTarget();
    }

    @Override
    public IBindableFrameBuffer getScaledRenderTarget() {
        return RenderHandlerManager.getOriginRenderTarget();
    }

    @Override
    public void initialize() {

    }

    @Override
    public void resize() {

    }

    @Override
    public void destroy() {

    }

    private Class<?> getShaderCompatUpscaleDispatcher() throws ClassNotFoundException {
        return Class.forName("io.homo.superresolution.shadercompat.IrisShaderCompatUpscaleDispatcher");
    }

    @Nullable
    public ITexture getColorTexture() {
        try {
            Class<?> dispatcherClass = getShaderCompatUpscaleDispatcher();
            ShaderCompatTextureInfo textureInfo = ((ShaderCompatTextureInfo) dispatcherClass.getField("colorTexture").get(null));
            if (textureInfo == null) return null;
            return textureInfo.getInternalTexture();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public ITexture getDepthTexture() {
        try {
            Class<?> dispatcherClass = getShaderCompatUpscaleDispatcher();
            ShaderCompatTextureInfo textureInfo = ((ShaderCompatTextureInfo) dispatcherClass.getField("depthTexture").get(null));
            if (textureInfo == null) return null;
            return textureInfo.getInternalTexture();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
