package io.homo.superresolution.common.render;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.mixin.core.accessor.LevelRendererAccessor;
import io.homo.superresolution.common.mixin.core.accessor.MinecraftAccessor;
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
import io.homo.superresolution.common.render.gl.framebuffer.FrameBuffer;
import io.homo.superresolution.common.render.gl.framebuffer.StorageFrameBuffer;
import io.homo.superresolution.common.render.gl.texture.Texture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class MinecraftRenderHandle {
    public static boolean isRenderingWorld = false;
    private static int frameCount = 0;
    private static Minecraft minecraft;

    private static MainTarget originRenderTarget;
    private static FrameBuffer renderTarget;
    private static PostChain entityEffect;
    private static RenderTarget entityTarget;
    private static Map<RenderTargetType, RenderTarget> renderTargets = new HashMap<>();

    public static MainTarget getOriginRenderTarget() {
        return originRenderTarget;
    }

    public static FrameBuffer getRenderTarget() {
        return renderTarget;
    }

    public static void init() {
        RenderSystem.assertOnRenderThread();
        minecraft = Minecraft.getInstance();
        originRenderTarget = (MainTarget) minecraft.getMainRenderTarget();
        renderTarget = new StorageFrameBuffer(true);
        renderTarget.resize(
                getRenderWidth(),
                getRenderHeight(),
                Minecraft.ON_OSX
        );
    }

    public static void updateEntityOutline() {
        entityEffect = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getEntityEffect();
        entityTarget = Minecraft.getInstance().levelRenderer.entityTarget();
        int renderWidth = getRenderWidth();
        int renderHeight = getRenderHeight();
        for (RenderTarget renderTarget : ((PostChainAccessor) entityEffect).getFullSizedTargets()) {
            if (renderTarget.width != renderWidth ||
                    renderTarget.height != renderHeight ||
                    ((PostChainAccessor) entityEffect).getScreenWidth() != renderWidth ||
                    ((PostChainAccessor) entityEffect).getScreenHeight() != renderHeight) {
                entityEffect.resize(renderWidth, renderHeight);
                break;
            }
        }
    }

    public static void updateRenderTarget() {
        renderTargets.clear();
        for (RenderTargetType renderTargetType : RenderTargetType.values()) {
            RenderTarget renderTarget = renderTargetType.get(Minecraft.getInstance().levelRenderer);
            if (renderTarget != null) {
                renderTargets.put(
                        renderTargetType,
                        renderTarget
                );
            }
        }
    }

    public static RenderTarget getRenderTarget(RenderTargetType type) {
        return renderTargets.get(type);
    }

    public static void onInitEntityEffectBegin() {
        setClientRenderTarget(getRenderTarget());
    }

    public static void onInitEntityEffectEnd() {
        setClientRenderTarget(getOriginRenderTarget());
    }

    public static void resize() {
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        int renderWidth = getRenderWidth();
        int renderHeight = getRenderHeight();
        callOnRenderTargets((renderTarget) -> resizeRenderTarget(renderTarget, renderWidth, renderHeight), true);
    }

    private static void resizeRenderTarget(RenderTarget renderTarget, int width, int height) {
        renderTarget.resize(width, height, Minecraft.ON_OSX);
    }

    public static PostChain getEntityEffect() {
        return ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getEntityEffect();
    }

    public static void onRenderWorldBegin() {
        isRenderingWorld = true;
        if (!Config.isEnableUpscale()) return;
        updateEntityOutline();
        updateRenderTarget();
        updateRenderTargetSize();
        setClientRenderTarget(getRenderTarget());
        SuperResolution.getCurrentAlgorithm().setInputFrameBuffer(getRenderTarget());
        getRenderTarget().bindWrite(true);
    }

    public static void onRenderWorldEnd() {
        isRenderingWorld = false;
        frameCount++;
        if (!Config.isEnableUpscale()) return;
        setClientRenderTarget(getOriginRenderTarget());
        getOriginRenderTarget().bindWrite(true);
        SuperResolution.getCurrentAlgorithm().dispatch(SuperResolution.frameTimeDelta);
        SuperResolution.getCurrentAlgorithm().blitToScreen(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
    }

    public static void setClientRenderTarget(RenderTarget renderTarget) {
        if (renderTarget == null) return;
        ((MinecraftAccessor) Minecraft.getInstance()).setRenderTarget(renderTarget);
    }

    public static float getCurrentScaleFactor() {
        return isRenderingWorld && Config.isEnableUpscale() ? Config.getRenderScaleFactor() : 1;
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

    public static void callOnRenderTargets(Consumer<RenderTarget> callback) {
        renderTargets.forEach(((renderTargetType, renderTarget) -> {
            if (renderTarget != null) {
                callback.accept(renderTarget);
            }
        }));
    }

    public static void callOnRenderTargets(Consumer<RenderTarget> callback, boolean includeMainRenderTarget) {
        callOnRenderTargets(callback);
        if (includeMainRenderTarget) callback.accept(getRenderTarget());
    }

    public static void onBlitEntityEffect() {
        Texture.blitToScreen(
                getRenderWidth(),
                getRenderHeight(),
                getScreenWidth(),
                getScreenHeight(),
                getRenderTarget(RenderTargetType.ENTITY).getColorTextureId()
        );
    }

    public static void updateRenderTargetSize() {
        final int FRAME = 3;
        if (frameCount % FRAME == 0) {
            int renderWidth = getRenderWidth();
            int renderHeight = getRenderHeight();
            callOnRenderTargets(
                    (renderTarget) -> {
                        if (renderTarget.width != renderWidth || renderTarget.height != renderHeight) {
                            renderTarget.resize(
                                    renderWidth,
                                    renderHeight,
                                    Minecraft.ON_OSX
                            );
                        }
                    }
            );
        }
    }
}
