package io.homo.superresolution.common.minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sun.jna.Pointer;
import io.homo.superresolution.api.event.*;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.mixin.core.accessor.MinecraftAccessor;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.GlStates;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL46;
#if MC_VER < MC_1_21_4
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
#endif
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL43.glCopyImageSubData;

public class MinecraftRenderHandle {
    private static final Map<MinecraftRenderTargetType, IBindableFrameBuffer> renderTargets = new HashMap<>();
    private static final Map<IBindableFrameBuffer, RenderTarget> renderTargetMap = new HashMap<>();
    public static boolean isRenderingWorld = false;
    public static float frameTime;
    private static int frameCount = 0;
    private static Minecraft minecraft;
    private static IBindableFrameBuffer originRenderTarget;
    private static IBindableFrameBuffer renderTarget;
    private static boolean needCapture = false;
    private static boolean needCaptureVulkan = false;
    private static boolean needCaptureUpscale = false;

    private static int[] timeQueryIds = new int[2]; // 0: world begin, 1: world end, 2: upscale begin, 3: upscale end
    private static long startTime, endTime;
    private static boolean queriesInitialized = false;

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

    public static IBindableFrameBuffer getOriginRenderTarget() {
        if (originRenderTarget == null) MinecraftRenderHandle.init();
        return originRenderTarget;
    }

    public static IBindableFrameBuffer getRenderTarget() {
        if (renderTarget == null) MinecraftRenderHandle.init();
        return renderTarget;
    }

    public static void init() {
        RenderSystem.assertOnRenderThread();
        minecraft = Minecraft.getInstance();
        originRenderTarget = MinecraftRenderTargetWrapper.of(minecraft.getMainRenderTarget());
        #if MC_VER > MC_1_21_5
        renderTarget = new io.homo.superresolution.common.minecraft.MinecraftRenderTargetWrapper(
                new TextureTarget(
                        "SuperrResolution-ScaledRenderTarget",
                        getRenderWidth(),
                        getRenderHeight(),
                        true
                )
        );
        #elif MC_VER > MC_1_21_4
        renderTarget = io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer.create(
                io.homo.superresolution.core.graphics.impl.texture.TextureFormat.RGBA8,
                TextureFormat.DEPTH24_STENCIL8,
                getRenderWidth(),
                getRenderHeight()
        );
        #else
        renderTarget = new LegacyStorageFrameBuffer(true);
        #endif
        renderTarget.setClearColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
        renderTarget.resizeFrameBuffer(
                getRenderWidth(),
                getRenderHeight()
        );

        if (!queriesInitialized) {
            GL41.glGenQueries(timeQueryIds);
            queriesInitialized = true;
        }
    }

    public static void fixPostChain(PostChain postChain) {
        #if MC_VER < MC_1_21_4
        postChain.getName();
        int renderWidth = getRenderWidth();
        int renderHeight = getRenderHeight();
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

    public static void updateRenderTarget() {
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

    public static IBindableFrameBuffer getRenderTarget(MinecraftRenderTargetType type) {
        return renderTargets.get(type);
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

    public static void onRenderWorldBegin(CallType type) {
        if (!checkRenderWorldCallPos(type)) return;
        isRenderingWorld = true;
        //SuperResolution.LOGGER.info("renderBegin");
        if (SuperResolution.cachedWidth != getScreenWidth() || SuperResolution.cachedHeight != getScreenHeight()) {
            SuperResolution.getInstance().resize(getScreenWidth(), getScreenHeight());
        }


        if (SuperResolutionConfig.isEnableDetailedProfiling()) {
            PerformanceInfo.begin("world");
            GL41.glBeginQuery(GL41.GL_TIME_ELAPSED, timeQueryIds[0]);
        }

        updateRenderTarget();
        updateRenderTargetSize();
        #if MC_VER == MC_1_21_5
        getOriginRenderTarget().asMcRenderTarget().resize(getRenderWidth(), getRenderHeight());
        #elif MC_VER > MC_1_21_5
        setClientRenderTarget(getRenderTarget().asMcRenderTarget());
        getRenderTarget().bind(FrameBufferBindPoint.Write);
        #else
        setClientRenderTarget(getRenderTarget().asMcRenderTarget());
        getRenderTarget().bind(FrameBufferBindPoint.Write);
        #endif
        SuperResolution.getCurrentAlgorithm().setInputFrameBuffer(getRenderTarget());

        if (needCapture) {
            if (RenderDoc.renderdoc != null) {
                RenderDoc.renderdoc.StartFrameCapture.call(null, null);
            }
        }
        if (needCaptureVulkan) {
            if (RenderDoc.renderdoc != null) {
                if (RenderSystems.vulkan() != null) {
                    RenderDoc.renderdoc.StartFrameCapture.call(
                            new Pointer(RenderSystems.vulkan().getVulkanInstance().address()),
                            null
                    );
                }
            }
        }
        try (GlState ignored = new GlState()) {
            LevelRenderStartEvent.EVENT.invoker().onLevelRenderStart();
        }
    }

    public static void onRenderWorldEnd(CallType type) {
        if (!checkRenderWorldCallPos(type)) return;
        isRenderingWorld = false;
        #if MC_VER == MC_1_21_5
        ((io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D) getRenderTarget().getTexture(FrameBufferAttachmentType.Color)).copyFromTex(
                ((com.mojang.blaze3d.opengl.GlTexture) java.util.Objects.requireNonNull(getOriginRenderTarget().asMcRenderTarget().getColorTexture())).glId()
        );
        getOriginRenderTarget().asMcRenderTarget().resize(getScreenWidth(), getScreenHeight());
        #else
        setClientRenderTarget(getOriginRenderTarget().asMcRenderTarget());
        #endif
        getOriginRenderTarget().bind(FrameBufferBindPoint.Write, true);

        try (GlState ignored = new GlState()) {
            LevelRenderEndEvent.EVENT.invoker().onLevelRenderEnd();
        }
        if (SuperResolutionConfig.isEnableDetailedProfiling()) {
            GL41.glEndQuery(GL41.GL_TIME_ELAPSED);
            long[] worldTime = {0};
            GL41.glGetQueryObjectui64v(timeQueryIds[0], GL41.GL_QUERY_RESULT, worldTime);
            PerformanceInfo.end("world", worldTime[0]);
        }

        try (GlState ignored = new GlState()) {
            if (SuperResolutionConfig.isEnableDetailedProfiling()) {
                PerformanceInfo.begin("upscale");
                GL41.glBeginQuery(GL41.GL_TIME_ELAPSED, timeQueryIds[1]);
            }
            if (needCaptureUpscale) {
                if (RenderDoc.renderdoc != null) {
                    RenderDoc.renderdoc.StartFrameCapture.call(null, null);
                }
            }

            AlgorithmManager.update();

            try (GlState ignored_ = new GlState()) {
                DispatchResource dispatchResource = AlgorithmManager.getDispatchResource();
                if (SuperResolution.currentAlgorithm != null) {
                    AlgorithmDispatchEvent.EVENT.invoker().onAlgorithmDispatch(
                            SuperResolution.currentAlgorithm,
                            dispatchResource
                    );
                }
                SuperResolution.getCurrentAlgorithm().dispatch(dispatchResource);
                if (SuperResolution.currentAlgorithm != null) {
                    AlgorithmDispatchFinishEvent.EVENT.invoker().onAlgorithmDispatchFinish(
                            SuperResolution.currentAlgorithm,
                            SuperResolution.currentAlgorithm.getOutputFrameBuffer().getTexture(FrameBufferAttachmentType.Color)
                    );
                }

            }
            IFrameBuffer outFbo = SuperResolution.getCurrentAlgorithm().getOutputFrameBuffer();
            Gl.DSA.blitFramebuffer(
                    (int) outFbo.handle(),
                    (int) MinecraftRenderHandle.getOriginRenderTarget().handle(),
                    0, 0, outFbo.getWidth(), outFbo.getHeight(),
                    0, 0, MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight(),
                    GL46.GL_COLOR_BUFFER_BIT,
                    GL46.GL_NEAREST
            );

            if (needCaptureUpscale) {
                if (RenderDoc.renderdoc != null) {
                    needCaptureUpscale = false;
                    RenderDoc.renderdoc.EndFrameCapture.call(null, null);
                }
            }
            if (SuperResolutionConfig.isEnableDetailedProfiling()) {
                GL41.glEndQuery(GL41.GL_TIME_ELAPSED);
                long[] upscaleTime = {0};
                GL41.glGetQueryObjectui64v(timeQueryIds[1], GL41.GL_QUERY_RESULT, upscaleTime);
                PerformanceInfo.end("upscale", upscaleTime[0]);
            }
            if (SuperResolutionConfig.getCaptureMode() == CaptureMode.C && !Platform.currentPlatform.iris().isShaderPackInUse())
                blitHandRenderTarget();
        }

        frameTime = PerformanceInfo.getAsMillis("world");
        getOriginRenderTarget().bind(FrameBufferBindPoint.Write);
        glViewport(
                0,
                0,
                getScreenWidth(),
                getScreenHeight()
        );
        if (needCapture) {
            if (RenderDoc.renderdoc != null) {
                needCapture = false;
                RenderDoc.renderdoc.EndFrameCapture.call(null, null);
            }
        }
        if (needCaptureVulkan) {
            if (RenderDoc.renderdoc != null) {
                if (RenderSystems.vulkan() != null) {
                    needCaptureVulkan = false;
                    RenderDoc.renderdoc.EndFrameCapture.call(
                            Pointer.createConstant(RenderSystems.vulkan().getVulkanInstance().address()),
                            null
                    );
                }
            }
        }
        //SuperResolution.LOGGER.info("renderEnd");

        frameCount++;
    }

    public static void setClientRenderTarget(RenderTarget renderTarget) {
        if (renderTarget == null) {
            throw new RuntimeException();
        }
        ((MinecraftAccessor) Minecraft.getInstance()).setRenderTarget(renderTarget);
    }

    public static float getCurrentScaleFactor() {
        return isRenderingWorld && minecraft.level != null ? getScaleFactor() : 1;
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
        getRenderTarget(MinecraftRenderTargetType.HAND).clearFrameBuffer();
        getRenderTarget(MinecraftRenderTargetType.HAND).bind(FrameBufferBindPoint.All);
    }

    public static void blitHandRenderTarget() {
        glEnable(GL_BLEND);
        callOnRenderTarget(MinecraftRenderTargetType.HAND, (renderTarget -> GlTexture2D.blitToScreen(
                getScreenWidth(),
                getScreenHeight(),
                getScreenWidth(),
                getScreenHeight(),
                renderTarget.getTexture(FrameBufferAttachmentType.Color)
        )));
        glDisable(GL_BLEND);
    }

    public static void onRenderHandEnd() {
        if (!checkRenderHandCallPos()) return;
        setClientRenderTarget(getRenderTarget().asMcRenderTarget());
        GlStates.pop("hand").restore();
    }

}