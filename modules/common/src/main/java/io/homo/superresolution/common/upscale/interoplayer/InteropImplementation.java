package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.interop.InteropResourceDescription;
import io.homo.superresolution.api.interop.InteropResourceRequirement;
import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface InteropImplementation<C extends InteropCommandContext> {
    SRRenderApiType inputApi();

    SRRenderApiType executionApi();

    void initialize(
            InitializationDescription description,
            List<InteropResourceRequirement> requirements);

    InteropResourceAccess resourceAccess();

    boolean validateInputResources(DispatchResource resource);

    Map<InteropResourceType, InteropResourceDescription> resourceDescriptions();

    boolean resourcesNeedRebuild(DispatchResource resource);

    void rebuildForDispatch(DispatchResource resource);

    InteropDispatchResource<C> beginDispatch(DispatchResource resource);

    void completeDispatch(InteropDispatchResource<C> resource, boolean success);

    void executeCommands(Consumer<C> recorder);

    ITexture outputTexture();

    void resize(int width, int height);

    void awaitUsers();

    void destroy();
}
