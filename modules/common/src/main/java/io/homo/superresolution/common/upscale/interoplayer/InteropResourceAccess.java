package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.interop.InteropResourceDescription;
import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class InteropResourceAccess {
    private final SRRenderApiType inputApi;
    private final SRRenderApiType executionApi;
    private final EnumMap<InteropResourceType, ITexture> executionTextures;
    private final Map<InteropResourceType, InteropResourceDescription> descriptions;
    private final ITexture sourceOutput;
    private final Object device;
    private final InteropResourceSet resources;

    public InteropResourceAccess(
            SRRenderApiType inputApi,
            SRRenderApiType executionApi,
            Map<InteropResourceType, ? extends ITexture> executionTextures,
            Map<InteropResourceType, InteropResourceDescription> descriptions,
            ITexture sourceOutput,
            Object device) {
        this.inputApi = inputApi;
        this.executionApi = executionApi;
        this.executionTextures = new EnumMap<>(InteropResourceType.class);
        this.executionTextures.putAll(executionTextures);
        EnumMap<InteropResourceType, InteropResourceDescription> descriptionCopy =
                new EnumMap<>(InteropResourceType.class);
        descriptionCopy.putAll(descriptions);
        this.descriptions = Collections.unmodifiableMap(descriptionCopy);
        this.sourceOutput = sourceOutput;
        this.device = device;
        this.resources = new InteropResourceSet(this.executionTextures);
    }

    public SRRenderApiType inputApi() { return inputApi; }
    public SRRenderApiType executionApi() { return executionApi; }
    public ITexture executionTexture(InteropResourceType type) { return executionTextures.get(type); }
    public ITexture outputTexture() { return executionTextures.get(InteropResourceType.OutputColor); }
    public ITexture sourceOutputTexture() { return sourceOutput; }
    public Object device() { return device; }
    public InteropResourceSet resources() { return resources; }
    public Map<InteropResourceType, InteropResourceDescription> descriptions() { return descriptions; }
}
