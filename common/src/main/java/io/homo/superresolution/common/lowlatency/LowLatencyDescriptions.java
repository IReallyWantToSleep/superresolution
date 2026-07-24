/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.lowlatency;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.LowLatencyRegisterEvent;
import io.homo.superresolution.api.registry.LowLatencyDescription;
import io.homo.superresolution.api.registry.LowLatencyRegistry;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.common.config.ConfigSpecType;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexMode;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexProvider;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexVulkanImpl;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.core.streamline.Streamline;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class LowLatencyDescriptions {
    private LowLatencyDescriptions() {
    }

    public static void register() {
        LowLatencyRegistry.register(
                LowLatencyDescription.builder()
                        .id("superresolution:none")
                        .displayName("None")
                        .providerFactory(NoneLowLatency::new)
                        .build()
        );

        LowLatencyRegistry.register(
                LowLatencyDescription.builder()
                        .id("superresolution:nv_reflex")
                        .displayName("NVIDIA Reflex")
                        .requirement(
                                Requirement.nothing()
                                        .isTrue(() -> VulkanPresentationFeature.isAvailable()
                                                && ((Streamline.isSupportedPlatform() && Streamline.isNativeAvailable())
                                                || NVIDIAReflexVulkanImpl.isSupported()))
                        )
                        .providerFactory(NVIDIAReflexProvider::new)
                        .addOptionDescription(
                                SpecialConfigDescription.of(
                                                "mode",
                                                ConfigSpecType.ENUM,
                                                NVIDIAReflexMode.OFF
                                        )
                                        .setName(Component.translatable("superresolution.screen.config.options.label.nv_reflex_mode"))
                                        .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.nv_reflex_mode"))
                                        .setClazz(NVIDIAReflexMode.class)
                                        .setValueNameSupplier((v) -> Optional.of(Component.translatable("superresolution.enum.nvreflexmode." + ((NVIDIAReflexMode) v).name().toLowerCase())))
                                        .setSaveConsumer((v) -> {
                                            // Value persistence is handled by SuperResolutionConfig.NVIDIA_REFLEX_MODE.
                                        })
                        )
                        .build()
        );

        SuperResolutionAPI.EVENT_BUS.post(new LowLatencyRegisterEvent());
    }
}
