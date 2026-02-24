package io.homo.superresolution.common.minecraft.handler.shadercompat;

public interface ShaderPipelineContext {
    Object getCustomUniformValue(String name);
    Object getCustomVariableValue(String name);
}
