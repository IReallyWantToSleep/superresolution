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
import com.mojang.blaze3d.systems.RenderSystem;
import com.sun.jna.Pointer;
import io.homo.superresolution.api.event.LevelRenderEndEvent;
import io.homo.superresolution.api.event.LevelRenderStartEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.CallType;
import io.homo.superresolution.common.minecraft.MinecraftRenderTargetWrapper;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.mixin.core.accessor.MinecraftAccessor;
import io.homo.superresolution.common.perf.PerformanceRecoder;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;

public class RenderHandlerManager {
    public static boolean needCapture = false;
    public static boolean needCaptureVulkan = false;
    public static boolean needCaptureUpscale = false;
    private static boolean isRenderingWorld;
    private static boolean shouldApplyScale;
    private static Minecraft minecraft;
    private static IMinecraftRenderHandler handler;
    public static int frameCount = 0;
    private static IBindableFrameBuffer originRenderTarget;

    public static void initialize() {
        RenderSystem.assertOnRenderThread();
        minecraft = Minecraft.getInstance();
        originRenderTarget = MinecraftRenderTargetWrapper.of(minecraft.getMainRenderTarget());
        PerformanceRecoder.initialize();
        updateHandler();
    }

    public static void resize() {
        updateHandler();
        handler.resize();
    }


    private static boolean needUpdateHandler() {
        if (handler == null) return true;
        if (
                handler instanceof MinecraftRenderHandler &&
                        ShaderCompatHandler.isShaderPackCompatSuperResolution()) {
            return true;
        }
        if (
                handler instanceof ShaderCompatHandler &&
                        !ShaderCompatHandler.isShaderPackCompatSuperResolution()) {
            return true;
        }
        return false;
    }

    private static void updateHandler() {
        if (needUpdateHandler()) {
            if (handler != null) {
                handler.destroy();
                handler = null;
            }
            if (ShaderCompatHandler.isShaderPackCompatSuperResolution()) {
                handler = new ShaderCompatHandler();
            } else {
                handler = new MinecraftRenderHandler();
            }
            minecraft.resizeDisplay();
            handler.initialize();
        }
    }

    public static void onRenderBegin() {
        PerformanceRecoder.beginFrame();
    }

    public static void onRenderEnd() {
        PerformanceRecoder.endFrame();
        frameCount++;
    }

    public static void onRenderWorldBegin(CallType type) {
        updateHandler();
        GlDebug.pushGroup(74108435, "MinecraftLevelRender");
        if (SuperResolution.cachedWidth != RenderHandlerManager.getScreenWidth() || SuperResolution.cachedHeight != RenderHandlerManager.getScreenHeight()) {
            SuperResolution.getInstance().resize(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
        }
        if (type == CallType.LEVEL_RENDERER) isRenderingWorld = true;
        if (!checkRenderWorldCallPos(type)) return;
        PerformanceRecoder.beginWorld();

        shouldApplyScale = true;
        if (RenderHandlerManager.needCapture) {
            if (RenderDoc.renderdoc != null) {
                RenderDoc.renderdoc.StartFrameCapture.call(null, null);
            }
        }
        if (RenderHandlerManager.needCaptureVulkan) {
            if (RenderDoc.renderdoc != null) {
                if (RenderSystems.vulkan() != null) {
                    RenderDoc.renderdoc.StartFrameCapture.call(
                            new Pointer(RenderSystems.vulkan().getVulkanInstance().address()),
                            null
                    );
                }
            }
        }
        if (LevelRenderStartEvent.EVENT.hasEvent()) {
            LevelRenderStartEvent.EVENT.invoker().onLevelRenderStart();
        }
        handler.onRenderWorldBegin(type);
    }

    public static void onRenderWorldEnd(CallType type) {
        if (type == CallType.LEVEL_RENDERER) isRenderingWorld = false;
        if (checkRenderWorldCallPos(type)) {
            PerformanceRecoder.endWorld();
            handler.onRenderWorldEnd(type);
            if (LevelRenderEndEvent.EVENT.hasEvent()) {
                LevelRenderEndEvent.EVENT.invoker().onLevelRenderEnd();
            }
            if (RenderHandlerManager.needCapture) {
                if (RenderDoc.renderdoc != null) {
                    RenderHandlerManager.needCapture = false;
                    RenderDoc.renderdoc.EndFrameCapture.call(null, null);
                }
            }
            if (RenderHandlerManager.needCaptureVulkan) {
                if (RenderDoc.renderdoc != null) {
                    if (RenderSystems.vulkan() != null) {
                        RenderHandlerManager.needCaptureVulkan = false;
                        RenderDoc.renderdoc.EndFrameCapture.call(
                                new Pointer(RenderSystems.vulkan().getVulkanInstance().address()),
                                null
                        );
                    }
                }
            }
            shouldApplyScale = false;
        }
        GlDebug.popGroup();
    }

    public static void onRenderHandBegin() {
        if (checkRenderHandCallPos()) {
            handler.onRenderHandBegin();
        }
    }

    public static void onRenderHandEnd() {
        if (checkRenderHandCallPos()) {
            handler.onRenderHandEnd();
        }
    }

    public static void onProcessPostChain(PostChain postChain) {
        updateHandler();
        handler.onProcessPostChain(postChain);
    }

    public static void needCapture() {
        needCapture = true;
    }

    public static void needCaptureVulkan() {
        needCaptureVulkan = true;
    }

    public static void needCaptureUpscale() {
        needCaptureUpscale = true;
    }

    public static int getFrameCount() {
        return frameCount;
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
        } && !Platform.currentPlatform.iris().isShaderPackInUse();
    }

    public static void setClientRenderTarget(RenderTarget renderTarget) {
        if (renderTarget == null) {
            throw new RuntimeException();
        }
        ((MinecraftAccessor) Minecraft.getInstance()).setRenderTarget(renderTarget);
    }

    public static float getCurrentScaleFactor() {
        return shouldApplyScale && minecraft.level != null ? getScaleFactor() : 1;
    }

    public static float getScaleFactor() {
        return SuperResolutionConfig.isEnableUpscale() ? SuperResolutionConfig.getRenderScaleFactor() : 1;
    }

    public static int getRenderHeight() {
        return (int) Math.max(getScreenHeight() * getScaleFactor(), 1);
    }

    public static int getRenderWidth() {
        return (int) Math.max(getScreenWidth() * getScaleFactor(), 1);
    }

    public static int getScreenHeight() {
        return Math.max(MinecraftWindow.getWindowHeight(), 1);
    }

    public static int getScreenWidth() {
        return Math.max(MinecraftWindow.getWindowWidth(), 1);
    }

    public static IBindableFrameBuffer getOriginRenderTarget() {
        return originRenderTarget;
    }

    public static IBindableFrameBuffer getRenderTarget() {
        return handler.getScaledRenderTarget();
    }

    public static ITexture getColorTexture() {
        return handler.getColorTexture();
    }

    public static ITexture getDepthTexture() {
        return handler.getDepthTexture();
    }
}
