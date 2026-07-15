package io.homo.superresolution.common.workmode;

import io.homo.superresolution.api.InitializationDescription;
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
        boolean onlySupportsFrameGeneration,
        List<String> disabledAlgorithms
) {
    public static SRWorkModeState defaults() {
        return new SRWorkModeState(
                InitializationDescription.defaults(),
                TextureFormat.RGBA16F,
                null,
                false,
                false,
                false,
                Collections.emptyList()
        );
    }
}
