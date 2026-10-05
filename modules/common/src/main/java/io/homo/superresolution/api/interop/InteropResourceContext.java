package io.homo.superresolution.api.interop;

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable format facts for the active work mode. Never owns or retains textures. */
public record InteropResourceContext(Map<InteropResourceType, TextureFormat> formats) {
    private static final InteropResourceContext EMPTY = new InteropResourceContext(Map.of());

    public InteropResourceContext {
        EnumMap<InteropResourceType, TextureFormat> copy = new EnumMap<>(InteropResourceType.class);
        formats.forEach((type, format) -> copy.put(
                Objects.requireNonNull(type), Objects.requireNonNull(format)));
        formats = Collections.unmodifiableMap(copy);
    }

    public static InteropResourceContext empty() {
        return EMPTY;
    }

    /** Capture the final algorithm inputs, not necessarily the original render attachments. */
    public static InteropResourceContext fromInputs(InputResourceSet inputs) {
        EnumMap<InteropResourceType, TextureFormat> formats = new EnumMap<>(InteropResourceType.class);
        if (inputs != null) {
            for (InteropResourceType type : InteropResourceType.values()) {
                // Iris depth format metadata is not authoritative.
                if (!type.isInput() || type == InteropResourceType.Depth) {
                    continue;
                }
                ITexture texture = inputs.get(type.inputType());
                if (texture != null) {
                    formats.put(type, texture.getTextureFormat());
                }
            }
        }
        return new InteropResourceContext(formats);
    }
}
