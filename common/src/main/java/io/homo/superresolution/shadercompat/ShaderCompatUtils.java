package io.homo.superresolution.shadercompat;

import io.homo.superresolution.shadercompat.mixin.core.CompositeRendererAccessor;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.pipeline.CompositeRenderer;

public class ShaderCompatUtils {
    public static boolean isCurrentShaderPackCompatSR() {
        if (Iris.getCurrentPack().isPresent()) {
            return Iris.getCurrentPack().get().hasFeature(FeatureFlags.getValue("SR_UPSCALE"));
        }
        return false;
    }
}
