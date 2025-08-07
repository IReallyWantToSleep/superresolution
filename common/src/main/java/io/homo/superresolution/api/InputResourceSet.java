package io.homo.superresolution.api;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public record InputResourceSet(
        ITexture colorTexture,
        ITexture depthTexture,
        ITexture motionVectorsTexture
) {

}
