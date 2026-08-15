/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.homo.superresolution.common.upscale.dlssrr;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesConverter;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.vulkan.VkGlInteropSemaphore;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBufferRing;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;

/** Vulkan/GL interop base for algorithms whose input set varies by dispatch. */
public abstract class DLSSRRVulkanInteropAlgorithm extends AbstractAlgorithm {
    public static final int INITIAL_COMMAND_BUFFER_RING_SIZE = 5;
    public static final int MAX_IN_FLIGHT_FRAME = 3;

    private final VulkanCommandBufferRing commandBufferRing =
            new VulkanCommandBufferRing(INITIAL_COMMAND_BUFFER_RING_SIZE);
    protected final InFlightFrameResourcesSet[] inFlightFrames =
            new InFlightFrameResourcesSet[MAX_IN_FLIGHT_FRAME];
    protected boolean syncSerialMode;
    protected int interopFrameSequence;

    private int builtRenderWidth = -1;
    private int builtRenderHeight = -1;
    private int builtScreenWidth = -1;
    private int builtScreenHeight = -1;

    protected abstract void dispatchVulkanUpscale(VulkanCommandBuffer commandBuffer, InFlightFrameResourcesSet frame);

    protected boolean isVulkanInteropReady() { return true; }
    protected boolean hasRequiredInputResources(InputResourceSet resources) { return true; }
    protected void onInteropResourcesCreated() { }
    protected void onBeforeInteropResourcesDestroyed() { }

    @Override
    public void initialize(InitializationDescription desc) {
        syncSerialMode = SuperResolutionConfig.getInteropSyncMode() == InteropSyncMode.LowLatency;
        initDesc = desc;
        createResources();
        onInteropResourcesCreated();
    }

    @Override
    public boolean dispatch(DispatchResource resource) {
        if (!isVulkanInteropReady() || resource.resources() == null || !hasRequiredInputResources(resource.resources())) {
            return false;
        }
        super.dispatch(resource);
        interopFrameSequence++;
        if (syncSerialMode) {
            dispatchSerial(resource);
        } else {
            dispatchPipelined(resource);
        }
        return true;
    }

    private void dispatchSerial(DispatchResource resource) {
        InFlightFrameResourcesSet frame = inFlightFrames[0];
        prepareFrame(frame, resource);
        VulkanDevice device = RenderSystems.vulkan().device();
        VulkanCommandBuffer commandBuffer = commandBufferRing.acquire(device);
        commandBuffer.begin();
        dispatchVulkanUpscale(commandBuffer, frame);
        commandBuffer.end();
        frame.fence = device.submitCommandBuffer(commandBuffer,
                new long[]{frame.glFinish.getVkSemaphoreHandle()},
                new int[]{VK_PIPELINE_STAGE_ALL_COMMANDS_BIT},
                new long[]{frame.upscaleVkFinish.getVkSemaphoreHandle()});
        frame.commandBuffer = commandBuffer;
        frame.upscaleVkFinish.waitVulkanSignal(new int[]{Math.toIntExact(frame.outputColorGlTexture.handle())},
                new int[0], new int[]{GL_LAYOUT_GENERAL_EXT});
        InteropResourcesConverter.flipY(frame.outputColorGlTexture, frame.flippedOutputGlTexture);
    }

    private void dispatchPipelined(DispatchResource resource) {
        int sequence = interopFrameSequence;
        InFlightFrameResourcesSet inputFrame = inFlightFrames[sequence % MAX_IN_FLIGHT_FRAME];
        if (inputFrame.commandBuffer != null) inputFrame.commandBuffer.waitForFence();
        prepareFrame(inputFrame, resource);

        if (sequence > 1) {
            InFlightFrameResourcesSet evaluateFrame = inFlightFrames[(sequence - 1) % MAX_IN_FLIGHT_FRAME];
            if (evaluateFrame.frameData != null) {
                VulkanDevice device = RenderSystems.vulkan().device();
                VulkanCommandBuffer commandBuffer = commandBufferRing.acquire(device);
                commandBuffer.begin();
                dispatchVulkanUpscale(commandBuffer, evaluateFrame);
                commandBuffer.end();
                evaluateFrame.fence = device.submitCommandBuffer(commandBuffer,
                        new long[]{evaluateFrame.glFinish.getVkSemaphoreHandle()},
                        new int[]{VK_PIPELINE_STAGE_ALL_COMMANDS_BIT},
                        new long[]{evaluateFrame.upscaleVkFinish.getVkSemaphoreHandle()});
                evaluateFrame.commandBuffer = commandBuffer;
            }
        }
        if (sequence > 2) {
            InFlightFrameResourcesSet outputFrame = inFlightFrames[(sequence - 2) % MAX_IN_FLIGHT_FRAME];
            if (outputFrame.commandBuffer != null) {
                outputFrame.commandBuffer.waitForFence();
                outputFrame.upscaleVkFinish.waitVulkanSignal(
                        new int[]{Math.toIntExact(outputFrame.outputColorGlTexture.handle())},
                        new int[0], new int[]{GL_LAYOUT_GENERAL_EXT});
                InteropResourcesConverter.flipY(outputFrame.outputColorGlTexture, outputFrame.flippedOutputGlTexture);
            }
        }
    }

    private void prepareFrame(InFlightFrameResourcesSet frame, DispatchResource resource) {
        frame.ensureInputTextures(resource.resources());
        frame.copyInputs(resource.resources());
        frame.frameData = FrameData.from(resource);
        int count = frame.presentInputTypes.size();
        int[] textures = new int[count];
        int[] layouts = new int[count];
        int index = 0;
        for (InputResourceType type : frame.presentInputTypes) {
            textures[index] = Math.toIntExact(frame.inputGlTextures.get(type).handle());
            layouts[index++] = GL_LAYOUT_SHADER_READ_ONLY_EXT;
        }
        frame.glFinish.signalVulkan(textures, new int[0], layouts);
    }

    private void createResources() {
        VulkanDevice device = RenderSystems.vulkan().device();
        device.getMainQueue().waitIdle();
        for (int i = 0; i < (syncSerialMode ? 1 : MAX_IN_FLIGHT_FRAME); i++) {
            inFlightFrames[i] = new InFlightFrameResourcesSet(i);
            inFlightFrames[i].initializeOutputResources();
        }
        builtRenderWidth = RenderHandlerManager.getRenderWidth();
        builtRenderHeight = RenderHandlerManager.getRenderHeight();
        builtScreenWidth = RenderHandlerManager.getScreenWidth();
        builtScreenHeight = RenderHandlerManager.getScreenHeight();
    }

    private void destroyResources() {
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        for (InFlightFrameResourcesSet frame : inFlightFrames) if (frame != null) frame.destroy();
    }

    @Override
    public void resize(int width, int height) {
        if (isVulkanInteropReady()
                && RenderHandlerManager.getRenderWidth() == builtRenderWidth
                && RenderHandlerManager.getRenderHeight() == builtRenderHeight
                && RenderHandlerManager.getScreenWidth() == builtScreenWidth
                && RenderHandlerManager.getScreenHeight() == builtScreenHeight) return;
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        commandBufferRing.destroy();
        onBeforeInteropResourcesDestroyed();
        destroyResources();
        createResources();
        onInteropResourcesCreated();
    }

    @Override
    public void destroy() {
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        commandBufferRing.destroy();
        onBeforeInteropResourcesDestroyed();
        destroyResources();
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return syncSerialMode ? inFlightFrames[0].outputFrameBuffer
                : inFlightFrames[(interopFrameSequence - 2 + MAX_IN_FLIGHT_FRAME) % MAX_IN_FLIGHT_FRAME].outputFrameBuffer;
    }

    @Override
    public int getOutputTextureId() {
        InFlightFrameResourcesSet frame = syncSerialMode ? inFlightFrames[0]
                : inFlightFrames[(interopFrameSequence - 2 + MAX_IN_FLIGHT_FRAME) % MAX_IN_FLIGHT_FRAME];
        return Math.toIntExact(frame.flippedOutputGlTexture.handle());
    }

    protected static final class FrameData {
        public final int renderWidth, renderHeight, screenWidth, screenHeight, frameCount, jitterSequenceLength;
        public final float frameTimeDelta, preExposure;
        public final Vector2f renderSize, jitterOffset;
        public final Matrix4f viewMatrix, projectionMatrix;
        private FrameData(DispatchResource value) {
            renderWidth = value.renderWidth(); renderHeight = value.renderHeight();
            screenWidth = value.screenWidth(); screenHeight = value.screenHeight(); frameCount = value.frameCount();
            jitterSequenceLength = value.jitterSequenceLength(); frameTimeDelta = value.frameTimeDelta();
            preExposure = value.preExposure(); renderSize = new Vector2f(value.renderSize());
            jitterOffset = new Vector2f(value.jitterOffset()); viewMatrix = new Matrix4f(value.viewMatrix());
            projectionMatrix = new Matrix4f(value.projectionMatrix());
        }
        static FrameData from(DispatchResource value) { return new FrameData(value); }
    }

    protected static class InFlightFrameResourcesSet {
        protected final int index;
        protected final EnumMap<InputResourceType, GlImportableTexture2D> inputGlTextures =
                new EnumMap<>(InputResourceType.class);
        protected final EnumMap<InputResourceType, VulkanTexture> inputVkTextures =
                new EnumMap<>(InputResourceType.class);
        protected final EnumSet<InputResourceType> presentInputTypes =
                EnumSet.noneOf(InputResourceType.class);
        protected GlImportableTexture2D outputColorGlTexture;
        protected VulkanTexture outputColorVkTexture;
        protected GlTexture2D flippedOutputGlTexture;
        protected IFrameBuffer outputFrameBuffer;
        protected VkGlInteropSemaphore glFinish;
        protected VkGlInteropSemaphore upscaleVkFinish;
        protected FrameData frameData;
        protected VulkanCommandBuffer commandBuffer;
        protected long fence;

        private InFlightFrameResourcesSet(int index) { this.index = index; }

        public Map<InputResourceType, VulkanTexture> inputVkTextures() { return inputVkTextures; }
        public Map<InputResourceType, VulkanTexture> presentInputVkTextures() {
            EnumMap<InputResourceType, VulkanTexture> result = new EnumMap<>(InputResourceType.class);
            for (InputResourceType type : presentInputTypes) result.put(type, inputVkTextures.get(type));
            return result;
        }
        public Map<InputResourceType, GlImportableTexture2D> inputGlTextures() { return inputGlTextures; }
        public VulkanTexture inputVkTexture(InputResourceType type) { return inputVkTextures.get(type); }
        public FrameData frameData() { return frameData; }

        private void initializeOutputResources() {
            VulkanDevice vk = RenderSystems.vulkan().device();
            GlDevice gl = RenderSystems.opengl().device();
            outputColorVkTexture = vk.createTextureExportable(TextureDescription.create().type(TextureType.Texture2D)
                    .usages(TextureUsages.create().sampler().storage().transferDestination())
                    .format(SuperResolutionConfig.getInternalTextureFormat()).width(RenderHandlerManager.getScreenWidth())
                    .height(RenderHandlerManager.getScreenHeight()).label("DLSSRR-Output-" + index).build());
            outputColorGlTexture = gl.createTextureImportable(outputColorVkTexture);
            flippedOutputGlTexture = (GlTexture2D) gl.createTexture(TextureDescription.create().type(TextureType.Texture2D)
                    .usages(TextureUsages.create().sampler().storage().transferDestination())
                    .format(SuperResolutionConfig.getInternalTextureFormat()).width(RenderHandlerManager.getScreenWidth())
                    .height(RenderHandlerManager.getScreenHeight()).label("DLSSRR-FlippedOutput-" + index).build());
            outputFrameBuffer = RenderSystems.current().device().createFramebuffer(
                    FramebufferDescription.create().colorAttachment(flippedOutputGlTexture).build());
            glFinish = VkGlInteropSemaphore.create(vk);
            upscaleVkFinish = VkGlInteropSemaphore.create(vk);
        }

        private void ensureInputTextures(InputResourceSet resources) {
            presentInputTypes.clear();
            for (InputResourceType type : InputResourceType.values()) {
                ITexture source = resources.get(type);
                if (source == null) {
                    destroyInput(type);
                    continue;
                }
                presentInputTypes.add(type);
                VulkanTexture existing = inputVkTextures.get(type);
                if (existing != null && existing.getTextureFormat() == source.getTextureFormat()
                        && existing.getWidth() == source.getWidth() && existing.getHeight() == source.getHeight()) continue;
                destroyInput(type);
                VulkanTexture vkTexture = RenderSystems.vulkan().device().createTextureExportable(
                        TextureDescription.create().type(source.getTextureType())
                                .usages(TextureUsages.create().sampler().storage().transferSource().transferDestination())
                                .format(source.getTextureFormat()).width(source.getWidth()).height(source.getHeight())
                                .label("DLSSRR-" + type + "-" + index).build());
                inputVkTextures.put(type, vkTexture);
                inputGlTextures.put(type, RenderSystems.opengl().device().createTextureImportable(vkTexture));
            }
        }

        private void copyInputs(InputResourceSet resources) {
            ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
            try {
                commandBuffer.begin();
                for (InputResourceType type : presentInputTypes) {
                    ITexture source = resources.get(type);
                    GlImportableTexture2D destination = inputGlTextures.get(type);
                    if (type == InputResourceType.MotionVectors || type == InputResourceType.SpecularMotionVectors) {
                        InteropResourcesConverter.flipMotionVectorY(commandBuffer, source, destination);
                    } else {
                        InteropResourcesConverter.flipY(commandBuffer, source, destination);
                    }
                }
                commandBuffer.end();
                RenderSystems.current().device().submitCommandBuffer(commandBuffer);
                commandBuffer.waitForFence();
            } finally { commandBuffer.destroy(); }
        }

        private void destroyInput(InputResourceType type) {
            GlImportableTexture2D gl = inputGlTextures.remove(type);
            VulkanTexture vk = inputVkTextures.remove(type);
            if (gl != null) gl.destroy();
            if (vk != null) vk.destroy();
        }

        private void destroy() {
            for (InputResourceType type : InputResourceType.values()) destroyInput(type);
            if (outputColorGlTexture != null) outputColorGlTexture.destroy();
            if (outputColorVkTexture != null) outputColorVkTexture.destroy();
            if (flippedOutputGlTexture != null) flippedOutputGlTexture.destroy();
            if (outputFrameBuffer != null) outputFrameBuffer.destroy();
            if (glFinish != null) glFinish.destroy();
            if (upscaleVkFinish != null) upscaleVkFinish.destroy();
        }
    }
}
