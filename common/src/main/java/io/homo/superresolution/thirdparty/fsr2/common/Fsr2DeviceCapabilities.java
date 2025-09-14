/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;

public class Fsr2DeviceCapabilities {
    private static boolean initialized = false;
    private static boolean fp16Supported = false;

    private static boolean detectFp16Support() {
        return GraphicsCapabilities.hasGLExtension("GL_NV_gpu_shader5") ||
                GraphicsCapabilities.hasGLExtension("GL_AMD_gpu_shader_half_float");
    }

    public static boolean isFp16Supported() {
        if (!initialized) {
            fp16Supported = detectFp16Support();
            initialized = true;
        }
        return fp16Supported && SuperResolutionConfig.SPECIAL.FSR2.FP16.get();
    }
}
