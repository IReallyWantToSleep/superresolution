package io.homo.superresolution.api.interop;

import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsage;

import java.util.Objects;
import java.util.Set;

/** Resolved allocation metadata. Equality deliberately excludes source texture handles. */
public record InteropResourceDescription(TextureFormat format, int width, int height,
                                         Set<TextureUsage> usages) {
    public InteropResourceDescription {
        Objects.requireNonNull(format);
        usages = Set.copyOf(usages);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Interop resource dimensions must be positive");
        }
        if (format.isDepth() || format.isInteger() || format.getGlslFormatQualifier() == null) {
            throw new IllegalArgumentException("Unsupported interop compute texture format: " + format);
        }
        if (!usages.contains(TextureUsage.Sampler) || !usages.contains(TextureUsage.Storage)) {
            throw new IllegalArgumentException("Interop textures need sampler and storage usage");
        }
    }
}
