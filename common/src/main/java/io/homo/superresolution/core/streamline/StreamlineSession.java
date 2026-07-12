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

package io.homo.superresolution.core.streamline;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class StreamlineSession implements AutoCloseable {
    private final AtomicLong nativeHandle;

    private StreamlineSession(long nativeHandle) {
        this.nativeHandle = new AtomicLong(nativeHandle);
    }

    static StreamlineSession open(StreamlineInitConfig config) {
        Objects.requireNonNull(config, "config");
        long[] handle = new long[1];
        int result = StreamlineNative.nOpen(
                config.showConsole,
                config.logLevel,
                config.preferenceFlags,
                config.pluginPaths,
                config.logPath,
                config.features,
                config.applicationId,
                config.engine,
                config.engineVersion,
                config.projectId,
                config.logListener,
                handle
        );
        if (result != 0) {
            throw new StreamlineException("slInit", result);
        }
        if (handle[0] == 0L) {
            throw new IllegalStateException("slInit returned success without a session handle");
        }
        return new StreamlineSession(handle[0]);
    }

    private static void requireBooleanOut(boolean[] value) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("output array must contain one element");
        }
    }

    private static void requireViewport(StreamlineTypes.Viewport viewport) {
        Objects.requireNonNull(viewport, "viewport");
    }

    private static void requireToken(StreamlineTypes.FrameToken token) {
        if (token == null || token.nativeHandle == 0L) {
            throw new IllegalArgumentException("A valid FrameToken is required");
        }
    }

    private static void requireTags(StreamlineTypes.ResourceTag[] tags) {
        if (tags == null || tags.length == 0) {
            throw new IllegalArgumentException("At least one ResourceTag is required");
        }
        for (StreamlineTypes.ResourceTag tag : tags) {
            StreamlineTypes.requireResourceTag(tag);
        }
    }

    private static void validateEvaluateInput(StreamlineTypes.EvaluateInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Evaluation input cannot be null");
        }
        switch (input.kind) {
            case StreamlineTypes.EvaluateInputKind.VIEWPORT -> requireViewport(input.viewport);
            case StreamlineTypes.EvaluateInputKind.RESOURCE_TAG ->
                    StreamlineTypes.requireResourceTag(input.resourceTag);
            case StreamlineTypes.EvaluateInputKind.CONSTANTS -> StreamlineTypes.requireConstants(input.constants);
            default -> throw new IllegalArgumentException("Unknown evaluation input kind: " + input.kind);
        }
    }

    private static void validateCameraData(StreamlineTypes.ReflexCameraData value) {
        StreamlineTypes.requireMatrix(value.worldToViewMatrix, "worldToViewMatrix");
        StreamlineTypes.requireMatrix(value.viewToClipMatrix, "viewToClipMatrix");
        StreamlineTypes.requireMatrix(value.previousRenderedWorldToViewMatrix, "previousRenderedWorldToViewMatrix");
        StreamlineTypes.requireMatrix(value.previousRenderedViewToClipMatrix, "previousRenderedViewToClipMatrix");
    }

    public boolean isClosed() {
        long handle = nativeHandle.get();
        return handle == 0L || !StreamlineNative.nIsSessionActive(handle);
    }

    public int shutdown() {
        long handle = nativeHandle.getAndSet(0L);
        return handle == 0L ? 0 : StreamlineNative.nClose(handle);
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * This API is not thread safe. Call it immediately after creating the Vulkan device.
     */
    public int setVulkanInfo(StreamlineTypes.VulkanInfo info) {
        Objects.requireNonNull(info, "info");
        if (info.device == 0L || info.instance == 0L || info.physicalDevice == 0L) {
            throw new IllegalArgumentException("VulkanInfo requires device, instance and physicalDevice");
        }
        return StreamlineNative.nSetVulkanInfo(
                requireHandle(),
                info.device,
                info.instance,
                info.physicalDevice,
                info.computeQueueIndex,
                info.computeQueueFamily,
                info.graphicsQueueIndex,
                info.graphicsQueueFamily,
                info.opticalFlowQueueIndex,
                info.opticalFlowQueueFamily,
                info.useNativeOpticalFlowMode,
                info.computeQueueCreateFlags,
                info.graphicsQueueCreateFlags,
                info.opticalFlowQueueCreateFlags
        );
    }

    /**
     * This API is not thread safe.
     */
    public int isFeatureSupported(int feature, long physicalDevice, boolean[] outSupported) {
        requireBooleanOut(outSupported);
        return StreamlineNative.nIsFeatureSupported(requireHandle(), feature, physicalDevice, outSupported);
    }

    /**
     * This API is not thread safe and requires a configured Vulkan device.
     */
    public int isFeatureLoaded(int feature, boolean[] outLoaded) {
        requireBooleanOut(outLoaded);
        return StreamlineNative.nIsFeatureLoaded(requireHandle(), feature, outLoaded);
    }

    /**
     * This API is not thread safe. The caller must flush relevant Vulkan work first.
     */
    public int setFeatureLoaded(int feature, boolean loaded) {
        return StreamlineNative.nSetFeatureLoaded(requireHandle(), feature, loaded);
    }

    /**
     * This API is not thread safe.
     */
    public int getFeatureRequirements(int feature, StreamlineTypes.FeatureRequirements outRequirements) {
        return StreamlineNative.nGetFeatureRequirements(
                requireHandle(),
                feature,
                Objects.requireNonNull(outRequirements, "outRequirements")
        );
    }

    public int getFeatureVersion(int feature, StreamlineTypes.FeatureVersion outVersion) {
        return StreamlineNative.nGetFeatureVersion(
                requireHandle(),
                feature,
                Objects.requireNonNull(outVersion, "outVersion")
        );
    }

    public int getNewFrameToken(Integer frameIndex, StreamlineTypes.FrameToken outToken) {
        Objects.requireNonNull(outToken, "outToken");
        return StreamlineNative.nGetNewFrameToken(
                requireHandle(),
                frameIndex != null,
                frameIndex == null ? 0 : frameIndex,
                outToken
        );
    }

    /**
     * This API is thread safe after Vulkan device setup.
     */
    @Deprecated
    public int setTag(StreamlineTypes.Viewport viewport, StreamlineTypes.ResourceTag[] tags, long commandBuffer) {
        requireViewport(viewport);
        requireTags(tags);
        return StreamlineNative.nSetTag(requireHandle(), viewport.value, tags, commandBuffer);
    }

    /**
     * This API is thread safe after Vulkan device setup.
     */
    public int setTagForFrame(
            StreamlineTypes.FrameToken frame,
            StreamlineTypes.Viewport viewport,
            StreamlineTypes.ResourceTag[] tags,
            long commandBuffer
    ) {
        requireToken(frame);
        requireViewport(viewport);
        requireTags(tags);
        return StreamlineNative.nSetTagForFrame(requireHandle(), frame.nativeHandle, viewport.value, tags, commandBuffer);
    }

    /**
     * This API is thread safe after Vulkan device setup.
     */
    public int setConstants(
            StreamlineTypes.Constants constants,
            StreamlineTypes.FrameToken frame,
            StreamlineTypes.Viewport viewport
    ) {
        StreamlineTypes.requireConstants(constants);
        requireToken(frame);
        requireViewport(viewport);
        return StreamlineNative.nSetConstants(requireHandle(), constants, frame.nativeHandle, viewport.value);
    }

    /**
     * This API is not thread safe and requires a configured Vulkan device.
     */
    public int allocateResources(long commandBuffer, int feature, StreamlineTypes.Viewport viewport) {
        requireViewport(viewport);
        return StreamlineNative.nAllocateResources(requireHandle(), commandBuffer, feature, viewport.value);
    }

    /**
     * This API is not thread safe. Vulkan work that uses the feature must be complete first.
     */
    public int freeResources(int feature, StreamlineTypes.Viewport viewport) {
        requireViewport(viewport);
        return StreamlineNative.nFreeResources(requireHandle(), feature, viewport.value);
    }

    /**
     * This API is not thread safe. Input frame and viewport values must match prior setup calls.
     */
    public int evaluateFeature(
            int feature,
            StreamlineTypes.FrameToken frame,
            StreamlineTypes.EvaluateInput[] inputs,
            long commandBuffer
    ) {
        requireToken(frame);
        if (inputs == null || inputs.length == 0) {
            throw new IllegalArgumentException("At least one evaluation input is required");
        }
        for (StreamlineTypes.EvaluateInput input : inputs) {
            validateEvaluateInput(input);
        }
        return StreamlineNative.nEvaluateFeature(requireHandle(), feature, frame.nativeHandle, inputs, commandBuffer);
    }

    /**
     * Returns the raw native function pointer resolved by slGetFeatureFunction.
     * The caller is responsible for using the exact native ABI.
     */
    public int getFeatureFunction(int feature, String name, long[] outAddress) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("function name is required");
        }
        if (outAddress == null || outAddress.length == 0) {
            throw new IllegalArgumentException("outAddress must contain one element");
        }
        return StreamlineNative.nGetFeatureFunction(requireHandle(), feature, name, outAddress);
    }

    public int dlssGetOptimalSettings(
            StreamlineTypes.DlssOptions options,
            StreamlineTypes.DlssOptimalSettings outSettings
    ) {
        return StreamlineNative.nDlssGetOptimalSettings(
                requireHandle(),
                Objects.requireNonNull(options, "options"),
                Objects.requireNonNull(outSettings, "outSettings")
        );
    }

    /**
     * This API is not thread safe.
     */
    public int dlssGetState(StreamlineTypes.Viewport viewport, StreamlineTypes.DlssState outState) {
        requireViewport(viewport);
        return StreamlineNative.nDlssGetState(
                requireHandle(),
                viewport.value,
                Objects.requireNonNull(outState, "outState")
        );
    }

    /**
     * This API is not thread safe.
     */
    public int dlssSetOptions(StreamlineTypes.Viewport viewport, StreamlineTypes.DlssOptions options) {
        requireViewport(viewport);
        return StreamlineNative.nDlssSetOptions(
                requireHandle(),
                viewport.value,
                Objects.requireNonNull(options, "options")
        );
    }

    /**
     * This API is not thread safe.
     */
    public int dlssGGetState(
            StreamlineTypes.Viewport viewport,
            StreamlineTypes.DlssGState outState,
            StreamlineTypes.DlssGOptions options
    ) {
        requireViewport(viewport);
        return StreamlineNative.nDlssGGetState(
                requireHandle(),
                viewport.value,
                Objects.requireNonNull(outState, "outState"),
                options
        );
    }

    /**
     * This API is not thread safe.
     */
    public int dlssGSetOptions(StreamlineTypes.Viewport viewport, StreamlineTypes.DlssGOptions options) {
        requireViewport(viewport);
        return StreamlineNative.nDlssGSetOptions(
                requireHandle(),
                viewport.value,
                Objects.requireNonNull(options, "options")
        );
    }

    /**
     * This API is not thread safe.
     */
    public int pclGetState(StreamlineTypes.PclState outState) {
        return StreamlineNative.nPclGetState(requireHandle(), Objects.requireNonNull(outState, "outState"));
    }

    public int pclSetMarker(int marker, StreamlineTypes.FrameToken frame) {
        requireToken(frame);
        return StreamlineNative.nPclSetMarker(requireHandle(), marker, frame.nativeHandle);
    }

    /**
     * This API is not thread safe.
     */
    public int pclSetOptions(StreamlineTypes.PclOptions options) {
        return StreamlineNative.nPclSetOptions(requireHandle(), Objects.requireNonNull(options, "options"));
    }

    /**
     * This API is not thread safe.
     */
    public int reflexGetState(StreamlineTypes.ReflexState outState) {
        return StreamlineNative.nReflexGetState(requireHandle(), Objects.requireNonNull(outState, "outState"));
    }

    public int reflexSleep(StreamlineTypes.FrameToken frame) {
        requireToken(frame);
        return StreamlineNative.nReflexSleep(requireHandle(), frame.nativeHandle);
    }

    /**
     * This API is not thread safe.
     */
    public int reflexSetOptions(StreamlineTypes.ReflexOptions options) {
        return StreamlineNative.nReflexSetOptions(requireHandle(), Objects.requireNonNull(options, "options"));
    }

    public int reflexSetCameraData(
            StreamlineTypes.Viewport viewport,
            StreamlineTypes.FrameToken frame,
            StreamlineTypes.ReflexCameraData data
    ) {
        requireViewport(viewport);
        requireToken(frame);
        validateCameraData(Objects.requireNonNull(data, "data"));
        return StreamlineNative.nReflexSetCameraData(requireHandle(), viewport.value, frame.nativeHandle, data);
    }

    public int reflexGetPredictedCameraData(
            StreamlineTypes.Viewport viewport,
            StreamlineTypes.FrameToken frame,
            StreamlineTypes.ReflexPredictedCameraData outData
    ) {
        requireViewport(viewport);
        requireToken(frame);
        Objects.requireNonNull(outData, "outData");
        return StreamlineNative.nReflexGetPredictedCameraData(requireHandle(), viewport.value, frame.nativeHandle, outData);
    }

    long requireHandle() {
        long handle = nativeHandle.get();
        if (handle == 0L || !StreamlineNative.nIsSessionActive(handle)) {
            throw new IllegalStateException("StreamlineSession is closed");
        }
        return handle;
    }
}
