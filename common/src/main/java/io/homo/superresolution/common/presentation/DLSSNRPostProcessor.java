/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.common.presentation;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.AlgorithmDispatchEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.algo.dlss.DLSSNR;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.utils.GlTextureCopier;
import net.minecraft.client.Minecraft;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL46;

/**
 * Drives private {@link DLSSNR} instances as a post processor inside the Vulkan
 * presentation pipeline. Depth/motion vectors are snapshotted on {@link AlgorithmDispatchEvent};
 * at the hudless-color capture point (world done, HUD not yet drawn) the main target color is
 * copied, fed through DLSSNR, and the result is blitted back to the main target so that the
 * following {@code FrameCaptureManager.captureHudlessColor} picks up the DLSSNR output.
 */
public final class DLSSNRPostProcessor {
    private static final int MAX_PASS_COUNT = 4;
    private static final float[] PASS_INTENSITY_SCALES = {1.0f, 0.60f, 0.35f, 0.20f};
    private static final DLSSNR[] dlssnrPasses = new DLSSNR[MAX_PASS_COUNT];
    private static boolean broken;
    private static boolean registered;
    private static int activePassCount;
    private static int configuredPassCount = -1;
    private static int dlssnrSettingsSignature;
    private static boolean historyResetPending = true;

    private static ITexture colorTexture;
    private static ITexture hdrOutputTexture;
    private static IFrameBuffer hdrOutputFrameBuffer;
    private static ITexture depthTexture;
    private static ITexture motionVectorTexture;
    private static int cachedScreenWidth = -1;
    private static int cachedScreenHeight = -1;
    private static int cachedRenderWidth = -1;
    private static int cachedRenderHeight = -1;
    private static boolean inputsCaptured;

    private DLSSNRPostProcessor() {
    }

    public static synchronized void initialize() {
        if (!registered) {
            SuperResolutionAPI.EVENT_BUS.addListener(DLSSNRPostProcessor::onAlgorithmDispatch);
            registered = true;
        }
    }

    public static synchronized void shutdown() {
        destroyPassesFrom(0);
        destroyTextures();
        broken = false;
        activePassCount = 0;
        configuredPassCount = -1;
        dlssnrSettingsSignature = 0;
        historyResetPending = true;
        inputsCaptured = false;
    }

    public static void processHudlessColor() {
        boolean active = isActive();
        if (!active || !inputsCaptured) {
            if (!active) {
                historyResetPending = true;
            }
            inputsCaptured = false;
            return;
        }
        inputsCaptured = false;
        if (!ensureReady()) {
            return;
        }
        ITexture color = originColorTexture();
        if (color == null) {
            return;
        }
        if (color.getWidth() != RenderHandlerManager.getScreenWidth()
                || color.getHeight() != RenderHandlerManager.getScreenHeight()) {
            SuperResolution.LOGGER.warn("Skipping DLSSNR frame: color dimensions do not match screen size");
            return;
        }
        boolean hdrInput = color.getTextureFormat().getDataType() == TextureFormat.DataType.FLOAT;
        // nvngx_dlssnr.dll exposes no HDR-input contract. Use reversible AgX Log
        // around the network, leaving the game's AgX display look to its own pass.
        if (hdrInput) {
            DLSSNRHdrColorTransform.compress(color, colorTexture);
        } else {
            GlTextureCopier.copy(CopyOperation.create()
                    .src(color).dst(colorTexture)
                    .fromTo(CopyOperation.TextureChannel.R, CopyOperation.TextureChannel.R)
                    .fromTo(CopyOperation.TextureChannel.G, CopyOperation.TextureChannel.G)
                    .fromTo(CopyOperation.TextureChannel.B, CopyOperation.TextureChannel.B));
        }
        ITexture passInput = colorTexture;
        IFrameBuffer outFbo = null;
        ITexture output = null;
        for (int pass = 0; pass < activePassCount; pass++) {
            DispatchResource dispatchResource = AlgorithmManager.getDispatchResource(
                    passInput,
                    depthTexture,
                    motionVectorTexture,
                    new Vector2f(0),
                    0
            );
            DLSSNR stage = dlssnrPasses[pass];
            if (stage == null || !stage.dispatch(dispatchResource)) {
                SuperResolution.LOGGER.warn("Skipping DLSSNR frame: pass {} is unavailable", pass + 1);
                return;
            }
            outFbo = stage.getOutputFrameBuffer();
            output = outFbo == null ? null : outFbo.getTexture(FrameBufferAttachmentType.Color);
            if (output == null) {
                SuperResolution.LOGGER.warn("Skipping DLSSNR frame: output color texture is unavailable after pass {}", pass + 1);
                return;
            }
            // The serial interop path has completed and exposed this pass's GL output here.
            // Feeding that texture back makes pass N+1 process the actual result of pass N.
            passInput = output;
        }
        DLSSNRHdrColorTransform.finish(
                color,
                output,
                hdrOutputTexture,
                hdrInput,
                SuperResolutionConfig.SPECIAL.DLSSNR.COLOR_STRENGTH.get()
        );
        Gl.DSA.blitFramebuffer(
                (int) hdrOutputFrameBuffer.handle(),
                (int) RenderHandlerManager.getOriginRenderTarget().handle(),
                0, 0, outFbo.getWidth(), outFbo.getHeight(),
                0, 0, RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight(),
                GL46.GL_COLOR_BUFFER_BIT,
                GL46.GL_NEAREST
        );
    }

    private static void onAlgorithmDispatch(AlgorithmDispatchEvent event) {
        boolean active = isActive();
        if (!active || event == null) {
            if (!active) {
                historyResetPending = true;
            }
            inputsCaptured = false;
            return;
        }
        DispatchResource dispatch = event.getDispatchResource();
        InputResourceSet resources = dispatch == null ? null : dispatch.resources();
        if (resources == null
                || !resources.has(InputResourceType.Depth)
                || !resources.has(InputResourceType.MotionVectors)) {
            inputsCaptured = false;
            return;
        }
        ITexture depth = resources.get(InputResourceType.Depth);
        ITexture motionVectors = resources.get(InputResourceType.MotionVectors);
        if (depth.getWidth() != RenderHandlerManager.getRenderWidth()
                || depth.getHeight() != RenderHandlerManager.getRenderHeight()
                || motionVectors.getWidth() != RenderHandlerManager.getRenderWidth()
                || motionVectors.getHeight() != RenderHandlerManager.getRenderHeight()) {
            SuperResolution.LOGGER.warn("Skipping DLSSNR frame: depth or motion-vector dimensions do not match render size");
            inputsCaptured = false;
            return;
        }
        if (!ensureReady()) {
            inputsCaptured = false;
            return;
        }
        GlTextureCopier.copy(
                CopyOperation.create()
                        .src(depth)
                        .dst(depthTexture)
                        .fromTo(CopyOperation.TextureChannel.R, CopyOperation.TextureChannel.R)
        );
        GlTextureCopier.copy(
                CopyOperation.create()
                        .src(motionVectors)
                        .dst(motionVectorTexture)
                        .fromTo(CopyOperation.TextureChannel.R, CopyOperation.TextureChannel.R)
                        .fromTo(CopyOperation.TextureChannel.G, CopyOperation.TextureChannel.G)
        );
        inputsCaptured = true;
    }

    private static boolean isActive() {
        return !broken
                && SuperResolutionConfig.SPECIAL.DLSSNR.ENABLE.get()
                && isWorldFrame();
    }

    private static boolean isWorldFrame() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null
                && SuperResolution.gameIsLoaded
                && minecraft.level != null;
    }

    private static boolean ensureReady() {
        int screenWidth = RenderHandlerManager.getScreenWidth();
        int screenHeight = RenderHandlerManager.getScreenHeight();
        int renderWidth = RenderHandlerManager.getRenderWidth();
        int renderHeight = RenderHandlerManager.getRenderHeight();
        boolean sizeChanged = screenWidth != cachedScreenWidth
                || screenHeight != cachedScreenHeight
                || renderWidth != cachedRenderWidth
                || renderHeight != cachedRenderHeight;
        int requestedPassCount = configuredPassCount();
        boolean passCountChanged = requestedPassCount != configuredPassCount;
        if (sizeChanged) {
            try {
                createTextures(screenWidth, screenHeight, renderWidth, renderHeight);
            } catch (Throwable t) {
                SuperResolution.LOGGER.error("Failed to resize DLSSNR post-process textures", t);
                broken = true;
                return false;
            }
        }

        if (requestedPassCount < activePassCount) {
            destroyPassesFrom(requestedPassCount);
            activePassCount = requestedPassCount;
        }

        for (int pass = 0; pass < requestedPassCount; pass++) {
            try {
                if (dlssnrPasses[pass] == null) {
                    // A failed optional stage is retried after a size or pass-count change,
                    // not every frame (feature creation is expensive and may wait for the GPU).
                    if (pass > 0 && !sizeChanged && !passCountChanged) {
                        break;
                    }
                    dlssnrPasses[pass] = createPass(pass);
                } else if (sizeChanged) {
                    dlssnrPasses[pass].resize(screenWidth, screenHeight);
                }
                activePassCount = pass + 1;
            } catch (Throwable t) {
                if (pass == 0) {
                    SuperResolution.LOGGER.error("Failed to initialize primary DLSSNR post-process pass", t);
                    broken = true;
                    return false;
                }
                destroyPassesFrom(pass);
                activePassCount = pass;
                SuperResolution.LOGGER.warn(
                        "DLSSNR pass {} initialization failed; falling back to {} pass(es)",
                        pass + 1,
                        activePassCount,
                        t
                );
                break;
            }
        }

        int settingsSignature = settingsSignature();
        if (historyResetPending || sizeChanged || passCountChanged
                || settingsSignature != dlssnrSettingsSignature) {
            invalidateAllPassHistories();
            dlssnrSettingsSignature = settingsSignature;
            historyResetPending = false;
        }
        configuredPassCount = requestedPassCount;
        return activePassCount > 0;
    }

    private static DLSSNR createPass(int passIndex) throws Throwable {
        DLSSNR instance = new DLSSNR() {
            @Override
            protected int getInputColorWidth() {
                return RenderHandlerManager.getScreenWidth();
            }

            @Override
            protected int getInputColorHeight() {
                return RenderHandlerManager.getScreenHeight();
            }

            @Override
            protected boolean useSerialSyncMode() {
                return true;
            }

            @Override
            protected float getIntensityScale() {
                return PASS_INTENSITY_SCALES[passIndex];
            }
        };
        try {
            instance.initialize(InitializationDescription.defaults());
            return instance;
        } catch (Throwable t) {
            try {
                instance.destroy();
            } catch (Throwable cleanupFailure) {
                t.addSuppressed(cleanupFailure);
            }
            throw t;
        }
    }

    private static int configuredPassCount() {
        return Math.max(1, Math.min(MAX_PASS_COUNT,
                Math.round(SuperResolutionConfig.SPECIAL.DLSSNR.PASS_COUNT.get())));
    }

    private static int settingsSignature() {
        int result = Float.floatToIntBits(SuperResolutionConfig.SPECIAL.DLSSNR.INTENSITY.get());
        result = 31 * result + Float.floatToIntBits(SuperResolutionConfig.SPECIAL.DLSSNR.LOCAL_TONE_STRENGTH.get());
        result = 31 * result + Float.floatToIntBits(SuperResolutionConfig.SPECIAL.DLSSNR.LOCAL_STRUCTURE_STRENGTH.get());
        result = 31 * result + Float.floatToIntBits(SuperResolutionConfig.SPECIAL.DLSSNR.SKIN_STRUCTURE_STRENGTH.get());
        result = 31 * result + Float.floatToIntBits(SuperResolutionConfig.SPECIAL.DLSSNR.STYLE.get());
        result = 31 * result + Boolean.hashCode(SuperResolutionConfig.SPECIAL.DLSSNR.USE_AUTO_MASK.get());
        result = 31 * result + Boolean.hashCode(SuperResolutionConfig.SPECIAL.DLSSNR.UI_CORRECTION.get());
        result = 31 * result + Boolean.hashCode(SuperResolutionConfig.SPECIAL.DLSSNR.DEPTH_INVERTED.get());
        return result;
    }

    private static void invalidateAllPassHistories() {
        for (int pass = 0; pass < activePassCount; pass++) {
            if (dlssnrPasses[pass] != null) {
                dlssnrPasses[pass].invalidateHistory();
            }
        }
    }

    private static void destroyPassesFrom(int firstPass) {
        for (int pass = Math.max(0, firstPass); pass < MAX_PASS_COUNT; pass++) {
            if (dlssnrPasses[pass] != null) {
                dlssnrPasses[pass].destroy();
                dlssnrPasses[pass] = null;
            }
        }
    }

    private static void createTextures(int screenWidth, int screenHeight, int renderWidth, int renderHeight) {
        destroyTextures();
        colorTexture = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .label("SRDLSSNRPostColorTexture")
                        .format(TextureFormat.RGBA16F)
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().storage().sampler())
                        .mipmapsDisabled()
                        .size(screenWidth, screenHeight)
                        .build()
        );
        depthTexture = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .label("SRDLSSNRPostDepthTexture")
                        .format(TextureFormat.R32F)
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().storage().sampler())
                        .mipmapsDisabled()
                        .size(renderWidth, renderHeight)
                        .build()
        );
        motionVectorTexture = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .label("SRDLSSNRPostMotionVectorTexture")
                        .format(TextureFormat.RG16F)
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().storage().sampler())
                        .mipmapsDisabled()
                        .size(renderWidth, renderHeight)
                        .build()
        );
        hdrOutputTexture = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .label("SRDLSSNRPostHdrOutputTexture")
                        .format(TextureFormat.RGBA16F)
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().storage().sampler().attachmentColor())
                        .mipmapsDisabled()
                        .size(screenWidth, screenHeight)
                        .build()
        );
        hdrOutputFrameBuffer = RenderSystems.current().device().createFramebuffer(
                io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription.create()
                        .colorAttachment(hdrOutputTexture)
                        .build()
        );
        cachedScreenWidth = screenWidth;
        cachedScreenHeight = screenHeight;
        cachedRenderWidth = renderWidth;
        cachedRenderHeight = renderHeight;
    }

    private static void destroyTextures() {
        if (colorTexture != null) {
            colorTexture.destroy();
            colorTexture = null;
        }
        if (hdrOutputFrameBuffer != null) {
            hdrOutputFrameBuffer.destroy();
            hdrOutputFrameBuffer = null;
        }
        if (hdrOutputTexture != null) {
            hdrOutputTexture.destroy();
            hdrOutputTexture = null;
        }
        if (depthTexture != null) {
            depthTexture.destroy();
            depthTexture = null;
        }
        if (motionVectorTexture != null) {
            motionVectorTexture.destroy();
            motionVectorTexture = null;
        }
        cachedScreenWidth = -1;
        cachedScreenHeight = -1;
        cachedRenderWidth = -1;
        cachedRenderHeight = -1;
    }

    private static ITexture originColorTexture() {
        IFrameBuffer framebuffer = SuperResolutionAPI.getOriginMinecraftFrameBuffer();
        return framebuffer == null ? null : framebuffer.getTexture(FrameBufferAttachmentType.Color);
    }
}
