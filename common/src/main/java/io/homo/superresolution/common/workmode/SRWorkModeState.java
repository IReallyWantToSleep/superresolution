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

package io.homo.superresolution.common.workmode;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.compat.iris.IrisCompatHelper;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public record SRWorkModeState(
        InitializationDescription initializationDescription,
        TextureFormat internalTextureFormat,
        @Nullable String motionVectorPreprocessingFunction,
        boolean shaderPackInUse,
        boolean shaderPackLoading,
        boolean supportsFrameGeneration,
        List<String> disabledAlgorithms
) {
    public static SRWorkModeState defaults() {
        return new SRWorkModeState(
                InitializationDescription.defaults(),
                TextureFormat.RGBA16F,
                null,
                IrisCompatHelper.hasActiveShaderpack(),
                false,
                false,
                Collections.emptyList()
        );
    }
}
