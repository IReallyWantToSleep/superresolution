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

package io.homo.superresolution.core.graphics.opengl.pipeline;

import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.opengl.Gl;

import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

public class GlPipelineDescriptorSet extends PipelineDescriptorSet {

    public GlPipelineDescriptorSet(IShaderProgram shader) {
        super(shader);
    }

    @Override
    protected void updateImpl() {
    }

    public void applyFromSnapshot(DescriptorSnapshot snapshot) {
        applyFromSnapshot(snapshot.getBindings());
    }

    public void applyFromSnapshot(Map<String, ResourceBinding> snapshot) {
        for (Map.Entry<String, ResourceBinding> entry : snapshot.entrySet()) {
            String name = entry.getKey();
            ResourceBinding binding = entry.getValue();
            switch (binding.type()) {
                case UNIFORM_BUFFER -> {
                    if (Gl.isLegacy()) {
                        int blockIndex = glGetUniformBlockIndex(
                                (int) shader.handle(),
                                name
                        );
                        if (blockIndex == GL_INVALID_INDEX) {
                            throw new RuntimeException("Uniform block '%s' not found".formatted(name));
                        }
                        glUniformBlockBinding((int) shader.handle(), blockIndex, binding.bindingPoint());
                        glBindBufferBase(GL_UNIFORM_BUFFER, binding.bindingPoint(), (int) binding.resource().handle());
                    } else {
                        Gl.DSA.bindBufferBase(GL_UNIFORM_BUFFER, binding.bindingPoint(), (int) binding.resource().handle());
                    }
                }

                case SAMPLER_TEXTURE -> {
                    ITexture texture = (ITexture) binding.resource();
                    glActiveTexture(GL_TEXTURE0 + binding.bindingPoint());

                    if (texture.getTextureType() == TextureType.Texture1D) {
                        glBindTexture(GL_TEXTURE_1D, (int) texture.handle());
                    } else if (texture.getTextureType() == TextureType.Texture2D) {
                        glBindTexture(GL_TEXTURE_2D, (int) texture.handle());
                    } else {
                        glBindTexture(GL_TEXTURE_2D, (int) texture.handle());
                    }
                    glUniform1i(glGetUniformLocation((int) shader.handle(), name), binding.bindingPoint());
                }

                case STORAGE_IMAGE -> {
                    Gl.DSA.bindImageTexture(
                            binding.bindingPoint(),
                            (int) binding.resource().handle(),
                            0,
                            false,
                            0,
                            switch (shader.getDescription().resourcesLayout().getResource(name).access()) {
                                case Read -> GL_READ_ONLY;
                                case Write -> GL_WRITE_ONLY;
                                case Both -> GL_READ_WRITE;
                            },
                            ((ITexture) binding.resource()).getTextureFormat().gl()
                    );
                }
            }
        }
    }

    @Override
    public void apply() {
        applyFromSnapshot(bindings);
    }
}
