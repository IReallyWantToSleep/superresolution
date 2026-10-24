package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.Objects;

public record InteropOutputTexture(ITexture executionView, ITexture sourceView) {
    public InteropOutputTexture {
        Objects.requireNonNull(executionView, "executionView");
        Objects.requireNonNull(sourceView, "sourceView");
    }
}
