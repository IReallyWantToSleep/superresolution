package io.homo.superresolution.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.mixin.core.MinecraftAccessor;
import io.homo.superresolution.render.gl.framebuffer.FrameBuffer;
import io.homo.superresolution.render.gl.framebuffer.StorageFrameBuffer;
import io.homo.superresolution.upscale.AlgorithmManager;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/*
更改游戏渲染世界的类
*/
public class MinecraftRenderingStates {
    private final static Minecraft minecraft;
    private final static RenderTarget originRenderTarget;
    public static float frameTimeDelta = 16.6f;
    public static int currentWidth;
    public static int currentHeight;
    private static FrameBuffer renderTarget;
    private static boolean shouldScale = false;
    private static Set<RenderTarget> minecraftRenderTarget;

    static {
        minecraft = Minecraft.getInstance();
        originRenderTarget = minecraft.getMainRenderTarget();
        RenderSystem.recordRenderCall(() -> {
            renderTarget = new FrameBuffer(true);
            renderTarget.resize(
                    getRenderHeight(),
                    getRenderHeight(),
                    Minecraft.ON_OSX
            );
        });
    }

    public static void init() {
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
        return renderTarget;
    }

    public static void setShouldScale(boolean scaling) {
        if (minecraft.level == null) return;
        if (scaling == shouldScale) return;
        shouldScale = scaling;
        minecraft.getProfiler().popPush(scaling ? "startScaling" : "finishScaling");
        if (scaling) {
            setClientRenderTarget(renderTarget);
            SuperResolution.currentAlgorithm.setInputFrameBuffer(renderTarget);
            renderTarget.bindWrite(true);
        } else {
            setClientRenderTarget(SuperResolution.mainTarget);
            SuperResolution.mainTarget.bindWrite(true);
        }
        minecraft.getProfiler().popPush("level");
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
        return shouldScale && Config.enableUpscale ? Config.getRenderScaleFactor() : 1;
    }

    private static void initMinecraftRenderTarget() {
        if (minecraft.level == null) return;
        if (minecraftRenderTarget != null) {
            minecraftRenderTarget.clear();
        } else {
            minecraftRenderTarget = new HashSet<>();
        }
        minecraftRenderTarget.add(minecraft.levelRenderer.entityTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getTranslucentTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getItemEntityTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getParticlesTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getWeatherTarget());
        minecraftRenderTarget.add(minecraft.levelRenderer.getCloudsTarget());
        minecraftRenderTarget.remove(null);
    }

    private static void resize(@Nullable RenderTarget renderTarget) {
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
        minecraftRenderTarget.forEach(MinecraftRenderingStates::resize);
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
        resize(minecraft.levelRenderer.entityTarget());
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
