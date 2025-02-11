package io.homo.superresolution.common.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.mixin.core.MinecraftAccessor;
import io.homo.superresolution.common.render.gl.framebuffer.FrameBuffer;
import io.homo.superresolution.common.render.gl.framebuffer.StorageFrameBuffer;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
更改游戏渲染世界的类
*/
public class MinecraftRenderingStates {
    public static float frameTimeDelta = 16.6f;
    public static int currentWidth;
    public static int currentHeight;
    public static Set<RenderTarget> minecraftRenderTarget;
    public static Map<String, RenderTarget> minecraftRenderTargetMap = new HashMap<>();
    private static Minecraft minecraft;
    private static RenderTarget originRenderTarget;
    private static FrameBuffer renderTarget;
    private static boolean shouldScale = false;

    public static void init() {
        RenderSystem.assertOnRenderThread();
        minecraft = Minecraft.getInstance();
        originRenderTarget = minecraft.getMainRenderTarget();
        calculateSize();
        renderTarget = new StorageFrameBuffer(true);
        renderTarget.resize(
                getRenderWidth(),
                getRenderHeight(),
                Minecraft.ON_OSX
        );
    }

    public static FrameBuffer getRenderTarget() {
        return renderTarget;
    }

    public static RenderTarget getOriginRenderTarget() {
        return originRenderTarget;
    }

    public static void setShouldScale(boolean scaling) {
        if (minecraft.level == null) return;
        if (scaling == shouldScale) return;
        shouldScale = scaling;
        minecraft.getProfiler().popPush(scaling ? "startScaling" : "finishScaling");
        if (scaling) {
            //renderTarget.clear(Minecraft.ON_OSX);
            SuperResolution.LOGGER.info("0");
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            setClientRenderTarget(renderTarget);
            SuperResolution.currentAlgorithm.setInputFrameBuffer(renderTarget);
            renderTarget.bindWrite(true);
        } else {
            SuperResolution.LOGGER.info("1");
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            setClientRenderTarget(originRenderTarget);
            originRenderTarget.bindWrite(true);
        }
        minecraft.getProfiler().popPush("level");
    }

    public static boolean shouldScale() {
        return shouldScale;
    }

    private static void calculateSize() {
        if (minecraft.level == null) return;
        currentWidth = AlgorithmManager.helper.getRenderWidth();
        currentHeight = AlgorithmManager.helper.getRenderHeight();
    }

    public static void setClientRenderTarget(RenderTarget renderTarget) {
        ((MinecraftAccessor) Minecraft.getInstance()).setRenderTarget(renderTarget);
    }

    public static float getCurrentScaleFactor() {
        return shouldScale && Config.isEnableUpscale() ? Config.getRenderScaleFactor() : 1;
    }

    private static void initMinecraftRenderTarget() {
        if (minecraft.level == null) return;
        if (minecraftRenderTarget != null) {
            minecraftRenderTarget.clear();
        } else {
            minecraftRenderTarget = new HashSet<>();
        }
        if (minecraftRenderTargetMap != null) {
            minecraftRenderTargetMap.clear();
        } else {
            minecraftRenderTargetMap = new HashMap<>();
        }
        minecraftRenderTarget.add(minecraft.levelRenderer.entityTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getTranslucentTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getItemEntityTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getParticlesTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getWeatherTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getCloudsTarget());

        if (minecraft.levelRenderer.entityTarget() != null)
            minecraftRenderTargetMap.put("entityTarget", minecraft.levelRenderer.entityTarget());
        if (minecraft.levelRenderer.getTranslucentTarget() != null)
            minecraftRenderTargetMap.put("translucentTarget", minecraft.levelRenderer.getTranslucentTarget());
        if (minecraft.levelRenderer.getItemEntityTarget() != null)
            minecraftRenderTargetMap.put("itemEntityTarget", minecraft.levelRenderer.getItemEntityTarget());
        if (minecraft.levelRenderer.getParticlesTarget() != null)
            minecraftRenderTargetMap.put("particlesTarget", minecraft.levelRenderer.getParticlesTarget());
        if (minecraft.levelRenderer.getWeatherTarget() != null)
            minecraftRenderTargetMap.put("weatherTarget", minecraft.levelRenderer.getWeatherTarget());
        if (minecraft.levelRenderer.getCloudsTarget() != null)
            minecraftRenderTargetMap.put("cloudsTarget", minecraft.levelRenderer.getCloudsTarget());
        minecraftRenderTarget.remove(null);
    }

    public static void resizeRenderTarget(@Nullable RenderTarget renderTarget) {
        if (minecraft.level == null) return;
        boolean prev = shouldScale;
        shouldScale = true;
        if (renderTarget != null) {
            renderTarget.resize(
                    getRenderWidth(),
                    getRenderHeight(),
                    Minecraft.ON_OSX
            );
        }
        shouldScale = prev;
    }

    public static void resizeMinecraftRenderTarget() {
        if (minecraft.level == null) return;
        initMinecraftRenderTarget();
        minecraftRenderTarget.forEach(MinecraftRenderingStates::resizeRenderTarget);
    }

    public static void onResolutionChanged() {
        if (minecraft.level == null) return;
        updateRenderTargetSize();
    }

    public static void updateRenderTargetSize() {
        if (minecraft.level == null) return;
        renderTarget.resize(
                getRenderWidth(),
                getRenderHeight(),
                Minecraft.ON_OSX
        );
        resizeRenderTarget(minecraft.levelRenderer.entityTarget());
        calculateSize();
    }

    public static void destroy() {
        renderTarget.destroyBuffers();
    }

    public static int getRenderHeight() {
        return (int) Math.max(getScreenHeight() * Config.getRenderScaleFactor(), 1);
    }

    public static int getRenderWidth() {
        return (int) Math.max(getScreenWidth() * Config.getRenderScaleFactor(), 1);
    }

    public static int getScreenHeight() {
        return SuperResolution.getMinecraftHeight();
    }

    public static int getScreenWidth() {
        return SuperResolution.getMinecraftWidth();
    }
}
