package io.homo.superresolution.common.render;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.mixin.core.accessor.LevelRendererAccessor;
import io.homo.superresolution.common.mixin.core.accessor.MinecraftAccessor;
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.render.gl.Gl;
import io.homo.superresolution.common.render.gl.GlConst;
import io.homo.superresolution.common.render.gl.GlState;
import io.homo.superresolution.common.render.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.common.render.gl.framebuffer.StorageFrameBuffer;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MinecraftRenderHandle {
    private static final Map<RenderTargetType, RenderTarget> renderTargets = new HashMap<>();
    public static boolean isRenderingWorld = false;
    public static float frameTime;
    private static int frameCount = 0;
    private static Minecraft minecraft;
    private static MainTarget originRenderTarget;
    private static GlFrameBuffer renderTarget;
    private static PostChain entityEffect;
    private static RenderTarget entityTarget;
    private static float frameStartTime;
    private static float frameEndTime;

    public static MainTarget getOriginRenderTarget() {
        return originRenderTarget;
    }

    public static GlFrameBuffer getRenderTarget() {
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
        callOnRenderTargets((renderTarget) -> resizeRenderTarget(renderTarget, renderWidth, renderHeight), false);
    }

    private static void resizeRenderTarget(RenderTarget renderTarget, int width, int height) {
        renderTarget.resize(width, height, Minecraft.ON_OSX);
    }

    public static PostChain getEntityEffect() {
        return ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getEntityEffect();
    }

    private static boolean checkRenderWorldCallPos(CallType type) {
        return switch (Config.getCaptureMode()) {
            case A, C -> type == CallType.GAME_RENDERER;
            case B -> type == CallType.LEVEL_RENDERER;
        };
    }

    private static boolean checkRenderHandCallPos() {
        return switch (Config.getCaptureMode()) {
            case A, B -> false;
            case C -> true;
        } && !Platform.currentPlatform.iris().isShaderPackInUse();
    }

    public static void onRenderWorldBegin(CallType type) {
        if (!checkRenderWorldCallPos(type)) return;
        isRenderingWorld = true;
        frameStartTime = (float) (Util.getNanos() / 1000000);
        updateEntityOutline();
        updateRenderTarget();
        updateRenderTargetSize();
        setClientRenderTarget(getRenderTarget());
        getRenderTarget(RenderTargetType.HAND).clear(Minecraft.ON_OSX);
        SuperResolution.getCurrentAlgorithm().setInputFrameBuffer(getRenderTarget());
        getRenderTarget().bindWrite(true);
    }

    public static void onRenderWorldEnd(CallType type) {
        if (!checkRenderWorldCallPos(type)) return;
        isRenderingWorld = false;
        frameCount++;
        setClientRenderTarget(getOriginRenderTarget());
        getOriginRenderTarget().bindWrite(true);
        AlgorithmManager.update();
        SuperResolution.getCurrentAlgorithm().dispatch(AlgorithmManager.getDispatchResource());
        SuperResolution.getCurrentAlgorithm().blitToScreen(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        if (Config.getCaptureMode() == CaptureMode.C &&
                Platform.currentPlatform.iris().isShaderPackInUse())
            blitHandRenderTarget();
        frameEndTime = (float) (Util.getNanos() / 1000000);
        frameTime = frameEndTime - frameStartTime;
    }

    public static void setClientRenderTarget(RenderTarget renderTarget) {
        if (renderTarget == null) return;
        ((MinecraftAccessor) Minecraft.getInstance()).setRenderTarget(renderTarget);
    }

    public static float getCurrentScaleFactor() {
        return isRenderingWorld && Config.isEnableUpscale() ? Config.getRenderScaleFactor() : 1;
    }


    public static int getRenderHeight() {
        return (int) Math.max(getScreenHeight() * getCurrentScaleFactor(), 1);
    }

    public static int getRenderWidth() {
        return (int) Math.max(getScreenWidth() * getCurrentScaleFactor(), 1);
    }

    public static int getScreenHeight() {
        return Math.max(SuperResolution.getMinecraftHeight(), 1);
    }

    public static int getScreenWidth() {
        return Math.max(SuperResolution.getMinecraftWidth(), 1);
    }

    public static void callOnRenderTargets(Consumer<RenderTarget> callback) {
        renderTargets.forEach(((renderTargetType, renderTarget) -> {
            if (renderTarget != null && renderTargetType != RenderTargetType.HAND) {
                callback.accept(renderTarget);
            }
        }));
    }

    public static void callOnRenderTargets(BiConsumer<RenderTarget, RenderTargetType> callback) {
        renderTargets.forEach(((renderTargetType, renderTarget) -> {
            if (renderTarget != null) {
                callback.accept(renderTarget, renderTargetType);
            }
        }));
    }

    public static void callOnRenderTarget(RenderTargetType type, Consumer<RenderTarget> callback) {
        if (getRenderTarget(type) != null) {
            callback.accept(getRenderTarget(type));
        }
    }

    public static void callOnRenderTargets(Consumer<RenderTarget> callback, boolean includeMainRenderTarget) {
        callOnRenderTargets(callback);
        if (includeMainRenderTarget) callback.accept(getRenderTarget());
    }

    public static void onBlitEntityEffect() {
        if (getRenderTarget(RenderTargetType.ENTITY) == null) return;
        GlTexture.blitToScreen(
                getRenderWidth(),
                getRenderHeight(),
                getScreenWidth(),
                getScreenHeight(),
                getRenderTarget(RenderTargetType.ENTITY).getColorTextureId()
        );
    }

    public static void updateRenderTargetSize() {
        int renderWidth = getRenderWidth();
        int renderHeight = getRenderHeight();
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        callOnRenderTargets(
                (renderTarget) -> {
                    if (renderTarget.width != renderWidth || renderTarget.height != renderHeight) {
                        renderTarget.resize(
                                renderWidth,
                                renderHeight,
                                Minecraft.ON_OSX
                        );
                    }
                }, true
        );
        RenderTarget handRenderTarget = getRenderTarget(RenderTargetType.HAND);
        if (handRenderTarget.width != screenWidth || handRenderTarget.height != screenHeight) {
            handRenderTarget.resize(
                    screenWidth,
                    screenHeight,
                    Minecraft.ON_OSX
            );
        }
    }

    public static void onRenderHandBegin() {
        if (!checkRenderHandCallPos()) return;
        GlState.save("hand");
        setClientRenderTarget(getRenderTarget(RenderTargetType.HAND));
        callOnRenderTarget(
                RenderTargetType.HAND,
                (renderTarget -> {
                    Gl.glBindFramebuffer(
                            GlConst.GL_DRAW_FRAMEBUFFER,
                            renderTarget.frameBufferId
                    );
                    Gl.glViewport(
                            0, 0,
                            getScreenWidth(),
                            getScreenHeight()
                    );
                })
        );
    }

    public static void blitHandRenderTarget() {
        RenderSystem.enableBlend();
        callOnRenderTarget(RenderTargetType.HAND, (renderTarget -> GlTexture.blitToScreen(
                renderTarget.width,
                renderTarget.height,
                getScreenWidth(),
                getScreenHeight(),
                renderTarget.getColorTextureId()
        )));
        RenderSystem.disableBlend();
    }

    public static void onRenderHandEnd() {
        if (!checkRenderHandCallPos()) return;
        setClientRenderTarget(getRenderTarget(RenderTargetType.HAND));
        Gl.glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, GlState.get("hand").writeFBO());
        Gl.glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, GlState.get("hand").readFBO());
        Gl.glViewport(
                0, 0,
                getRenderWidth(),
                getRenderHeight()
        );
        GlState.pop("hand");
        if (Config.getCaptureMode() != CaptureMode.C) blitHandRenderTarget();
    }
}
