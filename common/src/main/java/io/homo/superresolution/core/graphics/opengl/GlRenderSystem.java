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

package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderBaseUniform;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformSamplerTexture;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformStorageTexture;
import io.homo.superresolution.core.graphics.system.IRenderState;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;

import java.util.Map;

import static org.lwjgl.opengl.GL41.*;

public class GlRenderSystem implements IRenderSystem {
    private GlDevice device;
    public boolean supportsARBClearTexture;

    @Override
    public void initRenderSystem() {
        this.device = new GlDevice();
        supportsARBClearTexture = GL.getCapabilities().GL_ARB_clear_texture || GL.getCapabilities().OpenGL44;
    }

    @Override
    public void destroyRenderSystem() {
    }

    @Override
    public GlDevice device() {
        return device;
    }


    @Override
    public void finish() {
        glFinish();
    }
}