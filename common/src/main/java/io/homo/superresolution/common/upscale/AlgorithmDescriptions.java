/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.common.upscale;

import io.homo.superresolution.api.QualityPreset;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.AlgorithmRegisterEvent;
import io.homo.superresolution.api.platform.OperatingSystem;
import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.api.platform.SystemArchitecture;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.api.registry.ExtraResource;
import io.homo.superresolution.api.registry.ExtraResources;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.common.upscale.anime4k.Anime4K;
import io.homo.superresolution.common.upscale.dlss.DLSS;
import io.homo.superresolution.common.upscale.ffxfsr.FfxFSR;
import io.homo.superresolution.common.upscale.fsr1.FSR1;
import io.homo.superresolution.common.upscale.fsr2.FSR2;
import io.homo.superresolution.common.upscale.none.None;
import io.homo.superresolution.common.upscale.sgsr.v1.Sgsr1;
import io.homo.superresolution.common.upscale.sgsr.v2.Sgsr2;
import io.homo.superresolution.common.upscale.xess.XeSS;
import io.homo.superresolution.core.graphics.opengl.Gl;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AlgorithmDescriptions {
    private static final List<QualityPreset> FSR_QUALITY_PRESETS = List.of(
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.fsr.aa"))
                    .setCodeName("fsr_aa")
                    .setUpscaleRatio(1f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.fsr.quality"))
                    .setCodeName("fsr_quality")
                    .setUpscaleRatio(1.5f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.fsr.balanced"))
                    .setCodeName("fsr_balanced")
                    .setUpscaleRatio(1.7f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.fsr.performance"))
                    .setCodeName("fsr_performance")
                    .setUpscaleRatio(2.0f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.fsr.ultra_performance"))
                    .setCodeName("fsr_ultra_performance")
                    .setUpscaleRatio(3.0f)
    );
    private static final List<QualityPreset> XESS_QUALITY_PRESETS = List.of(
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.xess.ultra_performance"))
                    .setCodeName("xess_ultra_performance")
                    .setUpscaleRatio(3.0f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.xess.performance"))
                    .setCodeName("xess_performance")
                    .setUpscaleRatio(2.3f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.xess.balanced"))
                    .setCodeName("xess_balanced")
                    .setUpscaleRatio(2.0f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.xess.quality"))
                    .setCodeName("xess_quality")
                    .setUpscaleRatio(1.7f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.xess.ultra_quality"))
                    .setCodeName("xess_ultra_quality")
                    .setUpscaleRatio(1.5f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.xess.ultra_quality_plus"))
                    .setCodeName("xess_ultra_quality_plus")
                    .setUpscaleRatio(1.3f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.xess.native_aa"))
                    .setCodeName("xess_native_aa")
                    .setUpscaleRatio(1.0f)
    );
    private static final List<QualityPreset> DLSS_QUALITY_PRESETS = List.of(
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.dlss.ultra_performance"))
                    .setCodeName("dlss_ultra_performance")
                    .setUpscaleRatio(3.0f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.dlss.performance"))
                    .setCodeName("dlss_performance")
                    .setUpscaleRatio(2.0f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.dlss.balanced"))
                    .setCodeName("dlss_balanced")
                    .setUpscaleRatio(1.724f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.dlss.quality"))
                    .setCodeName("dlss_quality")
                    .setUpscaleRatio(1.5f),
            new QualityPreset()
                    .setName(Component.translatable("superresolution.algo.preset.dlss.dlaa"))
                    .setCodeName("dlss_dlaa")
                    .setUpscaleRatio(1.0f)
    );
    private static final List<QualityPreset> ANIME4K_QUALITY_PRESETS = List.of(
            new QualityPreset()
                    .setUpscaleRatio(2.0f)
                    .setName(Component.literal("2x"))
                    .setCodeName("anime4k_2x")
    );

    public static final AlgorithmDescription<None> NONE = AlgorithmDescription.builder(None.class)
            .briefName("None")
            .codeName("none")
            .displayName("None")
            .requirement(Requirement.nothing())
            .build();

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
            .briefName("AMD FSR 2 (OpenGL)")
            .codeName("fsr2")
            .displayName("AMD FidelityFX Super Resolution 2 (OpenGL)")
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

    public static final AlgorithmDescription<FfxFSR> FSR = AlgorithmDescription.builder(FfxFSR.class)
            .briefName("AMD FSR")
            .codeName("fsr")
            .displayName("AMD FidelityFX Super Resolution")
            .requirement(
                    Requirement.nothing()
                            .addSupportedOS(new OperatingSystem(SystemArchitecture.X86_64, OperatingSystemType.WINDOWS))
                            .addSupportedOS(new OperatingSystem(SystemArchitecture.X86_64, OperatingSystemType.LINUX))
                            .requiredGlExtension("GL_EXT_memory_object")
                            .requiredGlExtension("GL_EXT_semaphore")
                            .glMajorVersion(4)
                            .glMinorVersion(6)
                            .requireVulkan(true)
            )
            .supportJitter(true)
            .qualityPresets(FSR_QUALITY_PRESETS)
            .customUpscaleRatio(true)
            .build();

    public static final AlgorithmDescription<XeSS> XESS = AlgorithmDescription.builder(XeSS.class)
            .briefName("Intel XeSS")
            .codeName("xess")
            .displayName("Intel Xe Super Sampling")
            .requirement(
                    Requirement.nothing()
                            .addSupportedOS(new OperatingSystem(SystemArchitecture.X86_64, OperatingSystemType.WINDOWS))
                            .requiredGlExtension("GL_EXT_memory_object")
                            .requiredGlExtension("GL_EXT_semaphore")
                            .glMajorVersion(4)
                            .glMinorVersion(6)
                            .requireVulkan(true)
            )
            .extraResources(
                    ExtraResources.builder()
                            .add(ExtraResource.builder("libxess.dll")
                                    .addRemote(
                                            "https://cnb.cool/187J3X1-114514/mc-superresolution/-/releases/download/assets/libxess.dll",
                                            "CNB Mirror"
                                    )
                                    .build()
                            )
                            .build()
            )
            .supportJitter(true)
            .qualityPresets(XESS_QUALITY_PRESETS)
            .customUpscaleRatio(false)
            .build();

    public static final AlgorithmDescription<DLSS> DLSS = AlgorithmDescription.builder(DLSS.class)
            .briefName("NVIDIA DLSS")
            .codeName("dlss")
            .displayName("NVIDIA DLSS")
            .requirement(
                    Requirement.nothing()
                            .addSupportedOS(new OperatingSystem(SystemArchitecture.X86_64, OperatingSystemType.WINDOWS))
                            .addSupportedOS(new OperatingSystem(SystemArchitecture.X86_64, OperatingSystemType.LINUX))
                            .requiredGlExtension("GL_EXT_memory_object")
                            .requiredGlExtension("GL_EXT_semaphore")
                            .glMajorVersion(4)
                            .glMinorVersion(6)
                            .requireVulkan(true)
            )
            .extraResources(
                    Platform.currentPlatform.getOS().type == OperatingSystemType.WINDOWS
                            ? ExtraResources.builder()
                            .add(ExtraResource.builder("nvngx_dlss.dll")
                                    .addRemote(
                                            "https://cnb.cool/187J3X1-114514/mc-superresolution/-/releases/download/assets/nvngx_dlss.dll",
                                            "CNB Mirror"
                                    )
                                    .build()
                            )
                            .build()
                            : ExtraResources.builder().build()
            )
            .supportJitter(true)
            .qualityPresets(DLSS_QUALITY_PRESETS)
            .customUpscaleRatio(false)
            .build();

    public static final AlgorithmDescription<Sgsr1> SGSR1 = AlgorithmDescription.builder(Sgsr1.class)
            .briefName("SGSR V1")
            .codeName("sgsr1")
            .displayName("Snapdragon™ Game Super Resolution 1")
            .requirement(
                    Requirement.nothing()
                            .glMajorVersion(4)
                            .glMinorVersion(0)
            )
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

    public static void registryAlgorithms() {
        AlgorithmRegistry.registry(NONE);
        AlgorithmRegistry.registry(FSR1);
        AlgorithmRegistry.registry(FSR2);
        AlgorithmRegistry.registry(FSR);
        AlgorithmRegistry.registry(XESS);
        AlgorithmRegistry.registry(DLSS);
        AlgorithmRegistry.registry(SGSR1);
        AlgorithmRegistry.registry(SGSR2);
        if (Platform.currentPlatform.isDevelopmentEnvironment()) {
            AlgorithmRegistry.registry(ANIME4K);
        }
        SuperResolutionAPI.EVENT_BUS.post(new AlgorithmRegisterEvent());
    }
}
