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

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.AlgorithmRegisterEvent;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.api.registry.ExtraResource;
import io.homo.superresolution.api.registry.ExtraResources;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.upscale.dlss.DLSS;
import io.homo.superresolution.common.upscale.ffxfsr.FfxFSR;
import io.homo.superresolution.common.upscale.ffxfsr.FfxFSROgl;
import io.homo.superresolution.common.upscale.fsr1.FSR1;
import io.homo.superresolution.common.upscale.fsr2.FSR2;
import io.homo.superresolution.common.upscale.none.None;
import io.homo.superresolution.common.upscale.sgsr.v1.Sgsr1;
import io.homo.superresolution.common.upscale.sgsr.v2.Sgsr2;
import io.homo.superresolution.api.platform.OperatingSystem;
import io.homo.superresolution.api.platform.SystemArchitecture;
import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.common.upscale.xess.XeSS;
import io.homo.superresolution.core.graphics.opengl.Gl;

public class AlgorithmDescriptions {
    public static final AlgorithmDescription<None> NONE =
            new AlgorithmDescription<>(
                    None.class,
                    "None",
                    "none",
                    "None",
                    Requirement.nothing()
            );
    public static final AlgorithmDescription<FSR1> FSR1 =
            new AlgorithmDescription<>(
                    FSR1.class,
                    "FSR1",
                    "fsr1",
                    "AMD FidelityFX Super Resolution 1",
                    Requirement.nothing()
                            .glMajorVersion(4)
                            .glMinorVersion(3)
                            .isFalse(Gl::isLegacy)
                            .isTrue(Gl::isSupportDSA)
            );
    public static final AlgorithmDescription<FSR2> FSR2 =
            new AlgorithmDescription<>(
                    FSR2.class,
                    "FSR2",
                    "fsr2",
                    "AMD FidelityFX Super Resolution 2 (OpenGL)",
                    Requirement.nothing()
                            .requiredGlExtension("GL_KHR_shader_subgroup")
                            .glMajorVersion(4)
                            .glMinorVersion(5)
                            .isFalse(Gl::isLegacy)
                            .isTrue(Gl::isSupportDSA)
            );
    public static final AlgorithmDescription<FfxFSR> FSR =
            new AlgorithmDescription<>(
                    FfxFSR.class,
                    "FSR",
                    "fsr",
                    "AMD FidelityFX Super Resolution",
                    Requirement.nothing()
                            .addSupportedOS(new OperatingSystem(SystemArchitecture.X86_64, OperatingSystemType.WINDOWS))
                            .requiredGlExtension("GL_EXT_memory_object")
                            .requiredGlExtension("GL_EXT_semaphore")
                            .glMajorVersion(4)
                            .glMinorVersion(6)
                            .requireVulkan(true)
            );
    public static final AlgorithmDescription<XeSS> XESS =
            new AlgorithmDescription<>(
                    XeSS.class,
                    "XeSS",
                    "xess",
                    "Intel Xe Super Sampling",
                    Requirement.nothing()
                            .addSupportedOS(new OperatingSystem(SystemArchitecture.X86_64, OperatingSystemType.WINDOWS))
                            .requiredGlExtension("GL_EXT_memory_object")
                            .requiredGlExtension("GL_EXT_semaphore")
                            .glMajorVersion(4)
                            .glMinorVersion(6)
                            .requireVulkan(true),
                    ExtraResources.builder()
                            .add(ExtraResource.builder("libxess.dll")
                                    .addRemote(
                                            "https://cnb.cool/187J3X1-114514/mc-superresolution/-/releases/download/assets/libxess.dll",
                                            "CNB Mirror"
                                    )
                                    .build()
                            )
                            .build()
            );
    public static final AlgorithmDescription<DLSS> DLSS =
            new AlgorithmDescription<>(
                    DLSS.class,
                    "DLSS",
                    "dlss",
                    "NVIDIA DLSS",
                    Requirement.nothing()
                            .addSupportedOS(new OperatingSystem(SystemArchitecture.X86_64, OperatingSystemType.WINDOWS))
                            .addSupportedOS(new OperatingSystem(SystemArchitecture.X86_64, OperatingSystemType.LINUX))
                            .requiredGlExtension("GL_EXT_memory_object")
                            .requiredGlExtension("GL_EXT_semaphore")
                            .glMajorVersion(4)
                            .glMinorVersion(6)
                            .requireVulkan(true),
                    Platform.currentPlatform.getOS().type == OperatingSystemType.WINDOWS ?
                    ExtraResources.builder()
                            .add(ExtraResource.builder("nvngx_dlss.dll")
                                    .addRemote(
                                            "https://cnb.cool/187J3X1-114514/mc-superresolution/-/releases/download/assets/nvngx_dlss.dll",
                                            "CNB Mirror"
                                    )
                                    .build()
                            )
                            .build() : ExtraResources.builder().build()
            );
    public static final AlgorithmDescription<Sgsr1> SGSR1 =
            new AlgorithmDescription<>(
                    Sgsr1.class,
                    "SGSR V1",
                    "sgsr1",
                    "Snapdragon™ Game Super Resolution 1",
                    Requirement.nothing()
                            .glMajorVersion(4)
                            .glMinorVersion(0)
            );
    public static final AlgorithmDescription<Sgsr2> SGSR2 =
            new AlgorithmDescription<>(
                    Sgsr2.class,
                    "SGSR V2",
                    "sgsr2",
                    "Snapdragon™ Game Super Resolution 2",
                    Requirement.nothing()
                            .glMajorVersion(4)
                            .glMinorVersion(3)
                            .isFalse(Gl::isLegacy)
                            .isTrue(Gl::isSupportDSA)
            );

    public static void registryAlgorithms() {
        AlgorithmRegistry.registry(NONE);
        AlgorithmRegistry.registry(FSR1);
        AlgorithmRegistry.registry(FSR2);
        AlgorithmRegistry.registry(FSR);
        AlgorithmRegistry.registry(XESS);
        AlgorithmRegistry.registry(DLSS);
        AlgorithmRegistry.registry(SGSR1);
        AlgorithmRegistry.registry(SGSR2);
        SuperResolutionAPI.EVENT_BUS.post(new AlgorithmRegisterEvent());
    }
}