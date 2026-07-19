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

package io.homo.superresolution.common.framegeneration;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;
import io.homo.superresolution.common.framegeneration.constants.FGConstantsFeature;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.lowlatency.LowLatencyMode;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexMode;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsage;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import io.homo.superresolution.core.streamline.Streamline;
import io.homo.superresolution.core.streamline.StreamlineSession;
import io.homo.superresolution.core.streamline.StreamlineTypes;

import java.util.Arrays;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class FrameGeneration {
    private static final int VIEWPORT = 0;
    private static final int BUFFER_TYPE_DEPTH = 0;
    private static final int BUFFER_TYPE_MOTION_VECTORS = 1;
    private static final int BUFFER_TYPE_HUDLESS_COLOR = 2;
    private static final int FLAG_RETAIN_RESOURCES_WHEN_OFF = 1 << 3;
    private static final int QUEUE_MODE_BLOCK_NO_CLIENT_QUEUES = 1;
    private static boolean supportQueried;
    private static int maxGeneratedFrameCount;
    private static int minimumWidthOrHeight;

    public static synchronized boolean prepareFrame(
            FrameResources frameResources,
            int colorWidth,
            int colorHeight,
            int colorFormat,
            int backBufferCount,
            long commandBuffer
    ) {
        if (frameResources == null
                || commandBuffer == 0L
                //|| !frameResources.hasHudlessColor()
                || !frameResources.hasDepth()
                || !frameResources.hasMotionVector()
            //|| !ReflexFeature.isFrameGenerationReady()
        ) {
            disableFrameGeneration();
            return false;
        }
        FrameGenerationMode mode = displayedMode();
        StreamlineSession session = Streamline.session();
        StreamlineTypes.FrameToken token = Streamline.currentFrame();
        FGConstants fgConstants = FGConstantsFeature.getConstants(frameResources.logicalFrameIndex());
        //VulkanTexture hudlessColor = frameResources.hudlessColorVulkanTexture();
        VulkanTexture depth = frameResources.depthVulkanTexture();
        VulkanTexture motionVectors = frameResources.motionVectorVulkanTexture();
        if (!mode.isEnabled()
                || session == null
                || token == null
                || fgConstants == null
                //|| hudlessColor == null
                || depth == null
                || motionVectors == null
                //|| hudlessColor.getWidth() != colorWidth
                //|| hudlessColor.getHeight() != colorHeight
                || depth.getWidth() != motionVectors.getWidth()
                || depth.getHeight() != motionVectors.getHeight()
                || colorWidth < minimumWidthOrHeight
                || colorHeight < minimumWidthOrHeight) {
            disableFrameGeneration();
            return false;
        }

        int constantsResult = session.setConstants(
                toStreamlineConstants(fgConstants),
                token,
                new StreamlineTypes.Viewport(VIEWPORT)
        );
        if (constantsResult != 0) {
            disableFrameGeneration();
            return false;
        }

        StreamlineTypes.ResourceTag[] tags = {
                createTag(depth, BUFFER_TYPE_DEPTH),
                createTag(motionVectors, BUFFER_TYPE_MOTION_VECTORS),
                //createTag(hudlessColor, BUFFER_TYPE_HUDLESS_COLOR)
        };
        int tagResult = session.setTagForFrame(
                token,
                new StreamlineTypes.Viewport(VIEWPORT),
                tags,
                commandBuffer
        );
        if (tagResult != 0) {
            disableFrameGeneration();
            return false;
        }
        DlssGOptionsKey desired = new DlssGOptionsKey(
                mode.nativeMode(),
                Math.max(1, mode.generatedFrameCount()),
                backBufferCount,
                depth.getWidth(),
                depth.getHeight(),
                colorWidth,
                colorHeight,
                colorFormat,
                motionVectors.getTextureFormat().vk(),
                depth.getTextureFormat().vk(), 0//,
                //hudlessColor.getTextureFormat().vk()
        );
        if (applyOptions(desired)) {
            return true;
        }
        disableFrameGeneration();
        return false;
    }

    public static synchronized void disableFrameGeneration() {
        configureFrameGeneration(false);
    }

    public static synchronized boolean configureFrameGeneration(boolean enabled) {
        FrameGenerationMode mode = enabled ? displayedMode() : FrameGenerationMode.OFF;
        DlssGOptionsKey desired = enabled && mode.isEnabled()
                ? DlssGOptionsKey.basic(mode.nativeMode(), Math.max(1, mode.generatedFrameCount()))
                : DlssGOptionsKey.off();
        return applyOptions(desired);
    }

    private static int toVulkanImageUsage(VulkanTexture texture) {
        int usage = 0;
        for (TextureUsage textureUsage : texture.getTextureUsages().getUsages()) {
            switch (textureUsage) {
                case Sampler -> usage |= VK_IMAGE_USAGE_SAMPLED_BIT;
                case Storage -> usage |= VK_IMAGE_USAGE_STORAGE_BIT;
                case AttachmentColor -> usage |= VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
                case AttachmentDepth -> usage |= VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
                case TransferSource -> usage |= VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
                case TransferDestination -> usage |= VK_IMAGE_USAGE_TRANSFER_DST_BIT;
            }
        }
        return usage;
    }

    private static StreamlineTypes.Resource createResource(VulkanTexture texture) {
        StreamlineTypes.Resource resource = new StreamlineTypes.Resource();
        resource.type = StreamlineTypes.ResourceType.TEX_2D;
        resource.nativeHandle = texture.handle();
        resource.memory = texture.getImageMemory();
        resource.view = texture.getImageView();
        resource.state = texture.getCurrentLayout();
        resource.width = texture.getWidth();
        resource.height = texture.getHeight();
        resource.nativeFormat = texture.getTextureFormat().vk();
        resource.mipLevels = texture.getMipmapSettings().getLevels();
        resource.arrayLayers = 1;
        resource.usage = toVulkanImageUsage(texture);
        return resource;
    }

    private static StreamlineTypes.ResourceTag createTag(VulkanTexture texture, int bufferType) {
        StreamlineTypes.ResourceTag tag = new StreamlineTypes.ResourceTag();
        tag.resource = createResource(texture);
        tag.type = bufferType;
        tag.lifecycle = StreamlineTypes.ResourceLifecycle.VALID_UNTIL_PRESENT;
        tag.extent = new StreamlineTypes.Extent();
        tag.extent.width = texture.getWidth();
        tag.extent.height = texture.getHeight();
        return tag;
    }

    private static boolean applyOptions(DlssGOptionsKey desired) {
        StreamlineSession session = Streamline.session();
        if (session == null) {
            return desired.mode() == StreamlineTypes.DlssGMode.OFF;
        }
        StreamlineTypes.DlssGOptions options = new StreamlineTypes.DlssGOptions();
        options.mode = desired.mode();
        options.numFramesToGenerate = desired.numFramesToGenerate();
        options.numBackBuffers = desired.numBackBuffers();
        options.motionVectorDepthWidth = desired.motionVectorDepthWidth();
        options.motionVectorDepthHeight = desired.motionVectorDepthHeight();
        options.colorWidth = desired.colorWidth();
        options.colorHeight = desired.colorHeight();
        options.colorBufferFormat = desired.colorBufferFormat();
        options.motionVectorBufferFormat = desired.motionVectorBufferFormat();
        options.depthBufferFormat = desired.depthBufferFormat();
        options.hudLessBufferFormat = desired.hudlessBufferFormat();
        options.flags = FLAG_RETAIN_RESOURCES_WHEN_OFF;
        options.queueParallelismMode = QUEUE_MODE_BLOCK_NO_CLIENT_QUEUES;
        options.enableUserInterfaceRecomposition = 0;
        //options.onApiError = API_ERROR_LISTENER;
        int result = session.dlssGSetOptions(new StreamlineTypes.Viewport(VIEWPORT), options);
        if (result != 0) {
            return false;
        }
        return true;
    }

    public static synchronized void finishPresent(
            FrameResources frameResources,
            boolean frameGenerationEnabled
    ) {
        if (!frameGenerationEnabled) {
            return;
        }
        StreamlineSession session = Streamline.session();
        StreamlineTypes.DlssGState state = new StreamlineTypes.DlssGState();
        int result = session.dlssGGetState(
                new StreamlineTypes.Viewport(0),
                state,
                null
        );
        if (result != 0) {
            return;
        }
        if (state.inputsProcessingCompletionFence != 0L
                && state.lastPresentInputsProcessingCompletionFenceValue > 0L) {
            frameResources.setDlssGInputCompletion(
                    state.inputsProcessingCompletionFence,
                    state.lastPresentInputsProcessingCompletionFenceValue
            );
        } else {
            frameResources.clearDlssGInputCompletion();
        }
    }

    private static boolean isModeSupported(FrameGenerationMode mode) {
        return mode == FrameGenerationMode.OFF
                || (isSupported() && mode.generatedFrameCount() <= supportedGeneratedFrameCount());
    }

    private static int supportedGeneratedFrameCount() {
        if (!isSupported()) {
            return 0;
        }
        //if (!supportQueried) {
            refreshSupport();
       // }
        return maxGeneratedFrameCount;
    }

    public static boolean isSupported() {
        return Streamline.isInitialized()
                && Streamline.isDLSSGSupported()
                && LowLatency.mode() == LowLatencyMode.NVReflex;
    }

    public static void setFrameGenerationMode(FrameGenerationMode mode) {
        if (mode == null || !isModeSupported(mode)) {
            mode = FrameGenerationMode.OFF;
        }
        SuperResolutionConfig.setFrameGenerationMode(mode);
        synchronizeConfiguration();
    }

    public static FrameGenerationMode displayedMode() {
        FrameGenerationMode mode = SuperResolutionConfig.getFrameGenerationMode();
        return isModeSupported(mode) ? mode : FrameGenerationMode.OFF;
    }

    private static void refreshSupport() {
        supportQueried = true;
        maxGeneratedFrameCount = 0;
        minimumWidthOrHeight = 0;
        StreamlineSession session = Streamline.session();
        if (session == null || session.isClosed()) {
            return;
        }
        try {
            StreamlineTypes.DlssGState state = new StreamlineTypes.DlssGState();
            int result = session.dlssGGetState(
                    new StreamlineTypes.Viewport(VIEWPORT),
                    state,
                    null
            );
            if (result == 0) {
                updateSupport(state);
            } else {
            }
        } catch (Throwable throwable) {
        }
    }

    private static void updateSupport(StreamlineTypes.DlssGState state) {
        maxGeneratedFrameCount = Math.clamp(
                state.numFramesToGenerateMax,
                0,
                5
        );
        minimumWidthOrHeight = Math.max(0, state.minWidthOrHeight);
    }

    public static FrameGenerationMode[] availableModes() {
        List<FrameGenerationMode> modes = new java.util.ArrayList<>();
        modes.add(FrameGenerationMode.OFF);
        int supportedFrameCount = supportedGeneratedFrameCount();
        for (FrameGenerationMode mode : FrameGenerationMode.values()) {
            if (mode != FrameGenerationMode.OFF
                    && mode.generatedFrameCount() <= supportedFrameCount) {
                modes.add(mode);
            }
        }
        return modes.toArray(FrameGenerationMode[]::new);
    }

    private static void synchronizeConfiguration() {
        FrameGenerationMode mode = SuperResolutionConfig.getFrameGenerationMode();
        if (mode == FrameGenerationMode.OFF) {
            //PresentationFeature.refreshVsyncForFrameGeneration();
            return;
        }
        if (!isModeSupported(mode)) {
            SuperResolutionConfig.setFrameGenerationMode(FrameGenerationMode.OFF);
            SuperResolutionConfig.SPEC.save();
            //PresentationFeature.refreshVsyncForFrameGeneration();
            return;
        }
        if (SuperResolutionConfig.getNVIDIAReflexMode() == NVIDIAReflexMode.OFF) {
            SuperResolutionConfig.setNVIDIAReflexMode(NVIDIAReflexMode.ON);
            SuperResolutionConfig.SPEC.save();
        }
        //PresentationFeature.refreshVsyncForFrameGeneration();
    }

    private static StreamlineTypes.Constants toStreamlineConstants(FGConstants source) {
        StreamlineTypes.Constants constants = new StreamlineTypes.Constants();
        constants.cameraViewToClip = Arrays.copyOf(source.cameraViewToClip, 16);
        constants.clipToCameraView = Arrays.copyOf(source.clipToCameraView, 16);
        constants.clipToLensClip = Arrays.copyOf(source.clipToLensClip, 16);
        constants.clipToPrevClip = Arrays.copyOf(source.clipToPrevClip, 16);
        constants.prevClipToClip = Arrays.copyOf(source.prevClipToClip, 16);
        constants.jitterOffsetX = source.jitterOffsetX;
        constants.jitterOffsetY = source.jitterOffsetY;
        constants.motionVectorScaleX = source.motionVectorScaleX;
        constants.motionVectorScaleY = source.motionVectorScaleY;
        constants.cameraPinholeOffsetX = source.cameraPinholeOffsetX;
        constants.cameraPinholeOffsetY = source.cameraPinholeOffsetY;
        constants.cameraPosX = source.cameraPosX;
        constants.cameraPosY = source.cameraPosY;
        constants.cameraPosZ = source.cameraPosZ;
        constants.cameraUpX = source.cameraUpX;
        constants.cameraUpY = source.cameraUpY;
        constants.cameraUpZ = source.cameraUpZ;
        constants.cameraRightX = source.cameraRightX;
        constants.cameraRightY = source.cameraRightY;
        constants.cameraRightZ = source.cameraRightZ;
        constants.cameraFwdX = source.cameraFwdX;
        constants.cameraFwdY = source.cameraFwdY;
        constants.cameraFwdZ = source.cameraFwdZ;
        constants.cameraNear = source.cameraNear;
        constants.cameraFar = source.cameraFar;
        constants.cameraFov = source.cameraFov;
        constants.cameraAspectRatio = source.cameraAspectRatio;
        constants.motionVectorsInvalidValue = source.motionVectorsInvalidValue;
        constants.depthInverted = source.depthInverted;
        constants.cameraMotionIncluded = source.cameraMotionIncluded;
        constants.motionVectors3D = source.motionVectors3D;
        constants.reset = source.reset;
        constants.orthographicProjection = source.orthographicProjection;
        constants.motionVectorsDilated = source.motionVectorsDilated;
        constants.motionVectorsJittered = source.motionVectorsJittered;
        constants.minRelativeLinearDepthObjectSeparation =
                source.minRelativeLinearDepthObjectSeparation;
        return constants;
    }

    private record DlssGOptionsKey(
            int mode,

            int numFramesToGenerate,

            int numBackBuffers,

            int motionVectorDepthWidth,

            int motionVectorDepthHeight,

            int colorWidth,

            int colorHeight,

            int colorBufferFormat,

            int motionVectorBufferFormat,

            int depthBufferFormat,

            int hudlessBufferFormat
    ) {
        private static DlssGOptionsKey basic(int mode, int numFramesToGenerate) {
            return new DlssGOptionsKey(
                    mode,
                    numFramesToGenerate,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        private static DlssGOptionsKey off() {
            return basic(StreamlineTypes.DlssGMode.OFF, 1);
        }
    }
}
