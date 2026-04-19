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
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlRenderPass;

import java.util.ArrayList;
import java.util.List;

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

    private void clearRenderPassState() {
        activeRenderPass = null;
        boundGraphicsPipeline = null;
        boundComputePipeline = null;
        renderPassActive = false;
    }
}
