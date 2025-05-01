package io.homo.superresolution.common.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.api.event.AlgorithmDispatchEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.mixin.core.accessor.MinecraftAccessor;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.render.gl.GlState;
import io.homo.superresolution.common.render.gl.GlStates;
import io.homo.superresolution.common.render.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.framebuffer.LegacyStorageFrameBuffer;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.common.render.renderdoc.RenderDoc;
import io.homo.superresolution.common.render.utils.CallType;
import io.homo.superresolution.common.render.utils.MinecraftRenderTargetWrapper;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.common.render.utils.MinecraftRenderTargetType;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
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

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class MinecraftRenderHandle {
    private static final Map<MinecraftRenderTargetType, IFrameBuffer> renderTargets = new HashMap<>();
    private static final Map<IFrameBuffer, RenderTarget> renderTargetMap = new HashMap<>();
    public static boolean isRenderingWorld = false;
    public static float frameTime;
    private static int frameCount = 0;
    private static Minecraft minecraft;
    private static IFrameBuffer originRenderTarget;
    private static IFrameBuffer renderTarget;
    private static PostChain entityEffect;
    private static IFrameBuffer entityTarget;
    private static boolean needCapture = false;
    private static boolean needCaptureUpscale = false;

    public static void needCapture() {
        needCapture = true;
    }

    public static void needCaptureUpscale() {
        needCaptureUpscale = true;
    }

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
        #if MC_VER > MC_1_21_4
        renderTarget = GlFrameBuffer.create(
                TextureFormat.RGBA8,
                TextureFormat.DEPTH24_STENCIL8,
                getRenderWidth(),
                getRenderHeight()
        );
        #else
        renderTarget = new LegacyStorageFrameBuffer(true);
        #endif
        renderTarget.setClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        renderTarget.resizeFrameBuffer(
                getRenderWidth(),
                getRenderHeight()
        );
    }

    //bugjump在1.21.1后的版本重写了一堆代码，成功使发光效果不用我强行兼容力，感谢bugjump
    public static void updateEntityOutline() {
        entityTarget = MinecraftRenderTargetType.ENTITY.get(Minecraft.getInstance().levelRenderer);
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
        for (MinecraftRenderTargetType minecraftRenderTargetType : MinecraftRenderTargetType.values()) {
            IFrameBuffer renderTarget = minecraftRenderTargetType.get(Minecraft.getInstance().levelRenderer);
            if (renderTarget != null) {
                renderTargets.put(
                        minecraftRenderTargetType,
                        renderTarget
                );
            }
        }
    }

    public static IFrameBuffer getRenderTarget(MinecraftRenderTargetType type) {
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
        #if MC_VER > MC_1_21_4
        getOriginRenderTarget().asMcRenderTarget().resize(getRenderWidth(), getRenderHeight());
        SuperResolution.getCurrentAlgorithm().setInputFrameBuffer(getRenderTarget());
        #else
        setClientRenderTarget(getRenderTarget().asMcRenderTarget());
        getRenderTarget().bind(FrameBufferBindPoint.WRITE);
        SuperResolution.getCurrentAlgorithm().setInputFrameBuffer(getRenderTarget());
        #endif
        if (needCapture) {
            if (RenderDoc.renderdoc != null) {
                RenderDoc.renderdoc.StartFrameCapture.call(null, null);
            }
        }
    }

    public static void onRenderWorldEnd(CallType type) {
        if (!checkRenderWorldCallPos(type)) return;
        isRenderingWorld = false;
        frameCount++;

        #if MC_VER > MC_1_21_4
        ((GlTexture) getRenderTarget().getTexture(FrameBufferAttachmentType.COLOR)).copyFromTex(
                ((com.mojang.blaze3d.opengl.GlTexture) java.util.Objects.requireNonNull(getOriginRenderTarget().asMcRenderTarget().getColorTexture())).glId()
        );
        getOriginRenderTarget().asMcRenderTarget().resize(getScreenWidth(), getScreenHeight());
        #else
        setClientRenderTarget(getOriginRenderTarget().asMcRenderTarget());
        #endif
        getOriginRenderTarget().bind(FrameBufferBindPoint.WRITE);

        try (GlState ignored = new GlState()) {
            PerformanceInfo.begin("upscale");
            if (needCaptureUpscale) {
                if (RenderDoc.renderdoc != null) {
                    RenderDoc.renderdoc.StartFrameCapture.call(null, null);
                }
            }
            AlgorithmManager.update();
            try (GlState ignored_ = new GlState()) {
                DispatchResource dispatchResource = AlgorithmManager.getDispatchResource();
                if (SuperResolution.currentAlgorithm != null) {
                    AlgorithmDispatchEvent.EVENT.invoker().onAlgorithmRegister(
                            SuperResolution.currentAlgorithm,
                            dispatchResource
                    );
                }
                SuperResolution.getCurrentAlgorithm().dispatch(dispatchResource);
            }
            SuperResolution.getCurrentAlgorithm().blitToScreen(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
            );

            if (needCaptureUpscale) {
                if (RenderDoc.renderdoc != null) {
                    needCaptureUpscale = false;
                    RenderDoc.renderdoc.EndFrameCapture.call(null, null);
                }
            }
            PerformanceInfo.end("upscale");
            if (Config.getCaptureMode() == CaptureMode.C && !Platform.currentPlatform.iris().isShaderPackInUse())
                blitHandRenderTarget();
        }
        PerformanceInfo.end("world");
        frameTime = PerformanceInfo.getAsMillis("world");
        if (needCapture) {
            if (RenderDoc.renderdoc != null) {
                needCapture = false;
                RenderDoc.renderdoc.EndFrameCapture.call(null, null);
            }
        }
    }

    public static void setClientRenderTarget(RenderTarget renderTarget) {
        if (renderTarget == null) {
            throw new RuntimeException();
        }
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
        renderTargets.forEach(((minecraftRenderTargetType, renderTarget) -> {
            if (renderTarget != null && minecraftRenderTargetType != MinecraftRenderTargetType.HAND) {
                callback.accept(renderTarget);
            }
        }));
    }

    public static void callOnRenderTargets(BiConsumer<IFrameBuffer, MinecraftRenderTargetType> callback) {
        renderTargets.forEach(((minecraftRenderTargetType, renderTarget) -> {
            if (renderTarget != null) {
                callback.accept(renderTarget, minecraftRenderTargetType);
            }
        }));
    }

    public static void callOnRenderTarget(MinecraftRenderTargetType type, Consumer<IFrameBuffer> callback) {
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
        if (getRenderTarget(MinecraftRenderTargetType.ENTITY) == null) return;
        GlTexture.blitToScreen(
                getRenderWidth(),
                getRenderHeight(),
                getScreenWidth(),
                getScreenHeight(),
                getRenderTarget(MinecraftRenderTargetType.ENTITY).getTextureId(FrameBufferAttachmentType.COLOR)
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
        IFrameBuffer handRenderTarget = getRenderTarget(MinecraftRenderTargetType.HAND);
        if (handRenderTarget.getWidth() != screenWidth || handRenderTarget.getHeight() != screenHeight) {
            handRenderTarget.resizeFrameBuffer(
                    screenWidth,
                    screenHeight
            );
        }
    }

    public static void onRenderHandBegin() {
        if (!checkRenderHandCallPos()) return;
        GlStates.save("hand");
        setClientRenderTarget(getRenderTarget(MinecraftRenderTargetType.HAND).asMcRenderTarget());
        callOnRenderTarget(
                MinecraftRenderTargetType.HAND,
                (renderTarget -> {
                    renderTarget.clearFrameBuffer();
                    renderTarget.bind(FrameBufferBindPoint.ALL);
                })
        );
    }

    public static void blitHandRenderTarget() {
        glEnable(GL_BLEND);
        callOnRenderTarget(MinecraftRenderTargetType.HAND, (renderTarget -> GlTexture.blitToScreen(
                getScreenWidth(),
                getScreenHeight(),
                getScreenWidth(),
                getScreenHeight(),
                renderTarget.getTextureId(FrameBufferAttachmentType.COLOR)
        )));
        glDisable(GL_BLEND);
    }

    public static void onRenderHandEnd() {
        if (!checkRenderHandCallPos()) return;
        setClientRenderTarget(getRenderTarget().asMcRenderTarget());
        GlStates.pop("hand").restore();
    }
}