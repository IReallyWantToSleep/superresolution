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

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.ITextureView;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandBuffer;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

public class GlPipelineDescriptorSet extends PipelineDescriptorSet {
    private final Map<String, Integer> uniformLocations = new HashMap<>();
    private final Map<String, Integer> uniformBlockIndices = new HashMap<>();

    public GlPipelineDescriptorSet(IShaderProgram shader) {
        super(shader);
    }

    public void applyFromSnapshot(DescriptorSnapshot snapshot) {
        applyFromSnapshot(snapshot, null);
    }

    public void applyFromSnapshot(DescriptorSnapshot snapshot, GlCommandBuffer.ExecutionStateCache stateCache) {
        applyFromSnapshot(snapshot.getBindings(), stateCache);
    }

    public void applyFromSnapshot(Map<String, ResourceBinding> snapshot) {
        applyFromSnapshot(snapshot, null);
    }

    public void applyFromSnapshot(Map<String, ResourceBinding> snapshot, GlCommandBuffer.ExecutionStateCache stateCache) {
        int programHandle = (int) shader.handle();
        for (Map.Entry<String, ResourceBinding> entry : snapshot.entrySet()) {
            String name = entry.getKey();
            ResourceBinding binding = entry.getValue();
            switch (binding.type()) {
                case UNIFORM_BUFFER -> {
                    IBuffer buffer = (IBuffer) binding.resource();
                    if (Gl.isLegacy()) {
                        if (stateCache == null || !stateCache.matchesUniformBlockBinding(programHandle, name, binding.bindingPoint())) {
                            int blockIndex = getUniformBlockIndex(name);
                            if (blockIndex == GL_INVALID_INDEX) {
                                throw new RuntimeException("Uniform block '%s' not found".formatted(name));
                            }
                            glUniformBlockBinding(programHandle, blockIndex, binding.bindingPoint());
                            if (stateCache != null) {
                                stateCache.recordUniformBlockBinding(programHandle, name, binding.bindingPoint());
                            }
                        }
                    }
                    if (stateCache == null || !stateCache.matchesUniformBufferBinding(binding.bindingPoint(), buffer.handle(), binding.offset(), binding.range())) {
                        glBindBufferRange(GL_UNIFORM_BUFFER, binding.bindingPoint(), (int) buffer.handle(), binding.offset(), binding.range());
                        if (stateCache != null) {
                            stateCache.recordUniformBufferBinding(binding.bindingPoint(), buffer.handle(), binding.offset(), binding.range());
                        }
                    }
                }

                case SAMPLER_TEXTURE -> {
                    ITexture texture = (ITexture) binding.resource();
                    int textureTarget = getTextureTarget(texture.getTextureType());
                    long samplerHandle = binding.sampler() != null ? binding.sampler().handle() : 0L;
                    if (stateCache == null || !stateCache.matchesSamplerBinding(binding.bindingPoint(), textureTarget, texture.handle(), samplerHandle)) {
                        if (stateCache == null || !stateCache.matchesActiveTextureUnit(binding.bindingPoint())) {
                            glActiveTexture(GL_TEXTURE0 + binding.bindingPoint());
                            if (stateCache != null) {
                                stateCache.recordActiveTextureUnit(binding.bindingPoint());
                            }
                        }
                        glBindTexture(textureTarget, (int) texture.handle());
                        Gl.DSA.bindSampler(binding.bindingPoint(), (int) samplerHandle);
                        if (stateCache != null) {
                            stateCache.recordSamplerBinding(binding.bindingPoint(), textureTarget, texture.handle(), samplerHandle);
                        }
                    }
                    if (stateCache == null || !stateCache.matchesSamplerUniformBinding(programHandle, name, binding.bindingPoint())) {
                        int uniformLocation = getUniformLocation(name);
                        if (uniformLocation >= 0) {
                            glUniform1i(uniformLocation, binding.bindingPoint());
                        }
                        if (stateCache != null) {
                            stateCache.recordSamplerUniformBinding(programHandle, name, binding.bindingPoint());
                        }
                    }
                }

                case STORAGE_IMAGE -> {
                    ITexture tex = (ITexture) binding.resource();
                    int mipLevel = (tex instanceof ITextureView view) ? view.getBaseMipLevel() : 0;
                    int access = switch (shader.getDescription().resourcesLayout().getResource(name).access()) {
                        case Read -> GL_READ_ONLY;
                        case Write -> GL_WRITE_ONLY;
                        case Both -> GL_READ_WRITE;
                    };
                    int format = tex.getTextureFormat().gl();
                    if (stateCache == null || !stateCache.matchesStorageImageBinding(binding.bindingPoint(), tex.handle(), mipLevel, access, format)) {
                        Gl.DSA.bindImageTexture(
                                binding.bindingPoint(),
                                (int) tex.handle(),
                                mipLevel,
                                false,
                                0,
                                access,
                                format
                        );
                        if (stateCache != null) {
                            stateCache.recordStorageImageBinding(binding.bindingPoint(), tex.handle(), mipLevel, access, format);
                        }
                    }
                }
            }
        }
    }

    private int getTextureTarget(TextureType textureType) {
        return switch (textureType) {
            case Texture1D -> GL_TEXTURE_1D;
            case Texture2D -> GL_TEXTURE_2D;
        };
    }

    private int getUniformLocation(String name) {
        return uniformLocations.computeIfAbsent(name, key -> glGetUniformLocation((int) shader.handle(), key));
    }

    private int getUniformBlockIndex(String name) {
        return uniformBlockIndices.computeIfAbsent(name, key -> glGetUniformBlockIndex((int) shader.handle(), key));
    }

    @Override
    public void apply() {
        applyFromSnapshot(bindings);
    }

    @Override
    protected void updateImpl() {
    }
}
