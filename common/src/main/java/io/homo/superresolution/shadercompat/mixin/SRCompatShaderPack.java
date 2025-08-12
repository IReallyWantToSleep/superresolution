package io.homo.superresolution.shadercompat.mixin;

import io.homo.superresolution.shadercompat.SRShaderCompatConfig;

public interface SRCompatShaderPack {
    SRShaderCompatConfig superresolution$getSuperResolutionComaptConfig();

    boolean superresolution$isSupportsSuperResolution();
}
