package io.homo.superresolution.common.workmode;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import org.jetbrains.annotations.Nullable;

public record SRWorkModeState(
        InitializationDescription initializationDescription,
        TextureFormat internalTextureFormat,
        @Nullable String motionVectorPreprocessingFunction,
        boolean shaderPackInUse,
        boolean shaderPackLoading
) {
    public static SRWorkModeState defaults() {
        return new SRWorkModeState(
                InitializationDescription.defaults(),
                TextureFormat.RGBA16F,
                null,
                false,
                false
        );
    }
}
