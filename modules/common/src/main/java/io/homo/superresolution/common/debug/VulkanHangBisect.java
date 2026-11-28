/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.debug;

/**
 * JVM switches for bisecting a GPU hang (NVIDIA Xid 109, context switch timeout) inside SR's
 * upscale step on Minecraft's Vulkan backend. Every switch defaults to on, which is the normal
 * behaviour; pass {@code -D<name>=false} to turn one off.
 * <ul>
 *     <li>{@code sr.vk.checkpoints}: GPU checkpoints around each SR step in Minecraft's command
 *     buffer, which Minecraft prints in its device-lost crash report;</li>
 *     <li>{@code sr.vk.inPlaceInputs}: read Iris inputs through a region view instead of a copy;</li>
 *     <li>{@code sr.vk.publishProviderOutput}: publish the provider's output image instead of a
 *     copy of it;</li>
 *     <li>{@code sr.vk.combinedCapture}: capture frame-generation depth and motion vectors in one
 *     pass instead of two;</li>
 *     <li>{@code sr.vk.markWrittenTargets}: tell Iris which targets SR wrote instead of
 *     invalidating all of its mipmaps;</li>
 *     <li>{@code sr.vk.gpuTimers}: GPU timestamps for the frame-rate log (detailed profiling
 *     still creates them).</li>
 * </ul>
 */
public final class VulkanHangBisect {
    public static final boolean CHECKPOINTS = enabled("sr.vk.checkpoints");
    public static final boolean IN_PLACE_INPUTS = enabled("sr.vk.inPlaceInputs");
    public static final boolean PUBLISH_PROVIDER_OUTPUT = enabled("sr.vk.publishProviderOutput");
    public static final boolean COMBINED_CAPTURE = enabled("sr.vk.combinedCapture");
    public static final boolean MARK_WRITTEN_TARGETS = enabled("sr.vk.markWrittenTargets");
    public static final boolean GPU_TIMERS = enabled("sr.vk.gpuTimers");

    private VulkanHangBisect() {
    }

    private static boolean enabled(String name) {
        return !"false".equalsIgnoreCase(System.getProperty(name));
    }
}
