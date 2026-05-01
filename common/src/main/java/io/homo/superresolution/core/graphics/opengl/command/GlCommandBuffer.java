/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.opengl.command;

import io.homo.superresolution.core.graphics.impl.command.*;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.pipeline.state.ColorBlendAttachment;
import io.homo.superresolution.core.graphics.impl.pipeline.state.ColorBlendState;
import io.homo.superresolution.core.graphics.impl.pipeline.state.DepthStencilState;
import io.homo.superresolution.core.graphics.impl.pipeline.state.RasterizationState;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlRenderPass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlCommandBuffer implements ICommandBuffer {
    private final List<Runnable> glCalls = new ArrayList<>();
    private final GlDevice device;
    private final ICommandPool ownerPool;
    private final CommandBufferBehavior behavior;
    private CommandBufferState state = CommandBufferState.Executable;
    private GlRenderPass activeRenderPass;
    private GlGraphicsPipeline boundGraphicsPipeline;
    private GlComputePipeline boundComputePipeline;
    private boolean renderPassActive;
    private final ExecutionStateCache executionStateCache = new ExecutionStateCache();

    public GlCommandBuffer(GlDevice device, ICommandPool ownerPool, CommandBufferBehavior behavior) {
        this.device = device;
        this.ownerPool = ownerPool;
        this.behavior = behavior;
    }

    protected void _addGlCalls(Runnable glCalls) {
        if (state != CommandBufferState.Recording) {
            throw new IllegalStateException("CommandBuffer is not in recording state");
        }
        this.glCalls.add(glCalls);
    }

    @Override
    public void begin() {
        if (state == CommandBufferState.Destroyed) {
            throw new IllegalStateException("Cannot begin a destroyed command buffer");
        }
        if (state == CommandBufferState.Recording) {
            throw new IllegalStateException("Command buffer is already recording");
        }
        clearRenderPassState();
        state = CommandBufferState.Recording;
    }

    @Override
    public void end() {
        if (state != CommandBufferState.Recording) {
            throw new IllegalStateException("Command buffer is not recording");
        }
        if (renderPassActive) {
            throw new IllegalStateException("Command buffer still has an active render pass; call endRenderPass first");
        }
        state = CommandBufferState.Executable;
    }

    @Override
    public void reset() {
        if (state == CommandBufferState.Destroyed) {
            throw new IllegalStateException("Cannot reset a destroyed command buffer");
        }
        glCalls.clear();
        clearRenderPassState();
        state = CommandBufferState.Executable;
    }

    @Override
    public void destroy() {
        if (state == CommandBufferState.Destroyed) {
            return;
        }
        glCalls.clear();
        clearRenderPassState();
        state = CommandBufferState.Destroyed;
    }

    @Override
    public void submit(IDevice device) {
        if (state != CommandBufferState.Executable) {
            throw new IllegalStateException("Command buffer must be executable before submit");
        }
        GlDebug.pushGroup(GlDebug.nextCommandBufferId(), "Command Buffer");
        for (Runnable call : glCalls) {
            call.run();
        }
        GlDebug.popGroup();
        if (behavior == CommandBufferBehavior.OneTimeSubmit) {
            destroy();
        }
    }

    @Override
    public IDevice getDevice() {
        return device;
    }

    @Override
    public ICommandDecoder decoder() {
        return device.commandDecoder();
    }

    @Override
    public ICommandPool ownerPool() {
        return ownerPool;
    }

    @Override
    public CommandBufferState state() {
        return state;
    }

    @Override
    public CommandBufferBehavior behavior() {
        return behavior;
    }

    void beginRenderPass(GlRenderPass renderPass) {
        if (state != CommandBufferState.Recording) {
            throw new IllegalStateException("Command buffer is not in recording state");
        }
        if (renderPassActive) {
            throw new IllegalStateException("Render pass is already active");
        }
        this.activeRenderPass = renderPass;
        this.boundGraphicsPipeline = null;
        this.renderPassActive = true;
    }

    void endRenderPass() {
        if (!renderPassActive) {
            throw new IllegalStateException("No active render pass to end");
        }
        boundGraphicsPipeline = null;
        clearRenderPassState();
    }

    void bindGraphicsPipeline(GlGraphicsPipeline pipeline) {
        this.boundGraphicsPipeline = pipeline;
    }

    void bindComputePipeline(GlComputePipeline pipeline) {
        this.boundComputePipeline = pipeline;
    }

    GlGraphicsPipeline getBoundGraphicsPipeline() {
        return boundGraphicsPipeline;
    }

    GlComputePipeline getBoundComputePipeline() {
        return boundComputePipeline;
    }

    boolean isRenderPassActive() {
        return renderPassActive;
    }

    GlRenderPass getActiveRenderPass() {
        return activeRenderPass;
    }

    public ExecutionStateCache executionStateCache() {
        return executionStateCache;
    }

    private void clearRenderPassState() {
        activeRenderPass = null;
        boundGraphicsPipeline = null;
        boundComputePipeline = null;
        renderPassActive = false;
        executionStateCache.invalidateAll();
    }

    public static final class ExecutionStateCache {
        private int currentProgram = -1;
        private int activeTextureUnit = -1;
        private Object currentVao;
        private Object currentArrayBuffer;
        private RasterizationSnapshot rasterizationSnapshot;
        private DepthStencilSnapshot depthStencilSnapshot;
        private ColorBlendSnapshot colorBlendSnapshot;
        private ViewportSnapshot viewportSnapshot;
        private ScissorSnapshot scissorSnapshot;
        private Float lineWidth;
        private BlendConstantsSnapshot blendConstantsSnapshot;
        private final Map<Integer, UniformBufferBindingSnapshot> uniformBufferBindings = new HashMap<>();
        private final Map<Integer, SamplerBindingSnapshot> samplerBindings = new HashMap<>();
        private final Map<Integer, StorageImageBindingSnapshot> storageImageBindings = new HashMap<>();
        private final Map<ProgramResourceKey, Integer> samplerUniformBindings = new HashMap<>();
        private final Map<ProgramResourceKey, Integer> uniformBlockBindings = new HashMap<>();

        public void invalidateAll() {
            currentProgram = -1;
            activeTextureUnit = -1;
            currentVao = null;
            currentArrayBuffer = null;
            rasterizationSnapshot = null;
            depthStencilSnapshot = null;
            colorBlendSnapshot = null;
            viewportSnapshot = null;
            scissorSnapshot = null;
            lineWidth = null;
            blendConstantsSnapshot = null;
            uniformBufferBindings.clear();
            samplerBindings.clear();
            storageImageBindings.clear();
            samplerUniformBindings.clear();
            uniformBlockBindings.clear();
        }

        public boolean matchesProgram(int program) {
            return currentProgram == program;
        }

        public void recordProgram(int program) {
            currentProgram = program;
        }

        public boolean matchesActiveTextureUnit(int unit) {
            return activeTextureUnit == unit;
        }

        public void recordActiveTextureUnit(int unit) {
            activeTextureUnit = unit;
        }

        public boolean matchesRasterizationState(RasterizationState state) {
            return rasterizationSnapshot != null && rasterizationSnapshot.matches(state);
        }

        public boolean matchesPolygonMode(Object polygonMode) {
            return rasterizationSnapshot != null && rasterizationSnapshot.polygonMode() == polygonMode;
        }

        public boolean matchesCullMode(Object cullMode) {
            return rasterizationSnapshot != null && rasterizationSnapshot.cullMode() == cullMode;
        }

        public boolean matchesFrontFace(Object frontFace) {
            return rasterizationSnapshot != null && rasterizationSnapshot.frontFace() == frontFace;
        }

        public boolean matchesDepthClampEnable(boolean depthClampEnable) {
            return rasterizationSnapshot != null && rasterizationSnapshot.depthClampEnable() == depthClampEnable;
        }

        public boolean matchesRasterizerDiscardEnable(boolean rasterizerDiscardEnable) {
            return rasterizationSnapshot != null && rasterizationSnapshot.rasterizerDiscardEnable() == rasterizerDiscardEnable;
        }

        public void recordRasterizationState(RasterizationState state) {
            rasterizationSnapshot = RasterizationSnapshot.of(state);
        }

        public boolean matchesDepthStencilState(DepthStencilState state) {
            return depthStencilSnapshot != null && depthStencilSnapshot.matches(state);
        }

        public boolean matchesDepthTestEnable(boolean depthTestEnable) {
            return depthStencilSnapshot != null && depthStencilSnapshot.depthTestEnable() == depthTestEnable;
        }

        public boolean matchesDepthWriteEnable(boolean depthWriteEnable) {
            return depthStencilSnapshot != null && depthStencilSnapshot.depthWriteEnable() == depthWriteEnable;
        }

        public boolean matchesDepthCompareOp(Object depthCompareOp) {
            return depthStencilSnapshot != null && depthStencilSnapshot.depthCompareOp() == depthCompareOp;
        }

        public boolean matchesStencilTestEnable(boolean stencilTestEnable) {
            return depthStencilSnapshot != null && depthStencilSnapshot.stencilTestEnable() == stencilTestEnable;
        }

        public boolean matchesStencilWriteMask(boolean frontFace, int stencilWriteMask) {
            return depthStencilSnapshot != null && depthStencilSnapshot.stencilWriteMask() == stencilWriteMask;
        }

        public boolean matchesStencilFunc(boolean frontFace, Object compareOp, int reference, int compareMask) {
            if (depthStencilSnapshot == null) {
                return false;
            }
            return frontFace
                    ? depthStencilSnapshot.stencilCompareOpFront() == compareOp
                    && depthStencilSnapshot.stencilReference() == reference
                    && depthStencilSnapshot.stencilCompareMask() == compareMask
                    : depthStencilSnapshot.stencilCompareOpBack() == compareOp
                    && depthStencilSnapshot.stencilReference() == reference
                    && depthStencilSnapshot.stencilCompareMask() == compareMask;
        }

        public boolean matchesStencilOp(boolean frontFace, Object failOp, Object depthFailOp, Object passOp) {
            if (depthStencilSnapshot == null) {
                return false;
            }
            return frontFace
                    ? depthStencilSnapshot.stencilFailOpFront() == failOp
                    && depthStencilSnapshot.stencilDepthFailOpFront() == depthFailOp
                    && depthStencilSnapshot.stencilPassOpFront() == passOp
                    : depthStencilSnapshot.stencilFailOpBack() == failOp
                    && depthStencilSnapshot.stencilDepthFailOpBack() == depthFailOp
                    && depthStencilSnapshot.stencilPassOpBack() == passOp;
        }

        public void recordDepthStencilState(DepthStencilState state) {
            depthStencilSnapshot = DepthStencilSnapshot.of(state);
        }

        public boolean matchesColorBlendState(ColorBlendState state) {
            return colorBlendSnapshot != null && colorBlendSnapshot.matches(state);
        }

        public boolean matchesBlendEnable(int attachmentIndex, boolean blendEnable) {
            ColorBlendAttachmentSnapshot snapshot = getColorBlendAttachmentSnapshot(attachmentIndex);
            return snapshot != null && snapshot.blendEnable() == blendEnable;
        }

        public boolean matchesBlendFunction(int attachmentIndex,
                                            Object srcColorBlendFactor,
                                            Object dstColorBlendFactor,
                                            Object srcAlphaBlendFactor,
                                            Object dstAlphaBlendFactor) {
            ColorBlendAttachmentSnapshot snapshot = getColorBlendAttachmentSnapshot(attachmentIndex);
            return snapshot != null
                    && snapshot.srcColorBlendFactor() == srcColorBlendFactor
                    && snapshot.dstColorBlendFactor() == dstColorBlendFactor
                    && snapshot.srcAlphaBlendFactor() == srcAlphaBlendFactor
                    && snapshot.dstAlphaBlendFactor() == dstAlphaBlendFactor;
        }

        public boolean matchesBlendEquation(int attachmentIndex, Object colorBlendOp, Object alphaBlendOp) {
            ColorBlendAttachmentSnapshot snapshot = getColorBlendAttachmentSnapshot(attachmentIndex);
            return snapshot != null
                    && snapshot.colorBlendOp() == colorBlendOp
                    && snapshot.alphaBlendOp() == alphaBlendOp;
        }

        public boolean matchesColorWriteMask(int attachmentIndex, int colorWriteMask) {
            ColorBlendAttachmentSnapshot snapshot = getColorBlendAttachmentSnapshot(attachmentIndex);
            return snapshot != null && snapshot.colorWriteMask() == colorWriteMask;
        }

        public void recordColorBlendState(ColorBlendState state) {
            colorBlendSnapshot = ColorBlendSnapshot.of(state);
        }

        public boolean matchesViewport(float x, float y, float width, float height) {
            return viewportSnapshot != null && viewportSnapshot.matches(x, y, width, height);
        }

        public void recordViewport(float x, float y, float width, float height) {
            viewportSnapshot = new ViewportSnapshot(x, y, width, height);
        }

        public boolean matchesScissor(int x, int y, int width, int height) {
            return scissorSnapshot != null && scissorSnapshot.matches(x, y, width, height);
        }

        public void recordScissor(int x, int y, int width, int height) {
            scissorSnapshot = new ScissorSnapshot(x, y, width, height);
        }

        public boolean matchesLineWidth(float width) {
            return lineWidth != null && Float.compare(lineWidth, width) == 0;
        }

        public void recordLineWidth(float width) {
            lineWidth = width;
        }

        public boolean matchesBlendConstants(float r, float g, float b, float a) {
            return blendConstantsSnapshot != null && blendConstantsSnapshot.matches(r, g, b, a);
        }

        public void recordBlendConstants(float r, float g, float b, float a) {
            blendConstantsSnapshot = new BlendConstantsSnapshot(r, g, b, a);
        }

        public boolean matchesUniformBufferBinding(int bindingPoint, long handle, long offset, long range) {
            UniformBufferBindingSnapshot snapshot = uniformBufferBindings.get(bindingPoint);
            return snapshot != null && snapshot.matches(handle, offset, range);
        }

        public void recordUniformBufferBinding(int bindingPoint, long handle, long offset, long range) {
            uniformBufferBindings.put(bindingPoint, new UniformBufferBindingSnapshot(handle, offset, range));
        }

        public boolean matchesSamplerBinding(int bindingPoint, int textureTarget, long textureHandle, long samplerHandle) {
            SamplerBindingSnapshot snapshot = samplerBindings.get(bindingPoint);
            return snapshot != null && snapshot.matches(textureTarget, textureHandle, samplerHandle);
        }

        public void recordSamplerBinding(int bindingPoint, int textureTarget, long textureHandle, long samplerHandle) {
            samplerBindings.put(bindingPoint, new SamplerBindingSnapshot(textureTarget, textureHandle, samplerHandle));
        }

        public boolean matchesStorageImageBinding(int bindingPoint, long textureHandle, int mipLevel, int access, int format) {
            StorageImageBindingSnapshot snapshot = storageImageBindings.get(bindingPoint);
            return snapshot != null && snapshot.matches(textureHandle, mipLevel, access, format);
        }

        public void recordStorageImageBinding(int bindingPoint, long textureHandle, int mipLevel, int access, int format) {
            storageImageBindings.put(bindingPoint, new StorageImageBindingSnapshot(textureHandle, mipLevel, access, format));
        }

        public boolean matchesSamplerUniformBinding(int programHandle, String name, int bindingPoint) {
            Integer currentBinding = samplerUniformBindings.get(new ProgramResourceKey(programHandle, name));
            return currentBinding != null && currentBinding == bindingPoint;
        }

        public void recordSamplerUniformBinding(int programHandle, String name, int bindingPoint) {
            samplerUniformBindings.put(new ProgramResourceKey(programHandle, name), bindingPoint);
        }

        public boolean matchesUniformBlockBinding(int programHandle, String name, int bindingPoint) {
            Integer currentBinding = uniformBlockBindings.get(new ProgramResourceKey(programHandle, name));
            return currentBinding != null && currentBinding == bindingPoint;
        }

        public void recordUniformBlockBinding(int programHandle, String name, int bindingPoint) {
            uniformBlockBindings.put(new ProgramResourceKey(programHandle, name), bindingPoint);
        }

        public boolean matchesVao(Object vao) {
            return currentVao == vao;
        }

        public void recordVao(Object vao) {
            currentVao = vao;
        }

        public boolean matchesArrayBuffer(Object arrayBuffer) {
            return currentArrayBuffer == arrayBuffer;
        }

        public void recordArrayBuffer(Object arrayBuffer) {
            currentArrayBuffer = arrayBuffer;
        }

        private ColorBlendAttachmentSnapshot getColorBlendAttachmentSnapshot(int attachmentIndex) {
            if (colorBlendSnapshot == null) {
                return null;
            }
            return colorBlendSnapshot.attachment(attachmentIndex);
        }

        private record ProgramResourceKey(int programHandle, String name) {
        }

        private record ViewportSnapshot(float x, float y, float width, float height) {
            private boolean matches(float otherX, float otherY, float otherWidth, float otherHeight) {
                return Float.compare(x, otherX) == 0
                        && Float.compare(y, otherY) == 0
                        && Float.compare(width, otherWidth) == 0
                        && Float.compare(height, otherHeight) == 0;
            }
        }

        private record ScissorSnapshot(int x, int y, int width, int height) {
            private boolean matches(int otherX, int otherY, int otherWidth, int otherHeight) {
                return x == otherX && y == otherY && width == otherWidth && height == otherHeight;
            }
        }

        private record BlendConstantsSnapshot(float r, float g, float b, float a) {
            private boolean matches(float otherR, float otherG, float otherB, float otherA) {
                return Float.compare(r, otherR) == 0
                        && Float.compare(g, otherG) == 0
                        && Float.compare(b, otherB) == 0
                        && Float.compare(a, otherA) == 0;
            }
        }

        private record UniformBufferBindingSnapshot(long handle, long offset, long range) {
            private boolean matches(long otherHandle, long otherOffset, long otherRange) {
                return handle == otherHandle && offset == otherOffset && range == otherRange;
            }
        }

        private record SamplerBindingSnapshot(int textureTarget, long textureHandle, long samplerHandle) {
            private boolean matches(int otherTextureTarget, long otherTextureHandle, long otherSamplerHandle) {
                return textureTarget == otherTextureTarget
                        && textureHandle == otherTextureHandle
                        && samplerHandle == otherSamplerHandle;
            }
        }

        private record StorageImageBindingSnapshot(long textureHandle, int mipLevel, int access, int format) {
            private boolean matches(long otherTextureHandle, int otherMipLevel, int otherAccess, int otherFormat) {
                return textureHandle == otherTextureHandle
                        && mipLevel == otherMipLevel
                        && access == otherAccess
                        && format == otherFormat;
            }
        }

        private record RasterizationSnapshot(
                Object polygonMode,
                Object cullMode,
                Object frontFace,
                boolean depthClampEnable,
                boolean rasterizerDiscardEnable
        ) {
            private static RasterizationSnapshot of(RasterizationState state) {
                return new RasterizationSnapshot(
                        state.polygonMode(),
                        state.cullMode(),
                        state.frontFace(),
                        state.depthClampEnable(),
                        state.rasterizerDiscardEnable()
                );
            }

            private boolean matches(RasterizationState state) {
                return polygonMode == state.polygonMode()
                        && cullMode == state.cullMode()
                        && frontFace == state.frontFace()
                        && depthClampEnable == state.depthClampEnable()
                        && rasterizerDiscardEnable == state.rasterizerDiscardEnable();
            }
        }

        private record DepthStencilSnapshot(
                boolean depthTestEnable,
                boolean depthWriteEnable,
                Object depthCompareOp,
                boolean stencilTestEnable,
                Object stencilCompareOpFront,
                Object stencilCompareOpBack,
                Object stencilFailOpFront,
                Object stencilPassOpFront,
                Object stencilDepthFailOpFront,
                Object stencilFailOpBack,
                Object stencilPassOpBack,
                Object stencilDepthFailOpBack,
                int stencilCompareMask,
                int stencilWriteMask,
                int stencilReference
        ) {
            private static DepthStencilSnapshot of(DepthStencilState state) {
                return new DepthStencilSnapshot(
                        state.depthTestEnable(),
                        state.depthWriteEnable(),
                        state.depthCompareOp(),
                        state.stencilTestEnable(),
                        state.stencilCompareOpFront(),
                        state.stencilCompareOpBack(),
                        state.stencilFailOpFront(),
                        state.stencilPassOpFront(),
                        state.stencilDepthFailOpFront(),
                        state.stencilFailOpBack(),
                        state.stencilPassOpBack(),
                        state.stencilDepthFailOpBack(),
                        state.stencilCompareMask(),
                        state.stencilWriteMask(),
                        state.stencilReference()
                );
            }

            private boolean matches(DepthStencilState state) {
                return depthTestEnable == state.depthTestEnable()
                        && depthWriteEnable == state.depthWriteEnable()
                        && depthCompareOp == state.depthCompareOp()
                        && stencilTestEnable == state.stencilTestEnable()
                        && stencilCompareOpFront == state.stencilCompareOpFront()
                        && stencilCompareOpBack == state.stencilCompareOpBack()
                        && stencilFailOpFront == state.stencilFailOpFront()
                        && stencilPassOpFront == state.stencilPassOpFront()
                        && stencilDepthFailOpFront == state.stencilDepthFailOpFront()
                        && stencilFailOpBack == state.stencilFailOpBack()
                        && stencilPassOpBack == state.stencilPassOpBack()
                        && stencilDepthFailOpBack == state.stencilDepthFailOpBack()
                        && stencilCompareMask == state.stencilCompareMask()
                        && stencilWriteMask == state.stencilWriteMask()
                        && stencilReference == state.stencilReference();
            }
        }

        private record ColorBlendAttachmentSnapshot(
                boolean blendEnable,
                Object srcColorBlendFactor,
                Object dstColorBlendFactor,
                Object colorBlendOp,
                Object srcAlphaBlendFactor,
                Object dstAlphaBlendFactor,
                Object alphaBlendOp,
                int colorWriteMask
        ) {
            private static ColorBlendAttachmentSnapshot of(ColorBlendAttachment attachment) {
                return new ColorBlendAttachmentSnapshot(
                        attachment.blendEnable(),
                        attachment.srcColorBlendFactor(),
                        attachment.dstColorBlendFactor(),
                        attachment.colorBlendOp(),
                        attachment.srcAlphaBlendFactor(),
                        attachment.dstAlphaBlendFactor(),
                        attachment.alphaBlendOp(),
                        attachment.colorWriteMask()
                );
            }

            private boolean matches(ColorBlendAttachment attachment) {
                return blendEnable == attachment.blendEnable()
                        && srcColorBlendFactor == attachment.srcColorBlendFactor()
                        && dstColorBlendFactor == attachment.dstColorBlendFactor()
                        && colorBlendOp == attachment.colorBlendOp()
                        && srcAlphaBlendFactor == attachment.srcAlphaBlendFactor()
                        && dstAlphaBlendFactor == attachment.dstAlphaBlendFactor()
                        && alphaBlendOp == attachment.alphaBlendOp()
                        && colorWriteMask == attachment.colorWriteMask();
            }
        }

        private static final class ColorBlendSnapshot {
            private final boolean logicOpEnable;
            private final List<ColorBlendAttachmentSnapshot> attachments;

            private ColorBlendSnapshot(boolean logicOpEnable, List<ColorBlendAttachmentSnapshot> attachments) {
                this.logicOpEnable = logicOpEnable;
                this.attachments = attachments;
            }

            private static ColorBlendSnapshot of(ColorBlendState state) {
                List<ColorBlendAttachmentSnapshot> snapshots = new ArrayList<>(state.attachments().size());
                for (ColorBlendAttachment attachment : state.attachments()) {
                    snapshots.add(ColorBlendAttachmentSnapshot.of(attachment));
                }
                return new ColorBlendSnapshot(state.logicOpEnable(), snapshots);
            }

            private boolean matches(ColorBlendState state) {
                if (logicOpEnable != state.logicOpEnable() || attachments.size() != state.attachments().size()) {
                    return false;
                }
                for (int i = 0; i < attachments.size(); i++) {
                    if (!attachments.get(i).matches(state.attachments().get(i))) {
                        return false;
                    }
                }
                return true;
            }

            private ColorBlendAttachmentSnapshot attachment(int index) {
                if (index < 0 || index >= attachments.size()) {
                    return null;
                }
                return attachments.get(index);
            }
        }
    }
}
