package io.homo.superresolution.common.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.mixin.core.accessor.MinecraftAccessor;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.render.gl.Gl;
import io.homo.superresolution.common.render.gl.GlConst;
import io.homo.superresolution.common.render.gl.GlState;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.framebuffer.MinecraftRenderTarget;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
#if MC_VER < MC_1_21_4
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
import io.homo.superresolution.common.mixin.core.accessor.LevelRendererAccessor;
#endif
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MinecraftRenderHandle {
    private static final Map<RenderTargetType, IFrameBuffer> renderTargets = new HashMap<>();
    public static boolean isRenderingWorld = false;
    public static float frameTime;
    private static int frameCount = 0;
    private static Minecraft minecraft;
    private static IFrameBuffer originRenderTarget;
    private static IFrameBuffer renderTarget;
    private static PostChain entityEffect;
    private static IFrameBuffer entityTarget;

    public static int getFrameCount() {
        return frameCount;
    }

    public static IFrameBuffer getOriginRenderTarget() {
        return originRenderTarget;
    }

    public static IFrameBuffer getRenderTarget() {
        return renderTarget;
    }

    public static void init() {
        RenderSystem.assertOnRenderThread();
        minecraft = Minecraft.getInstance();
        originRenderTarget = MinecraftRenderTargetWrapper.of(minecraft.getMainRenderTarget());
        renderTarget = new MinecraftRenderTarget(true);
        renderTarget.resizeFrameBuffer(
                getRenderWidth(),
                getRenderHeight()
        );
    }

    //bugjump在1.21.1后的版本重写了一堆代码，成功使发光效果不用我强行兼容力，感谢bugjump
    public static void updateEntityOutline() {
        entityTarget = RenderTargetType.ENTITY.get(Minecraft.getInstance().levelRenderer);
        #if MC_VER < MC_1_21_4
        entityEffect = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getEntityEffect();
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
        #endif
    }

    public static void updateRenderTarget() {
        renderTargets.clear();
        for (RenderTargetType renderTargetType : RenderTargetType.values()) {
            IFrameBuffer renderTarget = renderTargetType.get(Minecraft.getInstance().levelRenderer);
            if (renderTarget != null) {
                renderTargets.put(
                        renderTargetType,
                        renderTarget
                );
            }
        }
    }

    public static IFrameBuffer getRenderTarget(RenderTargetType type) {
        return renderTargets.get(type);
    }

    public static void onInitEntityEffectBegin() {
        setClientRenderTarget(getRenderTarget().asMcRenderTarget());
    }

    public static void onInitEntityEffectEnd() {
        setClientRenderTarget(getOriginRenderTarget().asMcRenderTarget());
    }

    public static void resize() {
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        int renderWidth = getRenderWidth();
        int renderHeight = getRenderHeight();
        callOnRenderTargets((renderTarget) -> resizeRenderTarget(renderTarget, renderWidth, renderHeight), false);
    }

    private static void resizeRenderTarget(IFrameBuffer renderTarget, int width, int height) {
        renderTarget.resizeFrameBuffer(width, height);
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
        if (SuperResolution.cachedWidth != getScreenWidth() || SuperResolution.cachedHeight != getScreenHeight()) {
            SuperResolution.getInstance().resize(getScreenWidth(), getScreenHeight());
        }
        PerformanceInfo.begin("world");
        updateEntityOutline();
        updateRenderTarget();
        updateRenderTargetSize();
        setClientRenderTarget(getRenderTarget().asMcRenderTarget());
        SuperResolution.getCurrentAlgorithm().setInputFrameBuffer(getRenderTarget());
        getRenderTarget().bind(RenderTargetBindPoint.WRITE);
    }

    public static void onRenderWorldEnd(CallType type) {
        if (!checkRenderWorldCallPos(type)) return;
        isRenderingWorld = false;
        frameCount++;
        setClientRenderTarget(getOriginRenderTarget().asMcRenderTarget());
        getOriginRenderTarget().bind(RenderTargetBindPoint.ALL);
        PerformanceInfo.begin("upscale");
        AlgorithmManager.update();
        SuperResolution.getCurrentAlgorithm().dispatch(AlgorithmManager.getDispatchResource());
        SuperResolution.getCurrentAlgorithm().blitToScreen(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        PerformanceInfo.end("upscale");
        if (Config.getCaptureMode() == CaptureMode.C && !Platform.currentPlatform.iris().isShaderPackInUse())
            blitHandRenderTarget();
        PerformanceInfo.end("world");
        frameTime = PerformanceInfo.getAsMillis("world");
    }

    public static void setClientRenderTarget(RenderTarget renderTarget) {
        if (renderTarget == null) return;
        ((MinecraftAccessor) Minecraft.getInstance()).setRenderTarget(renderTarget);
    }

    public static float getCurrentScaleFactor() {
        return isRenderingWorld ? getScaleFactor() : 1;
    }

    public static float getScaleFactor() {
        return Config.isEnableUpscale() ? Config.getRenderScaleFactor() : 1;
    }


    public static int getRenderHeight() {
        return (int) Math.max(getScreenHeight() * getScaleFactor(), 1);
    }

    public static int getRenderWidth() {
        return (int) Math.max(getScreenWidth() * getScaleFactor(), 1);
    }

    public static int getScreenHeight() {
        return Math.max(SuperResolution.getMinecraftHeight(), 1);
    }

    public static int getScreenWidth() {
        return Math.max(SuperResolution.getMinecraftWidth(), 1);
    }

    public static void callOnRenderTargets(Consumer<IFrameBuffer> callback) {
        renderTargets.forEach(((renderTargetType, renderTarget) -> {
            if (renderTarget != null && renderTargetType != RenderTargetType.HAND) {
                callback.accept(renderTarget);
            }
        }));
    }

    public static void callOnRenderTargets(BiConsumer<IFrameBuffer, RenderTargetType> callback) {
        renderTargets.forEach(((renderTargetType, renderTarget) -> {
            if (renderTarget != null) {
                callback.accept(renderTarget, renderTargetType);
            }
        }));
    }

    public static void callOnRenderTarget(RenderTargetType type, Consumer<IFrameBuffer> callback) {
        if (getRenderTarget(type) != null) {
            callback.accept(getRenderTarget(type));
        }
    }

    public static void callOnRenderTarget(Consumer<IFrameBuffer> callback) {
        if (getRenderTarget() != null) {
            callback.accept(getRenderTarget());
        }
    }

    public static void callOnRenderTargets(Consumer<IFrameBuffer> callback, boolean includeMainRenderTarget) {
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
                getRenderTarget(RenderTargetType.ENTITY).getTextureId(IFrameBuffer.FrameBufferAttachmentType.COLOR)
        );
    }

    public static void updateRenderTargetSize() {
        int renderWidth = getRenderWidth();
        int renderHeight = getRenderHeight();
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        callOnRenderTargets(
                (renderTarget) -> {
                    if (renderTarget.getWidth() != renderWidth || renderTarget.getHeight() != renderHeight) {
                        renderTarget.resizeFrameBuffer(
                                renderWidth,
                                renderHeight
                        );
                    }
                }, true
        );
        IFrameBuffer handRenderTarget = getRenderTarget(RenderTargetType.HAND);
        if (handRenderTarget.getWidth() != screenWidth || handRenderTarget.getHeight() != screenHeight) {
            handRenderTarget.resizeFrameBuffer(
                    screenWidth,
                    screenHeight
            );
        }
    }

    public static void onRenderHandBegin() {
        if (!checkRenderHandCallPos()) return;
        GlState.save("hand");
        setClientRenderTarget(getRenderTarget(RenderTargetType.HAND).asMcRenderTarget());

        callOnRenderTarget(
                RenderTargetType.HAND,
                (renderTarget -> {
                    renderTarget.clearFrameBuffer();
                    Gl.glBindFramebuffer(
                            GlConst.GL_DRAW_FRAMEBUFFER,
                            renderTarget.getFrameBufferId()
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
                getScreenWidth(),
                getScreenHeight(),
                getScreenWidth(),
                getScreenHeight(),
                renderTarget.getTextureId(IFrameBuffer.FrameBufferAttachmentType.COLOR)
        )));
        RenderSystem.disableBlend();
    }

    public static void onRenderHandEnd() {
        if (!checkRenderHandCallPos()) return;

        setClientRenderTarget(getRenderTarget().asMcRenderTarget());
        Gl.glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, GlState.get("hand").writeFBO());
        Gl.glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, GlState.get("hand").readFBO());
        Gl.glViewport(
                0, 0,
                getRenderWidth(),
                getRenderHeight()
        );
        GlState.pop("hand");

    }
}
