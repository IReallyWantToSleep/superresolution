package io.homo.superresolution.core.graphics.impl.command.commands;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.system.IRenderState;

public class DrawCommand extends GpuCommand {
    public final float[] viewport = new float[]{-1, -1, -1, -1};
    public IFrameBuffer frameBuffer;
    public IShaderProgram<?> program = null;
    public DrawObject drawObject = null;
    public int firstVertex;
    public int vertexCount;
    public IRenderState.StateSnapshot stateSnapshot = null;

    public DrawCommand(IShaderProgram<?> program) {
        this.program = program;
    }

    @Override
    public GpuCommandType getCommandType() {
        return GpuCommandType.Draw;
    }
}
