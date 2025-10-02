/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.common.minecraft.handler;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.minecraft.CallType;
import io.homo.superresolution.common.minecraft.MinecraftRenderTargetType;
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ShaderCompatHandler implements IMinecraftRenderHandler {
    private final Map<MinecraftRenderTargetType, IBindableFrameBuffer> renderTargets = new HashMap<>();

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

    public static boolean isShaderPackCompatSuperResolution() {
        try {
            Class<?> irisApiClazz = Class.forName("io.homo.superresolution.shadercompat.IrisShaderPipelineHandle");
            return (Boolean) irisApiClazz.getMethod("shouldApplySuperResolutionChanges").invoke(null);
        } catch (Throwable e) {
            return false;
        }
    }

    public static Optional<SRShaderCompatConfig> getShaderPackCompatConfig() {
        try {
            Class<?> irisApiClazz = Class.forName("io.homo.superresolution.shadercompat.IrisShaderPipelineHandle");
            return (Optional<SRShaderCompatConfig>) irisApiClazz.getMethod("getCurrentShaderPackConfig").invoke(null);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public static Optional<SRShaderCompatConfig.WorldConfig> getCurrentLevelCompatConfig() {
        try {
            Class<?> irisApiClazz = Class.forName("io.homo.superresolution.shadercompat.ShaderCompatUpscaleDispatcher");
            return Optional.ofNullable((SRShaderCompatConfig.WorldConfig) irisApiClazz.getMethod("getCurrentConfig").invoke(null));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public static boolean isShaderPackCompatSuperResolutionJitter() {
        try {
            Class<?> irisApiClazz = Class.forName("io.homo.superresolution.shadercompat.IrisShaderPipelineHandle");
            return (Boolean) irisApiClazz.getMethod("shouldApplySuperResolutionChangesJitter").invoke(null);
        } catch (Throwable e) {
            return false;
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
                        renderTarget.resizeFrameBuffer(
                                screenWidth,
                                screenHeight
                        );
                    }
                }
        );
        IFrameBuffer handRenderTarget = getRenderTarget(MinecraftRenderTargetType.HAND);
        if (handRenderTarget.getWidth() != screenWidth || handRenderTarget.getHeight() != screenHeight) {
            handRenderTarget.resizeFrameBuffer(
                    screenWidth,
                    screenHeight
            );
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
        for (RenderTarget renderTarget : ((PostChainAccessor) postChain).getFullSizedTargets()) {
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
        return Class.forName("io.homo.superresolution.shadercompat.ShaderCompatUpscaleDispatcher");
    }

    public ITexture getColorTexture() {
        try {
            Class<?> dispatcherClass = getShaderCompatUpscaleDispatcher();
            return ((TextureInfo) dispatcherClass.getField("colorTexture").get(null)).getInternalTexture();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public ITexture getDepthTexture() {
        try {
            Class<?> dispatcherClass = getShaderCompatUpscaleDispatcher();
            return ((TextureInfo) dispatcherClass.getField("depthTexture").get(null)).getInternalTexture();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
