package io.homo.superresolution.api.interop;

import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsage;

import java.util.Objects;
import java.util.Set;

/** One algorithm-owned input or output allocation. A null fallback requires a known source format. */
public record InteropResourceRequirement(
        InteropResourceType type,
        Presence presence,
        SizeSource sizeSource,
        FormatSource formatSource,
        TextureFormat fallbackFormat,
        Set<TextureUsage> usages
) {
    public enum Presence { Required, Optional }
    public enum SizeSource { RenderSize, OutputSize, OneByOne, SourceSize }
    public enum FormatSource { Fixed, Context, SourceTexture, InternalColorConfig, InternalColorConfigOrContext }

    public InteropResourceRequirement {
        Objects.requireNonNull(type);
        Objects.requireNonNull(presence);
        Objects.requireNonNull(sizeSource);
        Objects.requireNonNull(formatSource);
        usages = Set.copyOf(usages);
        if (formatSource == FormatSource.Fixed && fallbackFormat == null) {
            throw new IllegalArgumentException("A fixed resource needs a format: " + type);
        }
        if (type == InteropResourceType.Depth
                && (formatSource != FormatSource.Fixed || fallbackFormat != TextureFormat.R32F)) {
            throw new IllegalArgumentException("Interop depth must use fixed R32F");
        }
        if (!type.isInput() && (presence != Presence.Required
                || sizeSource == SizeSource.SourceSize || formatSource == FormatSource.SourceTexture)) {
            throw new IllegalArgumentException("Outputs cannot depend on an input texture: " + type);
        }
    }

    public static InteropResourceRequirement input(
            InteropResourceType type, Presence presence, SizeSource size,
            FormatSource format, TextureFormat fallback) {
        if (!type.isInput()) {
            throw new IllegalArgumentException("Not an input: " + type);
        }
        return new InteropResourceRequirement(type, presence, size, format, fallback,
                Set.of(TextureUsage.Sampler, TextureUsage.Storage,
                        TextureUsage.TransferSource, TextureUsage.TransferDestination));
    }

    public static InteropResourceRequirement output(
            InteropResourceType type, SizeSource size, FormatSource format, TextureFormat fallback) {
        if (type.isInput()) {
            throw new IllegalArgumentException("Not an output: " + type);
        }
        return new InteropResourceRequirement(type, Presence.Required, size, format, fallback,
                Set.of(TextureUsage.Sampler, TextureUsage.Storage, TextureUsage.TransferDestination));
    }
}
