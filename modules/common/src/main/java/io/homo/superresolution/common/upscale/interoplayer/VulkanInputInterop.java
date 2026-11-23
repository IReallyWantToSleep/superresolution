package io.homo.superresolution.common.upscale.interoplayer;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.interop.InteropResourceDescription;
import io.homo.superresolution.api.interop.InteropResourceRequirement;
import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesPreprocessor;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.vulkan.*;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.homo.superresolution.api.interop.InteropResourceType.*;
import static org.lwjgl.vulkan.VK10.*;

/** Common ownership for implementations which consume Minecraft Vulkan images. */
abstract class VulkanInputInterop<C extends InteropCommandContext> implements InteropImplementation<C> {
    protected final EnumMap<InteropResourceType, ITexture> executionTextures =
            new EnumMap<>(InteropResourceType.class);
    protected final EnumMap<InteropResourceType, ITexture> vulkanTextures =
            new EnumMap<>(InteropResourceType.class);
    protected final VulkanTexture[] sourceOutputs = new VulkanTexture[2];
    private List<InteropResourceRequirement> requirements = List.of();
    protected VulkanDevice vulkanDevice;
    protected InteropResourceLayout builtLayout;
    private InteropResourceAccess resourceAccess;
    protected boolean unusable;
    private boolean initialized;
    private boolean destroyed;
    private int publishedSource = -1;

    @Override
    public final SRRenderApiType inputApi() {
        return SRRenderApiType.VULKAN;
    }

    protected abstract void initializeExecution();
    protected abstract ITexture allocateExecution(InteropResourceType type, TextureDescription description);
    protected abstract Object executionDevice();
    protected abstract void destroyExecution();
    protected abstract boolean hasPendingDispatch();

    protected void awaitExecution() {
    }

    /**
     * Whether the provider's output image itself is published as the algorithm output, instead
     * of a copy in {@link #sourceOutputs}. That is possible when the provider runs on Minecraft's
     * queue: everything that reads the published output is recorded after the dispatch and
     * before the next one, in queue order, so nothing still reads it when the next dispatch
     * writes it. That saves a full-resolution copy per frame and the two copy images.
     */
    protected boolean publishesExecutionOutput() {
        return false;
    }

    @Override
    public final void initialize(InitializationDescription description,
                                 List<InteropResourceRequirement> requirements) {
        Objects.requireNonNull(description, "description");
        if (initialized || vulkanDevice != null) {
            throw new IllegalStateException("Vulkan interop still owns resources");
        }
        this.requirements = List.copyOf(requirements);
        destroyed = false;
        createResources(resolveLayout(null,
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(),
                RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()));
        initialized = true;
    }

    private InteropResourceLayout resolveLayout(InputResourceSet inputs, int renderWidth, int renderHeight,
                                                int width, int height) {
        return InteropResourceLayout.resolve(requirements,
                InteropResourceContextManager.getCurrentContext(), inputs,
                SuperResolutionConfig.getInternalTextureFormat(), renderWidth, renderHeight, width, height);
    }

    protected final InteropResourceLayout resolveLayout(DispatchResource resource) {
        return resolveLayout(resource.resources(), resource.renderWidth(), resource.renderHeight(),
                resource.screenWidth(), resource.screenHeight());
    }

    @Override
    public final InteropResourceAccess resourceAccess() {
        return Objects.requireNonNull(resourceAccess, "Vulkan interop is not initialized");
    }

    @Override
    public final boolean validateInputResources(DispatchResource resource) {
        if (resource == null || !InteropResourceLayout.hasRequiredInputs(requirements, resource.resources())) {
            return false;
        }
        for (InteropResourceRequirement requirement : requirements) {
            if (!requirement.type().isInput()) {
                continue;
            }
            ITexture texture = resource.resources().get(requirement.type().inputType());
            if (texture != null && !(texture instanceof VulkanTexture)
                    && !(texture instanceof VulkanExternalTexture)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final Map<InteropResourceType, InteropResourceDescription> resourceDescriptions() {
        return builtLayout == null ? Map.of() : builtLayout.resources();
    }

    @Override
    public final boolean resourcesNeedRebuild(DispatchResource resource) {
        return unusable || builtLayout == null || !resolveLayout(resource).equals(builtLayout);
    }

    @Override
    public final void rebuildForDispatch(DispatchResource resource) {
        rebuildResources(resolveLayout(resource));
    }

    @Override
    public final void resize(int width, int height) {
        if (!initialized || destroyed || vulkanDevice == null) {
            throw new IllegalStateException("Vulkan interop is not initialized");
        }
        InteropResourceLayout layout = resolveLayout(null,
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), width, height);
        if (unusable || !layout.equals(builtLayout)) {
            rebuildResources(layout);
        }
    }

    private void rebuildResources(InteropResourceLayout layout) {
        awaitUsers();
        destroyResources();
        createResources(layout);
    }

    private void createResources(InteropResourceLayout layout) {
        vulkanDevice = RenderSystems.vulkan().device();
        if (vulkanDevice.ownsVkDevice()) {
            throw new IllegalStateException("Vulkan input requires the borrowed Minecraft device");
        }
        initializeExecution();
        for (Map.Entry<InteropResourceType, InteropResourceDescription> entry : layout.resources().entrySet()) {
            InteropResourceDescription description = entry.getValue();
            TextureUsages usages = TextureUsages.create();
            usages.getUsages().addAll(description.usages());
            if (entry.getKey() == OutputColor && publishesExecutionOutput()) {
                // Readers copy or blit the published output, which needs transfer-source usage.
                usages.transferSource();
            }
            ITexture texture = allocateExecution(entry.getKey(), TextureDescription.create()
                    .type(TextureType.Texture2D).width(description.width()).height(description.height())
                    .format(description.format()).usages(usages)
                    .label("VulkanInterop-" + entry.getKey()).build());
            executionTextures.put(entry.getKey(), texture);
        }
        InteropResourceDescription output = layout.resources().get(OutputColor);
        if (!publishesExecutionOutput()) {
            for (int i = 0; i < sourceOutputs.length; i++) {
                sourceOutputs[i] = (VulkanTexture) vulkanDevice.createTexture(TextureDescription.create()
                        .type(TextureType.Texture2D).width(output.width()).height(output.height())
                        .format(output.format()).usages(TextureUsages.create().sampler().storage()
                                .transferSource().transferDestination())
                        .label("VulkanInterop-SourceOutput-" + i).build());
            }
        }
        builtLayout = layout;
        publishedSource = -1;
        unusable = false;
        resourceAccess = new InteropResourceAccess(inputApi(), executionApi(), executionTextures,
                layout.resources(),
                publishesExecutionOutput() ? executionTextures.get(OutputColor) : sourceOutputs[0],
                executionDevice());
    }

    protected final void requireReady() {
        if (!initialized || destroyed || vulkanDevice == null || builtLayout == null || unusable) {
            throw new IllegalStateException("Vulkan interop resources are unavailable");
        }
    }

    protected final VulkanCommandBuffer getCurrentCommandBuffer() {
        #if MC_VER >= MC_26_3
        return new B3DVulkanCommandBuffer(RenderSystem.getDevice().createCommandEncoder());
        #else
        throw new UnsupportedOperationException("Minecraft Vulkan recording requires Minecraft 26.3");
        #endif
    }

    protected final void rememberExecution(VulkanInteropState states) {
        vulkanTextures.values().forEach(states::remember);
        for (VulkanTexture texture : sourceOutputs) {
            if (texture != null) {
                states.remember(texture);
            }
        }
    }

    protected final void prepareInputs(VulkanCommandBuffer commands, VulkanInteropState states,
                                       InputResourceSet inputs) {
        EnumMap<InteropResourceType, ITexture> borrowed = new EnumMap<>(InteropResourceType.class);
        for (InteropResourceType type : vulkanTextures.keySet()) {
            if (type.isInput()) {
                ITexture source = inputs.get(type.inputType());
                if (source != null) {
                    // Cropped/processed inputs are SR-owned images with real tracked
                    // layouts. A new GENERAL alias would discard their prior writes.
                    states.transition(commands, source, VK_IMAGE_LAYOUT_GENERAL,
                            VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT,
                            VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
                    borrowed.put(type, source);
                }
            }
        }
        InteropResourcesPreprocessor.processInputTextures(commands,
                borrowed.get(Color), vulkanTextures.get(Color),
                borrowed.get(Depth), vulkanTextures.get(Depth),
                borrowed.get(MotionVectors), vulkanTextures.get(MotionVectors),
                borrowed.get(Exposure), vulkanTextures.get(Exposure),
                SRWorkModeManager.getCurrentState().motionVectorPreprocessingFunction(), false);
        for (Map.Entry<InteropResourceType, ITexture> entry : borrowed.entrySet()) {
            InteropResourceType type = entry.getKey();
            if (type != Color && type != Depth && type != MotionVectors && type != Exposure) {
                InteropResourcesPreprocessor.copyTexture(commands, entry.getValue(), vulkanTextures.get(type));
            }
        }
        for (ITexture raw : borrowed.values()) {
            states.transition(commands, raw, VK_IMAGE_LAYOUT_GENERAL,
                    VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT,
                    VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
        }
    }

    protected final int recordOutput(VulkanCommandBuffer commands, VulkanInteropState states) {
        int next = publishedSource < 0 ? 0 : 1 - publishedSource;
        InteropResourcesPreprocessor.copyTexture(commands, vulkanTextures.get(OutputColor), sourceOutputs[next]);
        states.transition(commands, sourceOutputs[next], VK_IMAGE_LAYOUT_GENERAL,
                VK_ACCESS_MEMORY_READ_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
        return next;
    }

    protected final void publishOutput(int index) {
        publishedSource = index;
    }

    @Override
    public final ITexture outputTexture() {
        if (publishedSource < 0) {
            return null;
        }
        return publishesExecutionOutput() ? executionTextures.get(OutputColor) : sourceOutputs[publishedSource];
    }

    @Override
    public final void awaitUsers() {
        if (hasPendingDispatch()) {
            throw new IllegalStateException("Cannot retire resources during a Vulkan interop dispatch");
        }
        if (vulkanDevice != null) {
            PresentationBackendManager.flushCapturedFrame();
            B3DVulkanBridge.awaitIdle();
            awaitExecution();
        }
    }

    private void destroyResources() {
        for (int i = 0; i < sourceOutputs.length; i++) {
            if (sourceOutputs[i] != null) {
                sourceOutputs[i].destroy();
                sourceOutputs[i] = null;
            }
        }
        destroyExecution();
        executionTextures.clear();
        vulkanTextures.clear();
        resourceAccess = null;
        builtLayout = null;
        publishedSource = -1;
        vulkanDevice = null;
    }

    @Override
    public final void destroy() {
        if (destroyed) {
            return;
        }
        awaitUsers();
        destroyResources();
        initialized = false;
        destroyed = true;
    }
}
