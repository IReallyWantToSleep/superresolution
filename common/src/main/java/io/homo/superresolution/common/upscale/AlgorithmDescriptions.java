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
import io.homo.superresolution.common.upscale.algo.dlss.DLSS;
import io.homo.superresolution.common.upscale.algo.dlss.NgxDlssLatestProvider;
import io.homo.superresolution.common.upscale.algo.dlssrr.DLSSRR;
import io.homo.superresolution.common.upscale.algo.ffxfsr.FfxFSR;
import io.homo.superresolution.common.upscale.algo.ffxfsr.FfxFSR4D3D12;
import io.homo.superresolution.common.upscale.algo.legacy.anime4k.Anime4K;
import io.homo.superresolution.common.upscale.algo.legacy.fsr1.FSR1;
import io.homo.superresolution.common.upscale.algo.legacy.fsr2.FSR2;
import io.homo.superresolution.common.upscale.algo.legacy.sgsr.v1.Sgsr1;
import io.homo.superresolution.common.upscale.algo.legacy.sgsr.v2.Sgsr2;
import io.homo.superresolution.common.upscale.algo.none.None;
import io.homo.superresolution.common.upscale.algo.xess.XeSS;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.graphics.opengl.Gl;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AlgorithmDescriptions {
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
    public static final AlgorithmDescription<FfxFSR4D3D12> FSR4_D3D12 =
            AlgorithmDescription.builder(FfxFSR4D3D12.class)
                    .briefName("AMD FSR 4 (D3D12)")
                    .codeName("fsr4_d3d12")
                    .displayName("AMD FSR 4 (Direct3D 12)")
                    .requirement(
                            Requirement.nothing()
                                    .addSupportedOS(new OperatingSystem(
                                            SystemArchitecture.X86_64,
                                            OperatingSystemType.WINDOWS))
                                    .requiredGlExtension("GL_EXT_memory_object")
                                    .requiredGlExtension("GL_EXT_memory_object_win32")
                                    .requiredGlExtension("GL_EXT_semaphore")
                                    .requiredGlExtension("GL_EXT_semaphore_win32")
                                    .glMajorVersion(4)
                                    .glMinorVersion(6)
                                    .isTrue(NativeLibManager::d3d12InteropAvailable)
                    )
                    .extraResources(
                            ExtraResources.builder()
                                    .add(ExtraResource.builder(
                                                    FfxFSR4D3D12.UPSCALER_DLL_NAME)
                                            .addRemote(
                                                    () -> "https://raw.githubusercontent.com/GPUOpen-LibrariesAndSDKs/FidelityFX-SDK/v2.3.0/Kits/FidelityFX/signedbin/amd_fidelityfx_upscaler_dx12.dll",
                                                    "Github"
                                            )
                                            .addRemote(
                                                    () -> "https://api.fuukir.cn/dl/sr/amd_fidelityfx_upscaler_dx12.dll",
                                                    "Mirror"
                                            )
                                            .build())
                                    .build()
                    )
                    .supportJitter(true)
                    .qualityPresets(FSR_QUALITY_PRESETS)
                    .customUpscaleRatio(true)
                    .build();
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
                                            () -> "https://raw.githubusercontent.com/intel/xess/refs/heads/main/bin/libxess.dll",
                                            "Github"
                                    )
                                    .addRemote(
                                            () -> "https://api.fuukir.cn/dl/sr/libxess.dll",
                                            "Mirror"
                                    )
                                    .addRemote(
                                            () -> "https://cnb.cool/187J3X1-114514/mc-superresolution/-/releases/download/assets/libxess.dll",
                                            "Mirror (CNB)"
                                    )
                                    .build()
                            )
                            .build()
            )
            .supportJitter(true)
            .qualityPresets(XESS_QUALITY_PRESETS)
            .customUpscaleRatio(false)
            .build();
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
                                            NgxDlssLatestProvider.getInstance(),
                                            "NVIDIA NGX (Latest)"
                                    )
                                    .addRemote(
                                            () -> "https://raw.githubusercontent.com/NVIDIA/DLSS/refs/heads/main/lib/Windows_x86_64/rel/nvngx_dlss.dll",
                                            "Github"
                                    )
                                    .addRemote(
                                            () -> "https://api.fuukir.cn/dl/sr/nvngx_dlss.dll",
                                            "Mirror"
                                    )
                                    .addRemote(
                                            () -> "https://cnb.cool/187J3X1-114514/mc-superresolution/-/releases/download/assets/nvngx_dlss.dll",
                                            "Mirror (CNB)"
                                    )
                                    .build()
                            )
                            .build()
                            : ExtraResources.builder()
                            .add(ExtraResource.builder("libnvidia-ngx-dlss.so.310.7.0")
                                    .addRemote(
                                            () -> "https://raw.githubusercontent.com/NVIDIA/DLSS/a291cc7d2cc642a51566f3dfd5376f635cd1b284/lib/Linux_x86_64/rel/libnvidia-ngx-dlss.so.310.7.0",
                                            "Github"
                                    )
                                    .build()
                            )
                            .build()
            )
            .supportJitter(true)
            .qualityPresets(DLSS_QUALITY_PRESETS)
            .customUpscaleRatio(false)
            .build();
    public static final AlgorithmDescription<DLSSRR> DLSSRR = AlgorithmDescription.builder(DLSSRR.class)
            .briefName("NVIDIA DLSS-RR")
            .codeName("dlssrr")
            .displayName("NVIDIA DLSS Ray Reconstruction")
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
                            .add(ExtraResource.builder("nvngx_dlssd.dll")
                                    .addRemote(
                                            NgxDlssLatestProvider.getInstance(NgxDlssLatestProvider.NgxModel.DLSSD),
                                            "NVIDIA NGX (Latest)"
                                    )
                                    .addRemote(
                                            () -> "https://api.fuukir.cn/dl/sr/nvngx_dlssd.dll",
                                            "Mirror"
                                    )
                                    .addRemote(
                                            () -> "https://raw.githubusercontent.com/NVIDIA/DLSS/refs/heads/main/lib/Windows_x86_64/rel/nvngx_dlssd.dll",
                                            "Github"
                                    )
                                    .build()
                            )
                            .build()
                            : ExtraResources.builder()
                            .add(ExtraResource.builder("libnvidia-ngx-dlssd.so.310.7.0")
                                    .addRemote(
                                            () -> "https://raw.githubusercontent.com/NVIDIA/DLSS/a291cc7d2cc642a51566f3dfd5376f635cd1b284/lib/Linux_x86_64/rel/libnvidia-ngx-dlssd.so.310.7.0",
                                            "Github"
                                    )
                                    .build()
                            )
                            .build()
            )
            .supportJitter(true)
            .qualityPresets(DLSS_QUALITY_PRESETS)
            .customUpscaleRatio(false)
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

    public static void registryAlgorithms() {
        AlgorithmRegistry.registry(NONE);
        AlgorithmRegistry.registry(FSR1);
        AlgorithmRegistry.registry(FSR2);
        AlgorithmRegistry.registry(FSR);
        AlgorithmRegistry.registry(FSR4_D3D12);
        AlgorithmRegistry.registry(XESS);
        AlgorithmRegistry.registry(DLSS);
        AlgorithmRegistry.registry(DLSSRR);
        AlgorithmRegistry.registry(SGSR1);
        AlgorithmRegistry.registry(SGSR2);
        if (Platform.currentPlatform.isDevelopmentEnvironment()) {
            AlgorithmRegistry.registry(ANIME4K);
        }
        SuperResolutionAPI.EVENT_BUS.post(new AlgorithmRegisterEvent());
    }
}
