package io.homo.superresolution.core.graphics.impl.command.commands;

import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.math.Vector3i;

public class ComputeCommand {
    public IShaderProgram<?> shaderProgram;
    public Vector3i workGroupSize;

    public ComputeCommand(IShaderProgram<?> shaderProgram, Vector3i workGroupSize) {
        this.shaderProgram = shaderProgram;
        this.workGroupSize = workGroupSize;
    }
}
