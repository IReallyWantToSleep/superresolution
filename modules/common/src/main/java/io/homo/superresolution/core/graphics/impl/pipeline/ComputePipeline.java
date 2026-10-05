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

package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;

public abstract class ComputePipeline implements IPipeline {
    private final IShaderProgram shader;
    private final PipelineDescriptorSet descriptorSet;

    public ComputePipeline(IShaderProgram shader, PipelineDescriptorSet descriptorSet) {
        if (shader == null) {
            throw new IllegalArgumentException("Shader cannot be null");
        }
        this.shader = shader;
        this.descriptorSet = descriptorSet;
    }

    public static Builder builder() {
        return new Builder();
    }

    public PipelineDescriptorSet descriptorSet() {
        return descriptorSet;
    }

    public IShaderProgram shader() {
        return shader;
    }

    public static class Builder {
        private IShaderProgram shader;

        public IShaderProgram shader() {
            return shader;
        }

        public Builder shader(IShaderProgram shader) {
            this.shader = shader;
            return this;
        }

        public ComputePipeline build(IDevice device) {
            if (shader == null) {
                throw new IllegalStateException("Shader is required");
            }
            return device.createComputePipeline(this);
        }
    }
}
