package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class InteropResourceSet {
    private final EnumMap<InteropResourceType, ITexture> textures;

    public InteropResourceSet(Map<InteropResourceType, ? extends ITexture> textures) {
        this.textures = new EnumMap<>(InteropResourceType.class);
        textures.forEach((type, texture) -> this.textures.put(
                Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(texture, "texture")));
    }

    public ITexture get(InteropResourceType type) {
        return textures.get(type);
    }

    public boolean has(InteropResourceType type) {
        return textures.containsKey(type);
    }

    public Set<InteropResourceType> types() {
        return Collections.unmodifiableSet(textures.keySet());
    }

    public Map<InteropResourceType, ITexture> asMap() {
        return Collections.unmodifiableMap(textures);
    }

    public ITexture output() {
        return get(InteropResourceType.OutputColor);
    }
}
