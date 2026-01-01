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

package io.homo.superresolution.common.upscale.nis.struct;

import io.homo.superresolution.common.upscale.nis.enums.NISGPUArchitecture;

public class NISOptimizer {
    private boolean isUpscaling;
    private NISGPUArchitecture gpuArch;
    public int getOptimalBlockWidth() {
        switch (gpuArch) {
            case NVIDIA_Generic:
            case NVIDIA_Generic_fp16:
            case AMD_Generic:
            case Intel_Generic:
                return 32;
            default:
                return 32;
        }
    }

    public int getOptimalBlockHeight() {
        switch (gpuArch) {
            case NVIDIA_Generic:
                return isUpscaling ? 24 : 32;
            case NVIDIA_Generic_fp16:
                return 32;
            case AMD_Generic:
                return isUpscaling ? 24 : 32;
            case Intel_Generic:
                return isUpscaling ? 24 : 32;
            default:
                return isUpscaling ? 24 : 32;
        }
    }

    public int getOptimalThreadGroupSize() {
        switch (gpuArch) {
            case NVIDIA_Generic:
            case NVIDIA_Generic_fp16:
                return 128;
            case AMD_Generic:
            case Intel_Generic:
                return 256;
            default:
                return 256;
        }
    }
}
