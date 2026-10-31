/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.api.interop.InteropResourceDescription;
import io.homo.superresolution.api.interop.InteropResourceRequirement;
import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesPreprocessor;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.d3d12.*;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.MemoryBarrierType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static io.homo.superresolution.api.interop.InteropResourceType.*;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;

public final class OpenGLToD3D12Interop implements InteropImplementation<D3D12CommandContext> {
    private List<InteropResourceRequirement> requirements = List.of();
    private InteropResourceLayout builtLayout;
    private Resources resources;
    private InteropResourceAccess resourceAccess;
    private PendingDispatch pendingDispatch;
    private boolean initialized;
    private boolean destroyed;

    @Override
    public SRRenderApiType inputApi() {
        return SRRenderApiType.OPENGL;
    }

    @Override
    public SRRenderApiType executionApi() {
        return SRRenderApiType.D3D12;
    }

    @Override
    public void initialize(InitializationDescription description,
                           List<InteropResourceRequirement> requirements) {
        Objects.requireNonNull(description, "description");
        if (initialized || resources != null) {
            throw new IllegalStateException("OpenGL/D3D12 interop still owns resources");
        }
        if (!NativeLibManager.d3d12Available()) {
            throw new IllegalStateException("The optional D3D12 native library is unavailable");
        }
        this.requirements = List.copyOf(requirements);
        destroyed = false;
        createResources(resolveLayout(null,
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(),
                RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()));
        initialized = true;
    }

    private InteropResourceLayout resolveLayout(InputResourceSet inputs,
                                                int renderWidth, int renderHeight,
                                                int screenWidth, int screenHeight) {
        return InteropResourceLayout.resolve(requirements,
                InteropResourceContextManager.getCurrentContext(), inputs,
                SuperResolutionConfig.getInternalTextureFormat(),
                renderWidth, renderHeight, screenWidth, screenHeight);
    }

    private InteropResourceLayout resolveLayout(DispatchResource resource) {
        return resolveLayout(resource.resources(), resource.renderWidth(), resource.renderHeight(),
                resource.screenWidth(), resource.screenHeight());
    }

    @Override
    public InteropResourceAccess resourceAccess() {
        return Objects.requireNonNull(resourceAccess, "Interop resources are not initialized");
    }

    @Override
    public boolean validateInputResources(DispatchResource resource) {
        return resource != null
                && InteropResourceLayout.hasRequiredInputs(requirements, resource.resources());
    }

    @Override
    public Map<InteropResourceType, InteropResourceDescription> resourceDescriptions() {
        return builtLayout == null ? Map.of() : builtLayout.resources();
    }

    @Override
    public boolean resourcesNeedRebuild(DispatchResource resource) {
        return resources == null || resources.unrecoverable || !resolveLayout(resource).equals(builtLayout);
    }

    @Override
    public void rebuildForDispatch(DispatchResource resource) {
        rebuildResources(resolveLayout(resource));
    }

    @Override
    public InteropDispatchResource<D3D12CommandContext> beginDispatch(DispatchResource resource) {
        requireReady();
        if (pendingDispatch != null) {
            throw new IllegalStateException("A D3D12 dispatch is already active");
        }
        if (!resolveLayout(resource).equals(builtLayout)) {
            throw new IllegalStateException("D3D12 interop resources require rebuild before dispatch");
        }
        InteropFrameData frame = InteropFrameData.from(resource);
        InteropResourceSet executionResources = new InteropResourceSet(resources.executionTextures);
        processInputs(resource.resources());
        long openGlReady = resources.fence.reserveValue();
        long d3d12Done = resources.fence.reserveValue();
        boolean released = false;
        D3D12CommandContext context = null;
        try {
            resources.signalOpenGl(openGlReady);
            released = true;
            resources.assumeOpenGlHandoffStates();
            D3D12CommandBuffer commandBuffer = resources.beginCommandBuffer();
            context = new D3D12CommandContext(commandBuffer);
            pendingDispatch = new PendingDispatch(resources, context, openGlReady, d3d12Done);
            return new InteropDispatchResource<>(frame, executionResources, context);
        } catch (RuntimeException | Error failure) {
            if (context != null) {
                try {
                    context.close();
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            if (released) {
                recoverUnsubmitted(resources, openGlReady, d3d12Done, failure);
            } else {
                resources.unrecoverable = true;
            }
            throw failure;
        }
    }

    private void processInputs(InputResourceSet inputs) {
        InteropResourcesPreprocessor.processInputTextures(
                inputs.get(InputResourceType.Color), resources.gl(Color),
                inputs.get(InputResourceType.Depth), resources.gl(Depth),
                inputs.get(InputResourceType.MotionVectors), resources.gl(MotionVectors),
                inputs.get(InputResourceType.Exposure), resources.gl(Exposure),
                SRWorkModeManager.getCurrentState().motionVectorPreprocessingFunction());
        ICommandBuffer supplemental = null;
        try {
            for (InteropResourceType type : resources.executionTextures.keySet()) {
                if (!type.isInput() || type == Color || type == Depth
                        || type == MotionVectors || type == Exposure) {
                    continue;
                }
                if (supplemental == null) {
                    supplemental = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
                    supplemental.begin();
                }
                if (type == SpecularMotionVectors) {
                    InteropResourcesPreprocessor.flipMotionVectorY(
                            supplemental, inputs.get(type.inputType()), resources.gl(type));
                } else {
                    InteropResourcesPreprocessor.flipY(
                            supplemental, inputs.get(type.inputType()), resources.gl(type));
                }
            }
            if (supplemental != null) {
                supplemental.end();
                RenderSystems.opengl().device().submitCommandBuffer(supplemental);
            }
        } finally {
            if (supplemental != null) {
                supplemental.destroy();
            }
        }
    }

    @Override
    public void completeDispatch(InteropDispatchResource<D3D12CommandContext> resource, boolean success) {
        PendingDispatch pending = pendingDispatch;
        if (pending == null || pending.context() != resource.commandContext()) {
            throw new IllegalStateException("D3D12 dispatch context mismatch");
        }
        pendingDispatch = null;
        Resources owner = pending.resources();
        D3D12CommandBuffer commandBuffer = pending.context().commandBuffer();
        if (!success) {
            try {
                pending.context().close();
            } catch (RuntimeException | Error failure) {
                recoverUnsubmitted(owner, pending.openGlReady(), pending.d3d12Done(), failure);
                throw failure;
            }
            recoverUnsubmitted(owner, pending.openGlReady(), pending.d3d12Done(), null);
            return;
        }
        boolean submitAttempted = false;
        try {
            for (Map.Entry<InteropResourceType, D3D12Texture2D> entry : owner.executionTextures.entrySet()) {
                pending.context().setTextureState(entry.getValue(),
                        entry.getKey().isInput() ? D3D12ResourceState.COMPUTE_READ : D3D12ResourceState.COMMON);
            }
            pending.context().close();
            commandBuffer.end();
            submitAttempted = true;
            owner.queue.submit(commandBuffer, owner.fence,
                    pending.openGlReady(), pending.d3d12Done());
            owner.waitForOpenGl(pending.d3d12Done());
            owner.publishOutput();
        } catch (RuntimeException | Error failure) {
            try {
                pending.context().close();
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            if (!submitAttempted) {
                recoverUnsubmitted(owner, pending.openGlReady(), pending.d3d12Done(), failure);
            } else {
                try {
                    owner.device.waitIdle();
                    if (!owner.fence.isComplete(pending.d3d12Done())) {
                        owner.unrecoverable = true;
                        throw new IllegalStateException("D3D12 shared-fence recovery did not complete");
                    }
                    owner.waitForOpenGl(pending.d3d12Done());
                    owner.replaceCommandBuffer();
                } catch (RuntimeException | Error cleanupFailure) {
                    owner.unrecoverable = true;
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    private static void recoverUnsubmitted(Resources owner, long openGlReady, long d3d12Done,
                                           Throwable originalFailure) {
        try {
            owner.queue.recoverSharedFence(owner.fence, openGlReady, d3d12Done);
            owner.waitForOpenGl(d3d12Done);
            owner.replaceCommandBuffer();
        } catch (RuntimeException | Error recoveryFailure) {
            owner.unrecoverable = true;
            if (originalFailure == null) {
                throw recoveryFailure;
            }
            originalFailure.addSuppressed(recoveryFailure);
        }
    }

    @Override
    public ITexture outputTexture() {
        return resources == null ? null : resources.sourceOutput();
    }

    @Override
    public void executeCommands(Consumer<D3D12CommandContext> recorder) {
        Objects.requireNonNull(recorder, "recorder");
        if (resources == null || resources.unrecoverable || pendingDispatch != null) {
            throw new IllegalStateException("D3D12 interop resources are unavailable for setup recording");
        }
        D3D12CommandBuffer commandBuffer = resources.commandPool.createCommandBuffer(
                CommandBufferBehavior.ReusableSequential);
        D3D12CommandContext context = new D3D12CommandContext(commandBuffer);
        try {
            commandBuffer.begin();
            recorder.accept(context);
            context.close();
            commandBuffer.end();
            resources.queue.submit(commandBuffer);
            commandBuffer.waitForFence();
        } finally {
            context.close();
            resources.device.waitIdle();
            commandBuffer.destroy();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (!initialized || destroyed || resources == null) {
            throw new IllegalStateException("OpenGL/D3D12 interop is not initialized");
        }
        InteropResourceLayout layout = resolveLayout(null,
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), width, height);
        if (resources.unrecoverable || !layout.equals(builtLayout)) {
            rebuildResources(layout);
        }
    }

    private void rebuildResources(InteropResourceLayout layout) {
        awaitUsers();
        destroyResources();
        createResources(layout);
    }

    private void createResources(InteropResourceLayout layout) {
        resources = new Resources(layout);
        resources.initialize();
        builtLayout = layout;
        resourceAccess = new InteropResourceAccess(inputApi(), executionApi(),
                resources.executionTextures, layout.resources(), resources.sourceOutput(), resources.device);
    }

    private void requireReady() {
        if (!initialized || destroyed || resources == null || resources.unrecoverable) {
            throw new IllegalStateException("OpenGL/D3D12 interop is not available");
        }
    }

    @Override
    public void awaitUsers() {
        if (pendingDispatch != null) {
            throw new IllegalStateException("Cannot retire resources during an active D3D12 dispatch");
        }
        if (resources != null) {
            RenderSystems.opengl().finish();
            if (resources.device != null) {
                resources.device.waitIdle();
            }
        }
    }

    private void destroyResources() {
        if (resources != null) {
            resources.destroy();
            resources = null;
        }
        resourceAccess = null;
        builtLayout = null;
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        awaitUsers();
        destroyResources();
        initialized = false;
        destroyed = true;
    }

    private record PendingDispatch(Resources resources, D3D12CommandContext context,
                                   long openGlReady, long d3d12Done) {
    }

    private static final class Resources {
        private final InteropResourceLayout layout;
        private final EnumMap<InteropResourceType, D3D12Texture2D> executionTextures =
                new EnumMap<>(InteropResourceType.class);
        private final EnumMap<InteropResourceType, GlD3D12ImportableTexture2D> glTextures =
                new EnumMap<>(InteropResourceType.class);
        private final GlTexture2D[] sourceOutputs = new GlTexture2D[2];
        private D3D12Device device;
        private D3D12Queue queue;
        private D3D12Device.ExternalBorrowLease deviceBorrow;
        private D3D12Fence fence;
        private D3D12CommandPool commandPool;
        private D3D12CommandBuffer commandBuffer;
        private D3D12InteropSemaphore semaphore;
        private int[] sharedTextureIds;
        private int[] handoffLayouts;
        private int publishedSource;
        private boolean unrecoverable;

        private Resources(InteropResourceLayout layout) {
            this.layout = layout;
        }

        private void initialize() {
            D3D12OpenGlInterop.requireExtensions();
            device = RenderSystems.d3d12().device();
            queue = device.directQueue();
            deviceBorrow = device.borrowExternal();
            fence = device.createFence(0);
            commandPool = device.createCommandPool(CommandPoolFlags.Reset);
            commandBuffer = commandPool.createCommandBuffer(CommandBufferBehavior.ReusableSequential);
            semaphore = new D3D12InteropSemaphore(fence);
            semaphore.initializeImport();
            for (Map.Entry<InteropResourceType, InteropResourceDescription> entry : layout.resources().entrySet()) {
                InteropResourceDescription description = entry.getValue();
                TextureUsages usages = TextureUsages.create();
                usages.getUsages().addAll(description.usages());
                D3D12Texture2D texture = device.createSharedTexture2D(
                        TextureDescription.create().type(TextureType.Texture2D)
                                .width(description.width()).height(description.height())
                                .format(description.format()).usages(usages)
                                .label("D3D12Interop-" + entry.getKey()).build(),
                        D3D12ResourceState.COMMON);
                executionTextures.put(entry.getKey(), texture);
                GlD3D12ImportableTexture2D glTexture = new GlD3D12ImportableTexture2D(texture);
                glTextures.put(entry.getKey(), glTexture);
                glTexture.initializeImport();
            }
            InteropResourceDescription output = layout.resources().get(OutputColor);
            for (int i = 0; i < sourceOutputs.length; i++) {
                OwnedGlTexture2D source = new OwnedGlTexture2D(TextureDescription.create()
                        .type(TextureType.Texture2D).usages(TextureUsages.create().sampler().storage())
                        .format(output.format()).width(output.width()).height(output.height())
                        .label("D3D12Interop-SourceOutput-" + i).build());
                sourceOutputs[i] = source;
                source.initializeOwned();
            }
            sharedTextureIds = new int[glTextures.size()];
            handoffLayouts = new int[glTextures.size()];
            int index = 0;
            for (Map.Entry<InteropResourceType, GlD3D12ImportableTexture2D> entry : glTextures.entrySet()) {
                sharedTextureIds[index] = Math.toIntExact(entry.getValue().handle());
                handoffLayouts[index] = entry.getKey().isInput()
                        ? GL_LAYOUT_SHADER_READ_ONLY_EXT : GL_LAYOUT_GENERAL_EXT;
                index++;
            }
        }

        private GlD3D12ImportableTexture2D gl(InteropResourceType type) {
            return glTextures.get(type);
        }

        private ITexture sourceOutput() {
            return sourceOutputs[publishedSource];
        }

        private void publishOutput() {
            int nextSource = 1 - publishedSource;
            ICommandBuffer copy = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
            try {
                copy.begin();
                InteropResourcesPreprocessor.flipY(copy, gl(OutputColor), sourceOutputs[nextSource]);
                copy.memoryBarrier(MemoryBarrierType.TEXTURE_FETCH, MemoryBarrierType.STORAGE_IMAGE_WRITE);
                copy.end();
                RenderSystems.opengl().device().submitCommandBuffer(copy);
                publishedSource = nextSource;
            } finally {
                copy.destroy();
            }
        }

        private D3D12CommandBuffer beginCommandBuffer() {
            commandBuffer.waitForFence();
            commandBuffer.reset();
            commandBuffer.begin();
            return commandBuffer;
        }

        private void replaceCommandBuffer() {
            device.waitIdle();
            if (commandBuffer != null) {
                commandBuffer.destroy();
                commandBuffer = null;
            }
            commandBuffer = commandPool.createCommandBuffer(CommandBufferBehavior.ReusableSequential);
        }

        private void signalOpenGl(long value) {
            semaphore.signal(value, sharedTextureIds, handoffLayouts);
        }

        private void waitForOpenGl(long value) {
            semaphore.waitFor(value, sharedTextureIds, handoffLayouts);
        }

        private void assumeOpenGlHandoffStates() {
            executionTextures.forEach((type, texture) -> texture.assumeCommittedState(
                    type.isInput() ? D3D12ResourceState.COMPUTE_READ : D3D12ResourceState.COMMON));
        }

        private void destroy() {
            for (int i = 0; i < sourceOutputs.length; i++) {
                if (sourceOutputs[i] != null) {
                    sourceOutputs[i].destroy();
                    sourceOutputs[i] = null;
                }
            }
            for (InteropResourceType type : List.copyOf(glTextures.keySet())) {
                glTextures.get(type).destroy();
                glTextures.remove(type);
            }
            for (InteropResourceType type : List.copyOf(executionTextures.keySet())) {
                executionTextures.get(type).destroy();
                executionTextures.remove(type);
            }
            if (semaphore != null) {
                semaphore.close();
                semaphore = null;
            }
            if (commandBuffer != null) {
                commandBuffer.destroy();
                commandBuffer = null;
            }
            if (commandPool != null) {
                commandPool.destroy();
                commandPool = null;
            }
            if (fence != null) {
                fence.destroy();
                fence = null;
            }
            if (deviceBorrow != null) {
                deviceBorrow.close();
                deviceBorrow = null;
            }
            device = null;
            queue = null;
        }
    }

    private static final class OwnedGlTexture2D extends GlTexture2D {
        private OwnedGlTexture2D(TextureDescription description) {
            super(description);
        }

        private void initializeOwned() {
            configureMipmap();
            initializeTexture();
        }
    }
}
