/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.upscale.algo.legacy;

import io.homo.superresolution.api.QualityPreset;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.common.upscale.algo.legacy.anime4k.Anime4K;
import io.homo.superresolution.common.upscale.algo.legacy.fsr1.FSR1;
import io.homo.superresolution.common.upscale.algo.legacy.fsr2.FSR2;
import io.homo.superresolution.common.upscale.algo.legacy.sgsr.v1.Sgsr1;
import io.homo.superresolution.common.upscale.algo.legacy.sgsr.v2.Sgsr2;
import io.homo.superresolution.core.graphics.opengl.Gl;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class HackAlgorithmDescriptions {
    public static final AlgorithmDescription<FSR1> FSR1 = AlgorithmDescription.builder(FSR1.class)
            .briefName("AMD FSR 1")
            .codeName("fsr1")
            .displayName("AMD FidelityFX Super Resolution 1")
            .requirement(
                    Requirement.nothing()
                            .glMajorVersion(4)
                            .glMinorVersion(3)
                            .isFalse(Gl::isLegacy)
                            .isTrue(Gl::isSupportDSA)
            )
            .build();

    public static final AlgorithmDescription<FSR2> FSR2 = AlgorithmDescription.builder(FSR2.class)
            .briefName("AMD FSR 2 (Legacy)")
            .codeName("fsr2")
            .displayName("AMD FidelityFX Super Resolution 2 (Legacy)")
            .requirement(
                    Requirement.nothing()
                            .requiredGlExtension("GL_KHR_shader_subgroup")
                            .glMajorVersion(4)
                            .glMinorVersion(5)
                            .isFalse(Gl::isLegacy)
                            .isTrue(Gl::isSupportDSA)
            )
            .supportJitter(true)
            .build();

    public static final AlgorithmDescription<Sgsr1> SGSR1 = AlgorithmDescription.builder(Sgsr1.class)
            .briefName("SGSR V1")
            .codeName("sgsr1")
            .displayName("Snapdragon™ Game Super Resolution 1")
            .requirement(Requirement.nothing().glMajorVersion(4).glMinorVersion(0))
            .build();

    public static final AlgorithmDescription<Sgsr2> SGSR2 = AlgorithmDescription.builder(Sgsr2.class)
            .briefName("SGSR V2")
            .codeName("sgsr2")
            .displayName("Snapdragon™ Game Super Resolution 2")
            .requirement(
                    Requirement.nothing()
                            .glMajorVersion(4)
                            .glMinorVersion(3)
                            .isFalse(Gl::isLegacy)
                            .isTrue(Gl::isSupportDSA)
            )
            .build();

    private static final List<QualityPreset> ANIME4K_QUALITY_PRESETS = List.of(
            new QualityPreset()
                    .setUpscaleRatio(2.0f)
                    .setName(Component.literal("2x"))
                    .setCodeName("anime4k_2x")
    );

    public static final AlgorithmDescription<Anime4K> ANIME4K = AlgorithmDescription.builder(Anime4K.class)
            .briefName("Anime4K")
            .codeName("anime4k")
            .displayName("Anime4K")
            .requirement(
                    Requirement.nothing()
                            .glMajorVersion(4)
                            .glMinorVersion(3)
                            .isFalse(Gl::isLegacy)
                            .isTrue(Gl::isSupportDSA)
            )
            .qualityPresets(ANIME4K_QUALITY_PRESETS)
            .customUpscaleRatio(false)
            .build();

    private HackAlgorithmDescriptions() {
    }

    public static void register() {
        SuperResolutionAPI.registerAlgorithm(FSR1);
        SuperResolutionAPI.registerAlgorithm(FSR2);
        SuperResolutionAPI.registerAlgorithm(SGSR1);
        SuperResolutionAPI.registerAlgorithm(SGSR2);
        SuperResolutionAPI.registerAlgorithm(ANIME4K);
    }
}
