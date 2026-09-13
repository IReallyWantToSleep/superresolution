package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.interop.*;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static io.homo.superresolution.api.interop.InteropResourceRequirement.Presence.Optional;

/** Pure layout resolution; does not query GL or create GPU objects. */
public record InteropResourceLayout(Map<InteropResourceType, InteropResourceDescription> resources) {
    public InteropResourceLayout {
        EnumMap<InteropResourceType, InteropResourceDescription> copy = new EnumMap<>(InteropResourceType.class);
        copy.putAll(resources);
        resources = Collections.unmodifiableMap(copy);
    }

    public static InteropResourceLayout resolve(
            List<InteropResourceRequirement> requirements, InteropResourceContext context,
            InputResourceSet inputs, TextureFormat internalColorFormat,
            int renderWidth, int renderHeight, int outputWidth, int outputHeight) {
        EnumMap<InteropResourceType, InteropResourceDescription> result = new EnumMap<>(InteropResourceType.class);
        EnumSet<InteropResourceType> declared = EnumSet.noneOf(InteropResourceType.class);
        for (InteropResourceRequirement requirement : requirements) {
            InteropResourceType type = requirement.type();
            if (!declared.add(type)) {
                throw new IllegalArgumentException("Duplicate interop requirement: " + type);
            }
            ITexture source = inputs == null || !type.isInput() ? null : inputs.get(type.inputType());
            if (source == null && requirement.presence() == Optional) {
                continue;
            }
            if (source != null && source.getTextureType() != TextureType.Texture2D) {
                throw new IllegalArgumentException("Interop input must be a 2D texture: " + type);
            }
            TextureFormat format = switch (requirement.formatSource()) {
                case Fixed -> requirement.fallbackFormat();
                case Context -> context.formats().get(type);
                case SourceTexture -> source == null ? null : source.getTextureFormat();
                case InternalColorConfig -> internalColorFormat;
            };
            if (format == null) {
                format = requirement.fallbackFormat();
            }
            if (format == null) {
                throw new IllegalArgumentException("No format is available for interop resource " + type);
            }
            int width;
            int height;
            switch (requirement.sizeSource()) {
                case RenderSize -> { width = renderWidth; height = renderHeight; }
                case OutputSize -> { width = outputWidth; height = outputHeight; }
                case OneByOne -> { width = 1; height = 1; }
                case SourceSize -> {
                    if (source == null) {
                        throw new IllegalArgumentException("Source-sized interop resource is missing: " + type);
                    }
                    width = source.getWidth();
                    height = source.getHeight();
                }
                default -> throw new IllegalStateException("Unknown interop size");
            }
            if (source != null && (source.getWidth() < width || source.getHeight() < height)) {
                throw new IllegalArgumentException("Source is smaller than the interop input: " + type);
            }
            result.put(type, new InteropResourceDescription(format, width, height, requirement.usages()));
        }
        if (!result.containsKey(InteropResourceType.OutputColor)) {
            throw new IllegalArgumentException("An interop algorithm must declare OutputColor");
        }
        return new InteropResourceLayout(result);
    }
}
