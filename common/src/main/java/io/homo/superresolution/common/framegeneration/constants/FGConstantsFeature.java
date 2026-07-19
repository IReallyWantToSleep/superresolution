/*
 * Anemone Mod
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

package io.homo.superresolution.common.framegeneration.constants;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.AlgorithmDispatchEvent;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.VulkanInteropAlgorithm;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.workmode.SRWorkModeProvider;
import io.homo.superresolution.common.workmode.SRWorkModeState;
import net.minecraft.client.Minecraft;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FGConstantsFeature {
    private static final int CONSTANTS_CAPACITY = Math.max(16, VulkanInteropAlgorithm.MAX_IN_FLIGHT_FRAME * 4);
    private static final Map<Integer, FGConstants> CONSTANTS = new LinkedHashMap<>();

    private static boolean registered;
    private static boolean initialized;
    private static boolean renderFrameOpen;
    private static boolean dispatchCapturedThisRender;
    private static boolean captureHistoryInvalid = true;
    private static CapturedCameraFrame previousCameraFrame;
    private static int latestConstantsFrame = Integer.MIN_VALUE;

    private FGConstantsFeature() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        SuperResolutionAPI.EVENT_BUS.addListener(FGConstantsFeature::onAlgorithmDispatch);
        registered = true;
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        clearState();
        initialized = true;
    }

    public static synchronized void beginRenderFrame() {
        if (!initialized) {
            return;
        }
        renderFrameOpen = true;
        dispatchCapturedThisRender = false;
    }

    public static synchronized void endRenderFrame() {
        if (!initialized || !renderFrameOpen) {
            return;
        }
        renderFrameOpen = false;
        if (!dispatchCapturedThisRender) {
            invalidateHistoryInternal();
        }
    }

    public static synchronized FGConstants getConstants(int frameIndex) {
        return copyConstants(CONSTANTS.get(frameIndex));
    }

    public static synchronized FGConstants getLatestConstants() {
        return copyConstants(CONSTANTS.get(latestConstantsFrame));
    }

    public static synchronized int getLatestConstantsFrame() {
        return latestConstantsFrame;
    }

    public static synchronized void invalidateHistory(String reason) {
        if (initialized) {
            invalidateHistoryInternal();
        }
    }

    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }
        clearState();
        initialized = false;
    }

    public static synchronized void onAlgorithmDispatch(AlgorithmDispatchEvent event) {
        if (!initialized) {
            return;
        }
        dispatchCapturedThisRender = true;
        if (event == null) {
            invalidateHistoryInternal();
            return;
        }
        DispatchResource dispatch = event.getDispatchResource();
        if (dispatch == null) {
            invalidateHistoryInternal();
            return;
        }
        int frameIndex = dispatch.frameCount();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            invalidateHistoryInternal();
            return;
        }
        SRWorkModeProvider workModeProvider = SRWorkModeManager.getCurrentProvider();
        SRWorkModeState workModeState = SRWorkModeManager.getCurrentState();
        String workModeId = workModeProvider.id();
        boolean motionVectorsJittered = workModeState.initializationDescription().isMotionJittered();
        boolean cameraMotionIncluded =
                SRWorkModeManager.SHADER_COMPAT.equals(workModeId)
                        && workModeState.shaderPackInUse()
                        && !workModeState.shaderPackLoading();
        float aspectRatio = FGConstantsBuilder.resolveAspectRatio(
                event.getDispatchResource().projectionMatrix(),
                dispatch.renderWidth(),
                dispatch.renderHeight()
        );

        Vector3f right = new Vector3f(MinecraftCameraState.getLeftVector()).negate();
        FGConstantsBuilder.CameraFrame cameraFrame = new FGConstantsBuilder.CameraFrame(
                frameIndex,
                event.getDispatchResource().projectionMatrix(),
                MinecraftCameraState.getViewRotationMatrix(),
                MinecraftCameraState.getPosition(),
                MinecraftCameraState.getUpVector(),
                right,
                MinecraftCameraState.getLookVector(),
                MinecraftUtils.getCameraNear(),
                MinecraftUtils.getCameraFar(),
                (float) Math.toRadians(MinecraftCameraState.getFov()),
                aspectRatio
        );
        ContinuityKey continuityKey = new ContinuityKey(
                dispatch.renderWidth(),
                dispatch.renderHeight(),
                dispatch.screenWidth(),
                dispatch.screenHeight(),
                workModeId,
                workModeState.internalTextureFormat(),
                workModeState.motionVectorPreprocessingFunction(),
                cameraMotionIncluded,
                motionVectorsJittered
        );
        boolean reset = captureHistoryInvalid
                || previousCameraFrame == null
                || frameIndex != previousCameraFrame.frame.frameIndex() + 1
                || !continuityKey.equals(previousCameraFrame.continuityKey);
        FGCameraSnapshot snapshot = FGConstantsBuilder.build(
                cameraFrame,
                reset ? null : previousCameraFrame.frame,
                reset,
                cameraMotionIncluded,
                motionVectorsJittered
        );
        FGConstants constants = snapshot.toConstants();
        constants.jitterOffsetX = dispatch.jitterOffset().x;
        constants.jitterOffsetY = dispatch.jitterOffset().y;
        constants.motionVectorScaleX = 1;
        constants.motionVectorScaleY = 1;
        CONSTANTS.put(frameIndex, constants);
        latestConstantsFrame = frameIndex;
        trimConstants();
        previousCameraFrame = new CapturedCameraFrame(cameraFrame, continuityKey);
        captureHistoryInvalid = false;
    }

    private static void invalidateHistoryInternal() {
        captureHistoryInvalid = true;
        previousCameraFrame = null;
    }

    private static void trimConstants() {
        while (CONSTANTS.size() > CONSTANTS_CAPACITY) {
            Iterator<Integer> iterator = CONSTANTS.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private static FGConstants copyConstants(FGConstants source) {
        if (source == null) {
            return null;
        }
        FGConstants copy = new FGConstants();
        copy.cameraViewToClip = source.cameraViewToClip.clone();
        copy.clipToCameraView = source.clipToCameraView.clone();
        copy.clipToLensClip = source.clipToLensClip.clone();
        copy.clipToPrevClip = source.clipToPrevClip.clone();
        copy.prevClipToClip = source.prevClipToClip.clone();
        copy.jitterOffsetX = source.jitterOffsetX;
        copy.jitterOffsetY = source.jitterOffsetY;
        copy.motionVectorScaleX = source.motionVectorScaleX;
        copy.motionVectorScaleY = source.motionVectorScaleY;
        copy.cameraPinholeOffsetX = source.cameraPinholeOffsetX;
        copy.cameraPinholeOffsetY = source.cameraPinholeOffsetY;
        copy.cameraPosX = source.cameraPosX;
        copy.cameraPosY = source.cameraPosY;
        copy.cameraPosZ = source.cameraPosZ;
        copy.cameraUpX = source.cameraUpX;
        copy.cameraUpY = source.cameraUpY;
        copy.cameraUpZ = source.cameraUpZ;
        copy.cameraRightX = source.cameraRightX;
        copy.cameraRightY = source.cameraRightY;
        copy.cameraRightZ = source.cameraRightZ;
        copy.cameraFwdX = source.cameraFwdX;
        copy.cameraFwdY = source.cameraFwdY;
        copy.cameraFwdZ = source.cameraFwdZ;
        copy.cameraNear = source.cameraNear;
        copy.cameraFar = source.cameraFar;
        copy.cameraFov = source.cameraFov;
        copy.cameraAspectRatio = source.cameraAspectRatio;
        copy.motionVectorsInvalidValue = source.motionVectorsInvalidValue;
        copy.depthInverted = source.depthInverted;
        copy.cameraMotionIncluded = source.cameraMotionIncluded;
        copy.motionVectors3D = source.motionVectors3D;
        copy.reset = source.reset;
        copy.orthographicProjection = source.orthographicProjection;
        copy.motionVectorsDilated = source.motionVectorsDilated;
        copy.motionVectorsJittered = source.motionVectorsJittered;
        copy.minRelativeLinearDepthObjectSeparation = source.minRelativeLinearDepthObjectSeparation;
        return copy;
    }

    private static void clearState() {
        CONSTANTS.clear();
        renderFrameOpen = false;
        dispatchCapturedThisRender = false;
        captureHistoryInvalid = true;
        previousCameraFrame = null;
        latestConstantsFrame = Integer.MIN_VALUE;
    }

    private record CapturedCameraFrame(
            FGConstantsBuilder.CameraFrame frame,

            ContinuityKey continuityKey
    ) {
    }

    private record ContinuityKey(
            int renderWidth,

            int renderHeight,

            int screenWidth,

            int screenHeight,

            String workModeId,

            Object internalTextureFormat,

            String motionVectorPreprocessingFunction,

            boolean cameraMotionIncluded,

            boolean motionVectorsJittered
    ) {
    }

}
