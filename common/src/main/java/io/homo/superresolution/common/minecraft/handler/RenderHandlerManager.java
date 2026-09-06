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

package io.homo.superresolution.common.minecraft.handler;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.LevelRenderEndEvent;
import io.homo.superresolution.api.event.LevelRenderStartEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.CallType;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.minecraft.GameFrameIndex;
import io.homo.superresolution.common.minecraft.MinecraftRenderTargetWrapper;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.mixin.core.accessor.MinecraftAccessor;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.workmode.SRWorkModeProvider;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

public class RenderHandlerManager {
    private static boolean isRenderingWorld;
    private static boolean shouldApplyScale;
    private static boolean worldDebugGroupPushed;
    private static Minecraft minecraft;
    private static IMinecraftRenderHandler handler;
    private static String handlerProviderId;
    private static IBindableFrameBuffer originRenderTarget;
    private static boolean uiOnlyB3DVulkan;

    private static boolean needResize;
    public static void initialize() {
        RenderSystem.assertOnRenderThread();
        minecraft = Minecraft.getInstance();
        uiOnlyB3DVulkan = B3DVulkanBridge.isB3DVulkanBackend();
        if (uiOnlyB3DVulkan) {
            originRenderTarget = null;
            return;
        }
        originRenderTarget = MinecraftRenderTargetWrapper.of(MinecraftUtils.getMainRenderTarget());
        updateHandler();
    }

    public static void resize() {
        if (uiOnlyB3DVulkan) {
            return;
        }
        updateHandler();
        if (handler != null) {
            handler.resize();
        }
    }


    private static boolean needUpdateHandler(SRWorkModeProvider provider) {
        if (provider == null) {
            return handler != null;
        }
        return handler == null || !provider.id().equals(handlerProviderId);
    }

    public static void updateHandler() {
        if (uiOnlyB3DVulkan) {
            return;
        }
        SRWorkModeProvider provider = SRWorkModeManager.getCurrentProvider();
        if (!needUpdateHandler(provider)) {
            return;
        }
        if (handler != null) {
            handler.destroy();
        }
        handler = null;
        handlerProviderId = null;
        shouldApplyScale = false;
        worldDebugGroupPushed = false;
        if (provider == null) {
            return;
        }
        handler = provider.createRenderHandler();
        handlerProviderId = provider.id();
        handler.initialize();
        needResize = true;
    }

    public static void onFrameBegin() {
        if (uiOnlyB3DVulkan) {
            return;
        }
        if (needResize) {
            MinecraftUtils.resize();
            PresentationBackendManager.flushCapturedFrame();
            SuperResolution.getCurrentAlgorithm().resize(
                    RenderHandlerManager.getScreenWidth(),
                    RenderHandlerManager.getScreenHeight()
            );
            SuperResolution.cachedWidth = RenderHandlerManager.getScreenWidth();
            SuperResolution.cachedHeight = RenderHandlerManager.getScreenHeight();
            needResize = false;
        }
    }

    public static void onFrameEnd() {
    }

    public static void onRenderWorldBegin(CallType type) {
        if (uiOnlyB3DVulkan) {
            return;
        }
        updateHandler();
        if (handler == null) {
            return;
        }
        if (type == CallType.LEVEL_RENDERER) {
            isRenderingWorld = true;
        }
        if (!checkRenderWorldCallPos(type)) {
            return;
        }

        GlDebug.pushGroup(74108435, "MinecraftLevelRender");
        worldDebugGroupPushed = true;
        shouldApplyScale = true;
        SuperResolutionAPI.EVENT_BUS.post(new LevelRenderStartEvent());
        handler.onRenderWorldBegin(type);
    }

    public static void onRenderWorldEnd(CallType type) {
        if (uiOnlyB3DVulkan) {
            return;
        }
        if (type == CallType.LEVEL_RENDERER) {
            isRenderingWorld = false;
        }
        if (handler == null) {
            worldDebugGroupPushed = false;
            return;
        }
        if (checkRenderWorldCallPos(type)) {
            handler.onRenderWorldEnd(type);
            SuperResolutionAPI.EVENT_BUS.post(new LevelRenderEndEvent());
            shouldApplyScale = false;
        }
        if (worldDebugGroupPushed) {
            GlDebug.popGroup();
            worldDebugGroupPushed = false;
        }
    }

    public static void onRenderHandBegin() {
        if (uiOnlyB3DVulkan) {
            return;
        }
        if (checkRenderHandCallPos() && handler != null) {
            handler.onRenderHandBegin();
        }
    }

    public static void onRenderHandEnd() {
        if (uiOnlyB3DVulkan) {
            return;
        }
        if (checkRenderHandCallPos() && handler != null) {
            handler.onRenderHandEnd();
        }
    }

    public static void onProcessPostChain(PostChain postChain) {
        if (uiOnlyB3DVulkan) {
            return;
        }
        updateHandler();
        if (handler == null) {
            return;
        }
        handler.onProcessPostChain(postChain);
    }

    public static int getFrameCount() {
        return GameFrameIndex.current();
    }

    private static boolean checkRenderWorldCallPos(CallType type) {
        return switch (SuperResolutionConfig.getCaptureMode()) {
            case A, C -> type == CallType.GAME_RENDERER;
            case B -> type == CallType.LEVEL_RENDERER;
        };
    }

    private static boolean checkRenderHandCallPos() {
        return switch (SuperResolutionConfig.getCaptureMode()) {
            case A, B -> false;
            case C -> true;
        } && !SRWorkModeManager.getCurrentState().shaderPackInUse();
    }

    public static void setClientRenderTarget(RenderTarget renderTarget) {
        if (renderTarget == null) {
            throw new RuntimeException();
        }
        #if MC_VER < MC_26_2
        ((MinecraftAccessor) Minecraft.getInstance()).setRenderTarget(renderTarget);
        #endif
    }

    public static float getCurrentScaleFactor() {
        return shouldApplyScale && minecraft.level != null ? getScaleFactor() : 1;
    }

    public static float getScaleFactor() {
        // 仅帧生成模式（None 算法）：禁用渲染比例，光影包按原生分辨率渲染
        if (AlgorithmDescriptions.NONE.equals(SuperResolutionConfig.getUpscaleAlgorithm())
                && SRWorkModeManager.getCurrentState().supportsFrameGeneration()) {
            return 1;
        }
        return SuperResolutionConfig.isEnableUpscale() ? SuperResolutionConfig.getRenderScaleFactor() : 1;
    }

    // 某些算法的最小输入尺寸为32x32（比如DLSS），但Minecraft几乎不会小于这个尺寸
    // 当然，除了Windows上最小化窗口时😅，所以这里直接写死32
    // fuck Windows & Microsoft
    public static int getRenderHeight() {
        return (int) Math.max(getScreenHeight() * getScaleFactor(), 32);
    }

    public static int getRenderWidth() {
        return (int) Math.max(getScreenWidth() * getScaleFactor(), 32);
    }

    public static int getScreenHeight() {
        return Math.max(MinecraftWindow.getWindowHeight(), 32);
    }

    public static int getScreenWidth() {
        return Math.max(MinecraftWindow.getWindowWidth(), 32);
    }

    public static Vector2i getScreenSize() {
        return new Vector2i(
                getScreenWidth(),
                getScreenHeight()
        );
    }

    public static Vector2i getRenderSize() {
        return new Vector2i(
                getRenderWidth(),
                getRenderHeight()
        );
    }

    public static IBindableFrameBuffer getOriginRenderTarget() {
        return originRenderTarget;
    }

    public static IBindableFrameBuffer getRenderTarget() {
        if (handler == null) {
            return originRenderTarget;
        }
        return handler.getScaledRenderTarget();
    }

    @Nullable
    public static ITexture getColorTexture() {
        if (handler == null) {
            return null;
        }
        return handler.getColorTexture();
    }

    @Nullable
    public static ITexture getDepthTexture() {
        if (handler == null) {
            return null;
        }
        return handler.getDepthTexture();
    }

    public static IMinecraftRenderHandler getHandler() {
        return handler;
    }
}
