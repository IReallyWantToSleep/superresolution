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

public final class VulkanToD3D12Interop implements InteropImplementation<D3D12CommandContext> {
    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(
                "Vulkan-to-D3D12 interop is not implemented yet");
    }

    @Override
    public SRRenderApiType inputApi() {
        return SRRenderApiType.VULKAN;
    }

    @Override
    public SRRenderApiType executionApi() {
        return SRRenderApiType.D3D12;
    }

    @Override
    public void initialize(InitializationDescription description,
                           List<InteropResourceRequirement> requirements) {
        throw unsupported();
    }

    @Override
    public InteropResourceAccess resourceAccess() {
        throw unsupported();
    }

    @Override
    public boolean validateInputResources(DispatchResource resource) {
        throw unsupported();
    }

    @Override
    public Map<InteropResourceType, InteropResourceDescription> resourceDescriptions() {
        return Map.of();
    }

    @Override
    public boolean resourcesNeedRebuild(DispatchResource resource) {
        throw unsupported();
    }

    @Override
    public void rebuildForDispatch(DispatchResource resource) {
        throw unsupported();
    }

    @Override
    public InteropDispatchResource<D3D12CommandContext> beginDispatch(DispatchResource resource) {
        throw unsupported();
    }

    @Override
    public void completeDispatch(InteropDispatchResource<D3D12CommandContext> resource, boolean success) {
        throw unsupported();
    }

    @Override
    public void executeCommands(Consumer<D3D12CommandContext> recorder) {
        throw unsupported();
    }

    @Override
    public ITexture outputTexture() {
        return null;
    }

    @Override
    public void resize(int width, int height) {
        throw unsupported();
    }

    @Override
    public void awaitUsers() {
    }

    @Override
    public void destroy() {
    }
}
