#if MC_VER >= MC_26_1
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

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.perf.FramePacingTrace;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.vulkan.EXTPresentTiming.*;
import static org.lwjgl.vulkan.KHRCalibratedTimestamps.*;
import static org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.*;
import static org.lwjgl.vulkan.KHRPresentId2.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Optional VK_EXT_present_timing support for the 26.1+ LWJGL line.
 *
 * <p>The extension reports timestamps in a swapchain-local time domain. Results
 * are calibrated back to the host monotonic clock before they enter the CPU
 * frame-pacing trace.</p>
 */
final class PresentTimingSupport {
    static final int VISIBLE_STAGE = VK_PRESENT_STAGE_IMAGE_FIRST_PIXEL_VISIBLE_BIT_EXT;
    static final int OUT_STAGE = VK_PRESENT_STAGE_IMAGE_FIRST_PIXEL_OUT_BIT_EXT;

    private static final int MAX_RESULTS = 64;
    private static final int MAX_STAGES = 4;

    private final VulkanPresentationContext context;
    private final VulkanDevice device;
    private final VkDevice vkDevice;
    private final Map<Long, TraceContext> pending = new HashMap<>();

    private long swapchain;
    private boolean enabled;
    private int requestedStage;
    private int timeDomain;
    private long timeDomainId;

    PresentTimingSupport(VulkanPresentationContext context) {
        this.context = context;
        this.device = context.device();
        this.vkDevice = device.getVkDevice();
    }

    static SurfaceSupport querySurfaceSupport(
            VulkanPresentationContext context,
            MemoryStack stack
    ) {
        if (!context.renderSystem().isPresentTimingEnabled()
                || !context.renderSystem().isPresentId2Enabled()) {
            return SurfaceSupport.disabled();
        }

        try {
            VkPhysicalDeviceSurfaceInfo2KHR surfaceInfo =
                    VkPhysicalDeviceSurfaceInfo2KHR.calloc(stack)
                            .sType(KHRGetSurfaceCapabilities2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SURFACE_INFO_2_KHR)
                            .surface(context.surface());
            VkPresentTimingSurfaceCapabilitiesEXT timingCapabilities =
                    VkPresentTimingSurfaceCapabilitiesEXT.calloc(stack)
                            .sType(EXTPresentTiming.VK_STRUCTURE_TYPE_PRESENT_TIMING_SURFACE_CAPABILITIES_EXT);
            VkSurfaceCapabilitiesPresentId2KHR presentId2Capabilities =
                    VkSurfaceCapabilitiesPresentId2KHR.calloc(stack)
                            .sType(KHRPresentId2.VK_STRUCTURE_TYPE_SURFACE_CAPABILITIES_PRESENT_ID_2_KHR);
            VkSurfaceCapabilities2KHR capabilities =
                    VkSurfaceCapabilities2KHR.calloc(stack)
                            .sType(KHRGetSurfaceCapabilities2.VK_STRUCTURE_TYPE_SURFACE_CAPABILITIES_2_KHR)
                            .pNext(timingCapabilities)
                            .pNext(presentId2Capabilities);
            int result = vkGetPhysicalDeviceSurfaceCapabilities2KHR(
                    context.physicalDevice(),
                    surfaceInfo,
                    capabilities
            );
            if (result != VK_SUCCESS) {
                return SurfaceSupport.disabled();
            }
            SurfaceSupport support = new SurfaceSupport(
                    timingCapabilities.presentTimingSupported(),
                    presentId2Capabilities.presentId2Supported(),
                    timingCapabilities.presentStageQueries()
            );
            SuperResolution.LOGGER.info(
                    "Vulkan present timing surface support: timing={}, presentId2={}, stages=0x{}",
                    support.presentTimingSupported(),
                    support.presentId2Supported(),
                    Integer.toHexString(support.presentStageQueries())
            );
            return support;
        } catch (Throwable throwable) {
            SuperResolution.LOGGER.info(
                    "Vulkan present timing surface query is unavailable: {}",
                    String.valueOf(throwable.getMessage())
            );
            return SurfaceSupport.disabled();
        }
    }

    synchronized void configure(
            long targetSwapchain,
            SurfaceSupport surfaceSupport
    ) {
        reset();
        if (!context.renderSystem().isPresentTimingEnabled()
                || !context.renderSystem().isPresentId2Enabled()
                || !surfaceSupport.presentTimingSupported()
                || !surfaceSupport.presentId2Supported()
                || (surfaceSupport.presentStageQueries() & (VISIBLE_STAGE | OUT_STAGE)) == 0) {
            SuperResolution.LOGGER.info(
                    "Vulkan present timing disabled for swapchain: deviceTiming={}, devicePresentId2={}, "
                            + "surfaceTiming={}, surfacePresentId2={}, visibleStage={}",
                    context.renderSystem().isPresentTimingEnabled(),
                    context.renderSystem().isPresentId2Enabled(),
                    surfaceSupport.presentTimingSupported(),
                    surfaceSupport.presentId2Supported(),
                    (surfaceSupport.presentStageQueries() & VISIBLE_STAGE) != 0
            );
            return;
        }
        requestedStage = (surfaceSupport.presentStageQueries() & VISIBLE_STAGE) != 0
                ? VISIBLE_STAGE
                : OUT_STAGE;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSwapchainTimeDomainPropertiesEXT domains =
                    VkSwapchainTimeDomainPropertiesEXT.calloc(stack)
                            .sType(EXTPresentTiming.VK_STRUCTURE_TYPE_SWAPCHAIN_TIME_DOMAIN_PROPERTIES_EXT);
            MemoryUtil.memPutInt(
                    domains.address() + VkSwapchainTimeDomainPropertiesEXT.TIMEDOMAINCOUNT,
                    0
            );
            int result = EXTPresentTiming.vkGetSwapchainTimeDomainPropertiesEXT(
                    vkDevice,
                    targetSwapchain,
                    domains,
                    (LongBuffer) null
            );
            if (result != VK_SUCCESS || domains.timeDomainCount() <= 0) {
                return;
            }

            int count = domains.timeDomainCount();
            IntBuffer timeDomains = stack.mallocInt(count);
            LongBuffer timeDomainIds = stack.mallocLong(count);
            MemoryUtil.memPutInt(
                    domains.address() + VkSwapchainTimeDomainPropertiesEXT.TIMEDOMAINCOUNT,
                    count
            );
            MemoryUtil.memPutAddress(
                    domains.address() + VkSwapchainTimeDomainPropertiesEXT.PTIMEDOMAINS,
                    MemoryUtil.memAddress(timeDomains)
            );
            MemoryUtil.memPutAddress(
                    domains.address() + VkSwapchainTimeDomainPropertiesEXT.PTIMEDOMAINIDS,
                    MemoryUtil.memAddress(timeDomainIds)
            );
            result = EXTPresentTiming.vkGetSwapchainTimeDomainPropertiesEXT(
                    vkDevice,
                    targetSwapchain,
                    domains,
                    (LongBuffer) null
            );
            if (result != VK_SUCCESS && result != VK_INCOMPLETE) {
                return;
            }

            int selectedDomain = 0;
            long selectedDomainId = 0L;
            boolean selected = false;
            for (int index = 0; index < domains.timeDomainCount(); index++) {
                if (timeDomains.get(index) == VK_TIME_DOMAIN_SWAPCHAIN_LOCAL_EXT) {
                    selectedDomain = timeDomains.get(index);
                    selectedDomainId = timeDomainIds.get(index);
                    selected = true;
                    break;
                }
            }
            if (!selected) {
                for (int index = 0; index < domains.timeDomainCount(); index++) {
                    if (timeDomains.get(index) == VK_TIME_DOMAIN_PRESENT_STAGE_LOCAL_EXT) {
                        selectedDomain = timeDomains.get(index);
                        selectedDomainId = timeDomainIds.get(index);
                        selected = true;
                        break;
                    }
                }
            }
            if (!selected) {
                return;
            }

            result = EXTPresentTiming.vkSetSwapchainPresentTimingQueueSizeEXT(
                    vkDevice,
                    targetSwapchain,
                    MAX_RESULTS
            );
            if (result != VK_SUCCESS) {
                return;
            }

            swapchain = targetSwapchain;
            timeDomain = selectedDomain;
            timeDomainId = selectedDomainId;
            enabled = true;
            SuperResolution.LOGGER.info(
                    "Vulkan present timing enabled: swapchain=0x{}, timeDomain={}, id={}",
                    Long.toHexString(targetSwapchain),
                    timeDomain,
                    timeDomainId
            );
        } catch (Throwable throwable) {
            reset();
            SuperResolution.LOGGER.debug("Failed to initialize Vulkan present timing", throwable);
        }
    }

    synchronized boolean isEnabled() {
        return enabled;
    }

    synchronized long appendPresentInfo(
            MemoryStack stack,
            long timingPresentId,
            long pNext
    ) {
        if (!enabled || timingPresentId == 0L) {
            return pNext;
        }
        VkPresentTimingInfoEXT.Buffer timingInfos =
                VkPresentTimingInfoEXT.calloc(1, stack);
        timingInfos.get(0)
                .sType(EXTPresentTiming.VK_STRUCTURE_TYPE_PRESENT_TIMING_INFO_EXT)
                .targetTime(0L)
                .timeDomainId(timeDomainId)
                .presentStageQueries(requestedStage)
                .targetTimeDomainPresentStage(0);
        return VkPresentTimingsInfoEXT.calloc(stack)
                .sType(EXTPresentTiming.VK_STRUCTURE_TYPE_PRESENT_TIMINGS_INFO_EXT)
                .pNext(pNext)
                .swapchainCount(1)
                .pTimingInfos(timingInfos)
                .address();
    }

    synchronized void register(long presentId, FramePacingTrace.Context context) {
        if (enabled && presentId != 0L) {
            pending.put(presentId, new TraceContext(context));
        }
    }

    synchronized void discard(long presentId) {
        pending.remove(presentId);
    }

    synchronized void collect() {
        if (!enabled || swapchain == VK_NULL_HANDLE) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentStageTimeEXT.Buffer stageTimes =
                    VkPresentStageTimeEXT.calloc(MAX_RESULTS * MAX_STAGES, stack);
            VkPastPresentationTimingEXT.Buffer results =
                    VkPastPresentationTimingEXT.calloc(MAX_RESULTS, stack);
            for (int index = 0; index < MAX_RESULTS; index++) {
                VkPresentStageTimeEXT.Buffer stageSlice = VkPresentStageTimeEXT.create(
                        stageTimes.address() + (long) index * MAX_STAGES * VkPresentStageTimeEXT.SIZEOF,
                        MAX_STAGES
                );
                results.get(index)
                        .sType(EXTPresentTiming.VK_STRUCTURE_TYPE_PAST_PRESENTATION_TIMING_EXT);
                MemoryUtil.memPutInt(
                        results.get(index).address() + VkPastPresentationTimingEXT.PRESENTSTAGECOUNT,
                        MAX_STAGES
                );
                MemoryUtil.memPutAddress(
                        results.get(index).address() + VkPastPresentationTimingEXT.PPRESENTSTAGES,
                        stageSlice.address()
                );
            }

            VkPastPresentationTimingInfoEXT timingInfo =
                    VkPastPresentationTimingInfoEXT.calloc(stack)
                            .sType(EXTPresentTiming.VK_STRUCTURE_TYPE_PAST_PRESENTATION_TIMING_INFO_EXT)
                            .flags(
                                    VK_PAST_PRESENTATION_TIMING_ALLOW_PARTIAL_RESULTS_BIT_EXT
                                            | VK_PAST_PRESENTATION_TIMING_ALLOW_OUT_OF_ORDER_RESULTS_BIT_EXT
                            )
                            .swapchain(swapchain);
            VkPastPresentationTimingPropertiesEXT properties =
                    VkPastPresentationTimingPropertiesEXT.calloc(stack)
                            .sType(EXTPresentTiming.VK_STRUCTURE_TYPE_PAST_PRESENTATION_TIMING_PROPERTIES_EXT);
            MemoryUtil.memPutInt(
                    properties.address() + VkPastPresentationTimingPropertiesEXT.PRESENTATIONTIMINGCOUNT,
                    MAX_RESULTS
            );
            MemoryUtil.memPutAddress(
                    properties.address() + VkPastPresentationTimingPropertiesEXT.PPRESENTATIONTIMINGS,
                    results.address()
            );
            int result = EXTPresentTiming.vkGetPastPresentationTimingEXT(
                    vkDevice,
                    timingInfo,
                    properties
            );
            if (result != VK_SUCCESS && result != VK_INCOMPLETE) {
                return;
            }

            for (int index = 0; index < properties.presentationTimingCount(); index++) {
                VkPastPresentationTimingEXT timing = results.get(index);
                TraceContext traceContext = pending.get(timing.presentId());
                if (traceContext == null) {
                    continue;
                }
                boolean emitted = false;
                VkPresentStageTimeEXT.Buffer presentStages = timing.pPresentStages();
                if (presentStages != null) {
                    for (int stageIndex = 0; stageIndex < timing.presentStageCount(); stageIndex++) {
                        VkPresentStageTimeEXT stage = presentStages.get(stageIndex);
                        if (stage.stage() != requestedStage || stage.time() == 0L) {
                            continue;
                        }
                        long hostTimestamp = calibrate(
                                stack,
                                timing.timeDomain(),
                                timing.timeDomainId(),
                                stage.time()
                        );
                        if (hostTimestamp != 0L) {
                            FramePacingTrace.INSTANCE.instantAt(
                                    requestedStage == VISIBLE_STAGE
                                            ? "present_image_first_pixel_visible"
                                            : "present_image_first_pixel_out",
                                    traceContext.context(),
                                    hostTimestamp,
                                    "complete",
                                    "present_stage=" + stageName(requestedStage)
                                            + ",time_domain=" + timing.timeDomain()
                                            + ",time_domain_id=" + timing.timeDomainId()
                            );
                            emitted = true;
                        }
                        break;
                    }
                }
                if (emitted || timing.reportComplete()) {
                    pending.remove(timing.presentId());
                }
            }
        } catch (Throwable throwable) {
            SuperResolution.LOGGER.debug("Failed to collect Vulkan present timing results", throwable);
        }
    }

    synchronized void reset() {
        enabled = false;
        requestedStage = 0;
        swapchain = VK_NULL_HANDLE;
        timeDomain = 0;
        timeDomainId = 0L;
        pending.clear();
    }

    private long calibrate(
            MemoryStack stack,
            int resultDomain,
            long resultDomainId,
            long resultTimestamp
    ) {
        int hostDomain = Platform.get() == Platform.WINDOWS
                #if MC_VER >= MC_26_3
                ? KHRCalibratedTimestamps.VK_TIME_DOMAIN_QUERY_PERFORMANCE_COUNTER_KHR
                : KHRCalibratedTimestamps.VK_TIME_DOMAIN_CLOCK_MONOTONIC_KHR
                #else
                ? EXTCalibratedTimestamps.VK_TIME_DOMAIN_QUERY_PERFORMANCE_COUNTER_KHR
                : EXTCalibratedTimestamps.VK_TIME_DOMAIN_CLOCK_MONOTONIC_KHR
                #endif
                ;
        VkCalibratedTimestampInfoKHR.Buffer infos =
                VkCalibratedTimestampInfoKHR.calloc(2, stack);
        infos.get(0)
                .sType(KHRCalibratedTimestamps.VK_STRUCTURE_TYPE_CALIBRATED_TIMESTAMP_INFO_KHR)
                .timeDomain(hostDomain);
        VkSwapchainCalibratedTimestampInfoEXT swapchainInfo =
                VkSwapchainCalibratedTimestampInfoEXT.calloc(stack)
                        .sType(EXTPresentTiming.VK_STRUCTURE_TYPE_SWAPCHAIN_CALIBRATED_TIMESTAMP_INFO_EXT)
                        .swapchain(swapchain)
                        .presentStage(resultDomain == VK_TIME_DOMAIN_PRESENT_STAGE_LOCAL_EXT
                                ? requestedStage
                                : 0)
                        .timeDomainId(resultDomainId);
        infos.get(1)
                .sType(KHRCalibratedTimestamps.VK_STRUCTURE_TYPE_CALIBRATED_TIMESTAMP_INFO_KHR)
                .pNext(swapchainInfo.address())
                .timeDomain(resultDomain);
        LongBuffer timestamps = stack.mallocLong(2);
        LongBuffer maxDeviation = stack.mallocLong(1);
        long before = System.nanoTime();
        int result = KHRCalibratedTimestamps.vkGetCalibratedTimestampsKHR(
                vkDevice,
                infos,
                timestamps,
                maxDeviation
        );
        long after = System.nanoTime();
        if (result != VK_SUCCESS) {
            return 0L;
        }
        long hostAtQuery = before + (after - before) / 2L;
        return resultTimestamp + hostAtQuery - timestamps.get(1);
    }

    private static String stageName(int stage) {
        return stage == VISIBLE_STAGE ? "image_first_pixel_visible" : "image_first_pixel_out";
    }

    record SurfaceSupport(
            boolean presentTimingSupported,
            boolean presentId2Supported,
            int presentStageQueries
    ) {
        static SurfaceSupport disabled() {
            return new SurfaceSupport(false, false, 0);
        }
    }

    private record TraceContext(FramePacingTrace.Context context) {
    }
}
#endif
