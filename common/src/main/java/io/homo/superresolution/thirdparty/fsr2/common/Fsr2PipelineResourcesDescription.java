/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;

public class Fsr2PipelineResourcesDescription {
    private String name;
    private String shaderName;
    private ITexture texture;
    private GlBuffer ubo;
    private boolean isWritable = false;
    private boolean isUBO = false;

    public GlBuffer ubo() {
        return ubo;
    }

    public Fsr2PipelineResourcesDescription ubo(GlBuffer ubo) {
        this.ubo = ubo;
        return this;
    }

    public boolean isUBO() {
        return isUBO;
    }

    public Fsr2PipelineResourcesDescription ubo(boolean UBO) {
        isUBO = UBO;
        return this;
    }

    public boolean isWritable() {
        return isWritable;
    }

    public Fsr2PipelineResourcesDescription writable(boolean writable) {
        isWritable = writable;
        return this;
    }

    public Fsr2PipelineResourcesDescription name(String name) {
        this.name = name;
        return this;
    }

    public Fsr2PipelineResourcesDescription shaderName(String shaderName) {
        this.shaderName = shaderName;
        return this;
    }

    public Fsr2PipelineResourcesDescription texture(ITexture texture) {
        this.texture = texture;
        return this;
    }

    public String name() {
        return name;
    }

    public String shaderName() {
        return shaderName;
    }

    public ITexture texture() {
        return texture;
    }
}
