package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.Objects;

public final class InteropDispatchResource<C extends InteropCommandContext> {
    private final InteropFrameData frameData;
    private final InteropResourceSet resources;
    private final C commandContext;

    public InteropDispatchResource(
            InteropFrameData frameData,
            InteropResourceSet resources,
            C commandContext) {
        this.frameData = Objects.requireNonNull(frameData, "frameData");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.commandContext = Objects.requireNonNull(commandContext, "commandContext");
    }

    public InteropFrameData frameData() {
        return frameData;
    }

    public InteropResourceSet resources() {
        return resources;
    }

    public boolean has(InteropResourceType type) {
        return resources.has(type);
    }

    public ITexture texture(InteropResourceType type) {
        return resources.get(type);
    }

    public ITexture outputTexture() {
        return resources.output();
    }

    public C commandContext() {
        return commandContext;
    }
}
