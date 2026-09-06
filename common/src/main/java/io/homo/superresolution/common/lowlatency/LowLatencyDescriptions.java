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

package io.homo.superresolution.common.lowlatency;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.LowLatencyRegisterEvent;
import io.homo.superresolution.api.registry.LowLatencyDescription;
import io.homo.superresolution.api.registry.LowLatencyGroups;
import io.homo.superresolution.api.registry.LowLatencyRegistry;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.common.config.ConfigSpecType;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexMode;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexVulkanProvider;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.presentation.api.PresentationBackendType;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class LowLatencyDescriptions {
    /** Group representative id for "no low latency". */
    public static final String NONE_ID = LowLatencyGroups.NONE.getId();
    /** Group representative id for the NVIDIA Reflex algorithm group. */
    public static final String NV_REFLEX_GROUP_ID = LowLatencyGroups.NV_REFLEX.getId();
    /** SR-provided backend inside the NV_REFLEX group. */
    public static final String REFLEX_VK_BACKEND_ID = "superresolution:reflex_vk";

    private static boolean registered;

    private LowLatencyDescriptions() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        // "None" — both a group representative and the actual no-op provider.
        LowLatencyRegistry.register(
                LowLatencyDescription.builder()
                        .id(NONE_ID)
                        .displayName(LowLatencyGroups.NONE.getDisplayName())
                        .providerFactory(NoneLowLatency::new)
                        .build()
        );

        // NVIDIA Reflex — group representative. Carries the group-level Reflex mode option
        // shared by every backend in the group; the negotiator picks the concrete backend at
        // runtime. providerFactory is a safe fallback that is never selected (backends are
        // resolved through their own descriptions).
        LowLatencyRegistry.register(
                LowLatencyDescription.builder()
                        .id(NV_REFLEX_GROUP_ID)
                        .displayName(LowLatencyGroups.NV_REFLEX.getDisplayName())
                        .providerFactory(NoneLowLatency::new)
                        .addOptionDescription(
                                SpecialConfigDescription.of(
                                                "mode",
                                                ConfigSpecType.ENUM,
                                                NVIDIAReflexMode.OFF
                                        )
                                         .setName(Component.translatable("superresolution.screen.config.options.label.nv_reflex_mode"))
                                         .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.nv_reflex_mode"))
                                         .setClazz(NVIDIAReflexMode.class)
                                         .setRequirement(Requirement.nothing().isTrue(
                                                 () -> PresentationBackendManager.isPresentationBackendAvailable(
                                                         PresentationBackendType.VULKAN
                                                 )
                                         ))
                                         .setValueNameSupplier((v) -> Optional.of(Component.translatable("superresolution.enum.nvreflexmode." + v.name().toLowerCase())))
                                        .setValueSupplier(SuperResolutionConfig::getNVIDIAReflexMode)
                                        // Frame generation rides on Reflex, so it must not be
                                        // switched off while frame generation is running.
                                        .setItemEnableRequirement(mode -> mode != NVIDIAReflexMode.OFF
                                                || !FrameGeneration.isFrameGenerationEnabled())
                                        .setSaveConsumer(SuperResolutionConfig::setNVIDIAReflexMode)
                        )
                        .build()
        );

        // VK_NV_low_latency2 backend inside the NV Reflex group. The Streamline-based
        // Reflex backend is contributed by the Wisteria mod at a higher priority.
        LowLatencyRegistry.register(
                LowLatencyDescription.builder()
                        .id(REFLEX_VK_BACKEND_ID)
                        .displayName(Component.literal("VK_NV_low_latency2"))
                        .group(LowLatencyGroups.NV_REFLEX)
                        .priority(100)
                        .requirement(
                                Requirement.nothing()
                                        .isTrue(() -> PresentationBackendManager.isPresentationBackendAvailable(
                                                PresentationBackendType.VULKAN
                                        ))
                                        .isTrue(NVIDIAReflexVulkanProvider::isSupported)
                        )
                        .providerFactory(NVIDIAReflexVulkanProvider::new)
                        .build()
        );

        SuperResolutionAPI.EVENT_BUS.post(new LowLatencyRegisterEvent());
    }
}
