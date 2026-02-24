package io.homo.superresolution.shadercompat;

import io.homo.irisapi.IrisReflectionUtils;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderPipelineContext;
import kroppeb.stareval.function.FunctionReturn;
import kroppeb.stareval.function.Type;
import net.irisshaders.iris.parsing.VectorType;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;

import java.util.List;

public class IrisShaderPipelineContext implements ShaderPipelineContext {
    public final IrisRenderingPipeline pipeline;

    public IrisShaderPipelineContext(IrisRenderingPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public Object getCustomUniformValue(String name) {
        CustomUniforms customUniforms = pipeline.getCustomUniforms();
        List<CachedUniform> uniforms =IrisReflectionUtils.getUniformOrderCustomUniforms(customUniforms);
        for (CachedUniform uniform : uniforms) {
            if (uniform.getName().equals(name)) {
                FunctionReturn value = new FunctionReturn();
                uniform.writeTo(value);
                if (uniform.getType().equals(Type.Boolean)){
                    return value.booleanReturn;
                } else if (uniform.getType().equals(Type.Float)) {
                    return value.floatReturn;
                } else if (uniform.getType().equals(Type.Int)) {
                    return value.intReturn;
                } else if (uniform.getType().equals(VectorType.VEC2) || uniform.getType().equals(VectorType.VEC3) || uniform.getType().equals(VectorType.VEC4)) {
                    return value.objectReturn;
                }
            }
        }
        return null;
    }

    @Override
    public Object getCustomVariableValue(String name) {
        CustomUniforms customUniforms = pipeline.getCustomUniforms();
        List<CachedUniform> uniforms =IrisReflectionUtils.getVariableCustomUniforms(customUniforms);
        for (CachedUniform uniform : uniforms) {
            if (uniform.getName().equals(name)) {
                FunctionReturn value = new FunctionReturn();
                uniform.writeTo(value);
                if (uniform.getType().equals(Type.Boolean)){
                    return value.booleanReturn;
                } else if (uniform.getType().equals(Type.Float)) {
                    return value.floatReturn;
                } else if (uniform.getType().equals(Type.Int)) {
                    return value.intReturn;
                } else if (uniform.getType().equals(VectorType.VEC2) || uniform.getType().equals(VectorType.VEC3) || uniform.getType().equals(VectorType.VEC4)) {
                    return value.objectReturn;
                }
            }
        }
        return null;
    }
}
