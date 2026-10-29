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
        List<CachedUniform> uniforms = IrisReflectionUtils.getUniformOrderCustomUniforms(customUniforms);
        for (CachedUniform uniform : uniforms) {
            if (uniform.getName().equals(name)) {
                FunctionReturn value = new FunctionReturn();
                uniform.writeTo(value);
                if (uniform.getType().equals(Type.Boolean)) {
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
        List<CachedUniform> uniforms = IrisReflectionUtils.getVariableCustomUniforms(customUniforms);
        for (CachedUniform uniform : uniforms) {
            if (uniform.getName().equals(name)) {
                FunctionReturn value = new FunctionReturn();
                uniform.writeTo(value);
                if (uniform.getType().equals(Type.Boolean)) {
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
